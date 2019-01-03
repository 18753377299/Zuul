package com.filter;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandContext;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.MultiValueMap;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;



public class RouteZuulFilter extends ZuulFilter{
	private Logger logger = LoggerFactory.getLogger(RouteZuulFilter.class);
	protected ProxyRequestHelper helper;
	
	@Override
	public boolean shouldFilter() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public Object run() {
		// TODO Auto-generated method stub
		System.out.println("=================RouteZuulFilter");
		RequestContext requestContext =RequestContext.getCurrentContext();
		this.helper.addIgnoredHeaders();
		//使用ribbon和hystrix来向服务实例发起请求
		
		try {
			RibbonCommandContext commandContext = buildCommandContext(requestContext);
			ClientHttpResponse chResponse = forward(commandContext);
			setResponse(chResponse);
			return chResponse;
		} catch (Exception e) {
//			requestContext.set(ERROR_STATUS_CODE,);
		}		
		return null;
	}
	protected RibbonCommandContext buildCommandContext(RequestContext requestContext) {
		HttpServletRequest request =requestContext.getRequest();
		MultiValueMap<String,String> headers = this.helper.buildZuulRequestHeaders(request);
		MultiValueMap<String , String> 
		
	}
	protected ClientHttpResponse forward(RibbonCommandContext commandContext) {
		
	}
	protected void setResponse() {
		
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
