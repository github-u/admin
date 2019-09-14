package com.platform.service;

import java.util.Map;

import com.alibaba.druid.sql.ast.statement.SQLColumnDefinition;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlCreateTableStatement;
import com.platform.entity.ResultSupport;
import com.platform.utils.Pair;

import lombok.Getter;
import lombok.Setter;

public interface SQLService {
    
    ResultSupport<Boolean> generateSQLStatement(String createTableDDL);
    
    ResultSupport<MySqlCreateTableStatement> getSQLStatement(String tableName);
    
    ResultSupport<Pair<String, Map<Integer, PreparedStatementValue>>> getSelect(String tableName, Map<String, Object> queryParams);
    
    ResultSupport<Pair<String, Map<Integer, PreparedStatementValue>>> getUpdate(String tableName, Map<String, Object> updateParams);
    
    ResultSupport<Pair<String, Map<Integer, PreparedStatementValue>>> getInsert(String tableName, Map<String, Object> insertParams);
    
    ResultSupport<Pair<String, Map<Integer, PreparedStatementValue>>> getDelete(String tableName, Map<String, Object> params);
    
    public static class PreparedStatementValue{
        
        @Getter @Setter private SQLColumnDefinition sqlColumnDefinition;
        
        @Getter @Setter private Object value;
        
        public PreparedStatementValue(SQLColumnDefinition sqlColumnDefinition, Object value) {
            this.sqlColumnDefinition = sqlColumnDefinition;
            this.value = value;
        }
        
    }
    
    public static class SQLServiceModeCode{
        public static final String CreateTableSQLStatement = "CreateTableSQLStatement";
    }
    
    public static class SQLServiceResultCode{
        
        public static final String NormalCode = "NormalCode";
        
        public static final String NoneCreateTableSQLFound = "NoneCreateTableSQLFound";
        
        public static final String NoneCreateTableStatementFound = "NoneCreateTableStatementFound";
        
        public static final String NoneCreateTableStatementGenerated = "NoneCreateTableStatementGenerated";
        
    }
    
    public static class VelocityContextKey{
        
        public static final String TableName = "__table_name";
        
        public static final String Columns = "__columns";
        
        public static final String UpdateColumns = "__update_columns";
        
        public static final String InsertColumns = "__insert_columns";
        
        public static final String Conditions = "__conditions";
        
        public static final String Start = "__start";
        
        public static final String Limit = "__limit";
        
    }
    
    public static class PreSetColumn{
        
        public static final String Id = "id";
        
        public static final String Status = "status";
        
        public static final String GmtCreate = "gmt_create";
        
        public static final String GmtModified = "gmt_modified";
        
        public static final String Version = "version";
        
        public static final String Attribute = "attribute";
        
    }
    
    public static class Constans{
        
        public static final Long DefaultStart = 0L;
        
        public static final Long DefaultLimit = 20L;
        
        public static final Long DefaultRowId = 0L;
        
        public static final String DefaultSelectVmPath = "vm/sql/select.vm";
        
        public static final String DefaultUpdateVmPath = "vm/sql/update.vm";
        
        public static final String DefaultInsertVmPath = "vm/sql/insert.vm";
        
        public static final String DateTimeType = "datetime";
        
        public static final String StringType = "varchar";
        
        public static final String DateTimeStartSuffix = ".start";
        
        public static final String DateTimeEndSuffix = ".end";
        
        public static final String PercentSign = "%";
        
        public static final int NormalStatus = 0;
        
        public static final int DeleteStatus = -1;
        
        public static final int DefaultVersion = 0;
        
        public static final String ASCII126 = "`";
        
        public static final String LastInsertId = "last_insert_id()";
        
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
