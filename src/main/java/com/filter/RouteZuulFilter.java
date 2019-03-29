package com.filter;



import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.REQUEST_ENTITY_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.RETRYABLE_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.ribbon.RibbonHttpResponse;
import org.springframework.cloud.netflix.ribbon.apache.RibbonApacheHttpResponse;
import org.springframework.cloud.netflix.ribbon.support.RibbonRequestCustomizer;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommand;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandContext;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandFactory;
import org.springframework.cloud.netflix.zuul.util.ZuulRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.MultiValueMap;

import com.netflix.client.ClientException;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;



public class RouteZuulFilter extends ZuulFilter{
	private Logger logger = LoggerFactory.getLogger(RouteZuulFilter.class);
	protected ProxyRequestHelper helper;
	private boolean useServlet31 = true;
	protected RibbonCommandFactory<?> ribbonCommandFactory;
	protected List<RibbonRequestCustomizer> requestCustomizers;
	
	public RouteZuulFilter() {}
	public RouteZuulFilter(ProxyRequestHelper helper, RibbonCommandFactory<?> ribbonCommandFactory,
			List<RibbonRequestCustomizer> requestCustomizers) {
		this.helper = helper;
		this.ribbonCommandFactory = ribbonCommandFactory;
		this.requestCustomizers = requestCustomizers;
		// To support Servlet API 3.1 we need to check if getContentLengthLong exists
		try {
			// TODO: remove in 2.0
			HttpServletRequest.class.getMethod("getContentLengthLong");
		} catch (NoSuchMethodException e) {
			useServlet31 = false;
		}
	}

	public RouteZuulFilter(RibbonCommandFactory<?> ribbonCommandFactory) {
		this(new ProxyRequestHelper(), ribbonCommandFactory, null);
	}

	/* for testing */
	boolean isUseServlet31() {
		return useServlet31;
	}

	@Override
	public boolean shouldFilter() {
		// TODO Auto-generated method stub
//		return true;
		RequestContext ctx = RequestContext.getCurrentContext();
		return (ctx.getRouteHost() == null && ctx.get(SERVICE_ID_KEY) != null && ctx.sendZuulResponse());
	}

	@Override
	public Object run() {
		System.out.println("=================RouteZuulFilter");
//		RequestContext requestContext =RequestContext.getCurrentContext();
//		this.helper.addIgnoredHeaders();
		
		//使用ribbon和hystrix来向服务实例发起请求		
//		try {
//			RibbonCommandContext commandContext = buildCommandContext(requestContext);
//			ClientHttpResponse chResponse = forward(commandContext);
//			setResponse(chResponse);
//			return chResponse;
//		} catch (Exception e) {
////			requestContext.set(ERROR_STATUS_CODE,);
//			e.printStackTrace();
//			throw new ZuulRuntimeException(e);
//		}	
		return null;
	}
	protected RibbonCommandContext buildCommandContext(RequestContext context) {
		HttpServletRequest request = context.getRequest();

		MultiValueMap<String, String> headers = this.helper.buildZuulRequestHeaders(request);
		MultiValueMap<String, String> params = this.helper.buildZuulRequestQueryParams(request);
		String verb = getVerb(request);
		InputStream requestEntity = getRequestBody(request);
		if (request.getContentLength() < 0 && !verb.equalsIgnoreCase("GET")) {
			context.setChunkedRequestBody();
		}

		String serviceId = (String) context.get(SERVICE_ID_KEY);
		Boolean retryable = (Boolean) context.get(RETRYABLE_KEY);
//		Object loadBalancerKey = context.get(LOAD_BALANCER_KEY);//loadBalancerKey
//		Object loadBalancerKey = context.get("loadBalancerKey");

		String uri = this.helper.buildZuulRequestURI(request);

		// remove double slashes
		uri = uri.replace("//", "/");

		long contentLength = useServlet31 ? request.getContentLengthLong() : request.getContentLength();

		return new RibbonCommandContext(serviceId, verb, uri, retryable, headers, params, requestEntity,
				this.requestCustomizers, contentLength);
	}
	
	protected ClientHttpResponse forward(RibbonCommandContext context) throws Exception {
		Map<String, Object> info = this.helper.debug(context.getMethod(),context.getUri(), context.getHeaders(),
				context.getParams(), context.getRequestEntity());
		
		RibbonCommand command = this.ribbonCommandFactory.create(context);
		try {
			ClientHttpResponse response = command.execute();
			this.helper.appendDebug(info, response.getStatusCode().value(), response.getHeaders());
			return response;
		} catch (HystrixRuntimeException ex) {
			return handleException(info, ex);
		}
	}
	
	protected ClientHttpResponse handleException(Map<String, Object> info, HystrixRuntimeException ex)
			throws ZuulException {
		int statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
		Throwable cause = ex;
		String message = ex.getFailureType().toString();

		ClientException clientException = findClientException(ex);
		if (clientException == null) {
			clientException = findClientException(ex.getFallbackException());
		}

		if (clientException != null) {
			if (clientException.getErrorType() == ClientException.ErrorType.SERVER_THROTTLED) {
				statusCode = HttpStatus.SERVICE_UNAVAILABLE.value();
			}
			cause = clientException;
			message = clientException.getErrorType().toString();
		}
		info.put("status", String.valueOf(statusCode));
		throw new ZuulException(cause, "Forwarding error", statusCode, message);
	}
	protected void setResponse(ClientHttpResponse resp)  throws ClientException, IOException {
		RequestContext.getCurrentContext().set("zuulResponse", resp);
		RibbonHttpResponse rsp = (RibbonHttpResponse) resp;
		Field responseField=null;
		RibbonApacheHttpResponse responseValue=null;
		Field uriField=null;
		URI uriValue=null;
		try {
			responseField = RibbonHttpResponse.class.getDeclaredField("response");
			responseField.setAccessible(true);
			responseValue = (RibbonApacheHttpResponse) responseField.get(rsp);
			uriField = RibbonApacheHttpResponse.class.getDeclaredField("uri");
			uriField.setAccessible(true);
			uriValue = (URI) uriField.get(responseValue);
			RequestContext.getCurrentContext().set("URI", uriValue);
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.helper.setResponse(resp.getStatusCode().value(), resp.getBody() == null ? null : resp.getBody(),
				resp.getHeaders());
	}
	public String  getVerb(HttpServletRequest request) {		
		String method = request.getMethod();
		if (method == null) {
			return "GET";
		}
		return method;
	}
	public InputStream  getRequestBody(HttpServletRequest request) {
		
		InputStream requestEntity = null;
		try {
			requestEntity = (InputStream) RequestContext.getCurrentContext().get(REQUEST_ENTITY_KEY);
			if (requestEntity == null) {
				requestEntity = request.getInputStream();
			}
		} catch (IOException ex) {
			logger.error("Error during getRequestBody", ex);
		}
		return requestEntity;
	}
	protected ClientException findClientException(Throwable t) {
		if (t == null) {
			return null;
		}
		if (t instanceof ClientException) {
			return (ClientException) t;
		}
		return findClientException(t.getCause());
	}	
	
	
	@Override
	public String filterType() {
		// TODO Auto-generated method stub
		return "route";
	}

	@Override
	public int filterOrder() {
		// TODO Auto-generated method stub
		return 0;
	}
}
