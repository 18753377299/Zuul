package com;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import com.filter.ErrorZuulFilter;
import com.filter.PostZuulFilter;
import com.filter.PreZuulFilter;
import com.filter.RouteZuulFilter;


@EnableZuulProxy
@EnableAutoConfiguration
//@SpringCloudApplication
@SpringBootApplication
@ComponentScan(basePackages = "com")
public class ZuulApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZuulApplication.class, args);
	}
	
	@Bean
	public PreZuulFilter preZuulFilter() {  
	    return new PreZuulFilter();  
	} 
	@Bean
	public ErrorZuulFilter errorZuulFilter() {  
	    return new ErrorZuulFilter();  
	}  
	@Bean
	public PostZuulFilter postZuulFilter() {  
	    return new PostZuulFilter();  
	} 
	@Bean
	public RouteZuulFilter routeZuulFilter() {  
	    return new RouteZuulFilter();  
	}  
	
}
