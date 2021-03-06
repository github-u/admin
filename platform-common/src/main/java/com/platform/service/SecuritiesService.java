package com.platform.service;

import java.util.Date;
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
		
		public static final String IS_TRADE_DAY_EXCEPTION = "IS_TRADE_DAY_EXCEPTION";
		
	}
	
	ResultSupport<List<String>> getSecuritiesCodes();
	
	ResultSupport<Long> get(String type, String tableName, String securitiesCode, 
			String columnNames, String uniqColumnNames, Map<String, Object> conditions);
	
	ResultSupport<Long> get(String type, String tableName, String securitiesCode,
			String columnNames, String uniqColumnNames, Map<String, Object> conditions,
			Function<String, String> beforeSourceTableNameAliasProcessor,
			Function<Map<String, Object>, List<Map<String, Object>>> postSourceProcessor, 
			Function<String, String> postSourceTableNameAliasProcessor,
			boolean parallel);
	
	ResultSupport<Long> getBatch(String type, String tableName, 
			String columnNames, String uniqColumnNames, Map<String, Object> conditions);
	
	ResultSupport<Long> getBatch(String type, String tableName, 
			String columnNames, String uniqColumnNames, Map<String, Object> conditions,
			Function<String, String> beforeSourceTableNameAliasProcessor,
			Function<Map<String, Object>, List<Map<String, Object>>> postSourceProcessor, 
			Function<String, String> postSourceTableNameAliasProcessor,
			boolean parallel);
	
	ResultSupport<Boolean> isTradeDay(Date date);
	
}
