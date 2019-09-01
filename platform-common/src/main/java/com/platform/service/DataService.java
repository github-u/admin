package com.platform.service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.platform.entity.ResultSupport;

public interface DataService {
    
    ResultSupport<List<Map<String, Object>>> select(String tableName, Map<String, Object> params);
    
    ResultSupport<Integer> update(String tableName, Map<String, Object> params);
    
    ResultSupport<Integer> insert(String tableName, Map<String, Object> params);
    
    ResultSupport<Integer> delete(String tableName, int id);
    
    public interface PreparedStatementExecutor<T> {
        ResultSupport<T> call(PreparedStatement preparedStatment) throws SQLException;
    }
    
    public static class DataServiceModeCode{
        public static final String Execute = "Execute";
    }
    
    public static class DataServiceResultCode{
        
        public static final String NormalCode = "NormalCode";
        
        public static final String PreparedStatementSetObjectException = "PreparedStatementSetObjectException";
        
        public static final String PreparedStatementExecutorException = "PreparedStatementExecutorException";
        
        public static final String PreparedStatementOrConnectionCloseException = "PreparedStatementOrConnectionCloseException";
        
    }
    
}
