package com.platform.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.platform.entity.ResultSupport;

public interface SecuritiesService {
	
	public class SecuritiesServiceResultCode{
		public static long SUCCESS = 0;
	}
	
	public class ResultCode{
		
		public static final String SAVE_FAIL = "SAVE_FAIL";
		
		public static final String SAVE_EXCEPTION = "SAVE_EXCEPTION";
		
		public static final String GET_BATCH_POST_SOURCE_TRRANSFER_EXCEPTION = "GET_BATCH_POST_SOURCE_TRRANSFER_EXCEPTION";
		
	}
	
	ResultSupport<List<String>> getSecuritiesCodes();
	
	ResultSupport<Long> get(String type, String tableName, String securitiesCode, 
			String columnNames, String uniqColumnNames, Map<String, Object> conditions);
	
	ResultSupport<Long> getBatch(String type, String tableName, 
			String columnNames, String uniqColumnNames, Map<String, Object> conditions);
	
	ResultSupport<Long> getBatch(String type, String tableName, 
			String columnNames, String uniqColumnNames, Map<String, Object> conditions,
			Function<Map<String, Object>, Map<String, Object>> postSourceProcessor, 
			Function<String, String> postSourceTableNameAliasProcessor,
			boolean parallel);
	
}
