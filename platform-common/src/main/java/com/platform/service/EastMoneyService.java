package com.platform.service;

import java.util.Map;

import com.platform.entity.ResultSupport;

public interface EastMoneyService {
	
	/**
	 * @securitiesCode 000001
	 * @start 20191201
	 * @end 20191231 
	 */
    ResultSupport<Map<String, Object>> getKLinesOfMonth(String securitiesCode, String start, String end);

    public class ResultCode{
    	
    	public static final String SECURITIES_CODE_ILLEGAL = "SECURITIES_CODE_ILLEGAL";
    	
        public static final String HTTP_STATUS_ILLEGAL = "HTTP_STATUS_ILLEGAL";
        
        public static final String HTTP_CLIENT_EXECUTE_EXCEPTION = "HTTP_CLIENT_EXECUTE_EXCEPTION";
        
        public static final String HTTP_RESULT_ILLEGAL_JSON_PATTERN = "HTTP_RESULT_ILLEGAL_JSON_PATTERN";
        
        public static final String HTTP_RESULT_MAYBE_NESTED_JSON_STRING = "HTTP_RESULT_MAYBE_NESTED_JSON_STRING";
        
        public static final String TUSHARE_SERVER_ERROR = "TUSHARE_SERVER_ERROR";
        
        public static final String ITEM_TRANS_EXCEPTION = "ITEM_TRANS_EXCEPTION";
        
    }
    
    public class EastMoneyResultCode{
        
    	public static final long SUC = 0;
        
    }
    
}
