package com.filter;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

public class ErrorZuulFilter extends ZuulFilter {

	private Logger Logger = LoggerFactory.getLogger(ErrorZuulFilter.class);
	
	@Override
	public boolean shouldFilter() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public Object run() {
		// TODO Auto-generated method stub
		System.out.println("error");
		RequestContext requestContext =RequestContext.getCurrentContext();
		HttpServletRequest request =requestContext.getRequest();
		this.response(requestContext,"500","服务异常！");		
		return null;
	}
	public void response(RequestContext requestContext,String code,String message) {
		String path = requestContext.getRequest().getServletPath();
		//不跳转路由
		requestContext.setSendZuulResponse(false);
		requestContext.setResponseStatusCode(500);
		HttpServletResponse response =requestContext.getResponse();
		try {
			PrintWriter pw =response.getWriter();
			pw.write("{\"status\":\""+code+"\",\"message\":\""+message+"\",\"data\":{\"code\":\""+code+"\"}}");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public String filterType() {
		// TODO Auto-generated method stub
		return "error";
	}

	@Override
	public int filterOrder() {
		// TODO Auto-generated method stub
		return 0;
	}

}
