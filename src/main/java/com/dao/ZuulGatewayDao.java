package com.dao;

import java.util.List;

import com.po.ZuulRouteVO;

public interface ZuulGatewayDao {
	
	public List<ZuulRouteVO> findByEnable(boolean enable);
}
