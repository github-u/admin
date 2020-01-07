package com.platform.service.impl;

import java.util.List;
import java.util.Map;

import com.platform.entity.ResultSupport;

public interface SourceService {
	
	public ResultSupport<List<Map<String, Object>>> source(
			String sourceName, 
			String securitiesCode, 
			String columnNames, 
			Map<String, Object> conditions
			);
	
	public ResultSupport<List<Map<String, Object>>> source(
			String sourceName, 
			String columnNames, 
			Map<String, Object> conditions
			);
	
	public static final class Source{
		
		public static final String EAST_MONEY = "EAST_MONEY";
		
		public static final String TU_SHARE = "TU_SHARE";
		
	}
	
}
