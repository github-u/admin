package com.platform.service;

import com.platform.entity.ResultSupport;
import com.platform.entity.tushare.TuShareData;
import com.platform.entity.tushare.TuShareParam;

public interface TuShareService {
    
    ResultSupport<TuShareData> getData(TuShareParam tuShareParam);
    
    public class ResultCode{
        
        public static final String HTTP_STATUS_ILLEGAL = "HTTP_STATUS_ILLEGAL";
        
        public static final String HTTP_CLIENT_EXECUTE_EXCEPTION = "HTTP_CLIENT_EXECUTE_EXCEPTION";
        
        public static final String TUSHARE_SERVER_ERROR = "TUSHARE_SERVER_ERROR";
        
        public static final String ITEM_TRANS_EXCEPTION = "ITEM_TRANS_EXCEPTION";
        
    }
    
    public class TuShareResultCode{
        
        public static final long SUC = 0;
        
    }
    
}
