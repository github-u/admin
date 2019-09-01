package com.platform.service;

import java.util.Map;

import com.platform.entity.ResultSupport;

import lombok.Getter;
import lombok.Setter;

public interface SQLService {
    
    ResultSupport<Boolean> generateSQLStatement(String createTableDDL);
    
    ResultSupport<String> getSelect(String tableName, Map<String, Object> params);
    
    ResultSupport<String> getUpdate(String tableName, Map<String, Object> params);
    
    ResultSupport<String> getInsert(String tableName, Map<String, Object> params);
    
    ResultSupport<String> getDelete(String tableName, Map<String, Object> params);
    
    public static class SQLServiceModeCode{
        public static final String CreateTableSQLStatement = "CreateTableSQLStatement";
    }
    
    public static class SQLServiceResultCode{
        
        public static final String NormalCode = "NormalCode";
        
        public static final String NoneCreateTableSQLFound = "NoneCreateTableSQLFound";
        
        public static final String NoneCreateTableStatementFound = "NoneCreateTableStatementFound";
        
    }
    
    public static class VelocityContextKey{
        
        public static String TableName = "__table_name";
        
        public static String Columns = "__columns";
        
        public static String Conditions = "__conditions";
        
        public static String Start = "__start";
        
        public static String Limit = "__limit";
        
    }
    
    public static class Constans{
        
        public static Long DefaultStart = 0L;
        
        public static Long DefaultLimit = 20L;
        
        public static String DefaultSelectVmPath = "vm/sql/select.vm";
        
        public static String DateTimeType = "datetime";
        
        public static String DateTimeStartSuffix = ".start";
        
        public static String DateTimeEndSuffix = ".end";
        
    }
    
    public static class DateValue {
        
        @Getter @Setter private Object start;
        
        @Getter @Setter private Object end;
        
        public DateValue(Object start, Object end) {
            this.start = start;
            this.end = end;
        }
        
    }
    
}
