package com.platform.service.impl;

import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

//import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.alibaba.druid.sql.ast.statement.SQLColumnDefinition;
import com.google.common.base.Preconditions;
import com.platform.entity.ResultSupport;
import com.platform.entity.properties.PropertieKeys;
import com.platform.service.DataService;
import com.platform.service.PropertyService;
import com.platform.service.SQLService;
import com.platform.service.SQLService.Constans;
import com.platform.service.SQLService.PreSetColumn;
import com.platform.service.SQLService.PreparedStatementValue;
import com.platform.utils.LangUtil;
import com.platform.utils.Pair;

import lombok.Getter;

public class DataServiceImpl implements DataService {
    
    private static Logger logger = LoggerFactory.getLogger(DataServiceImpl.class);
    
    private DataSource druidDataSource;
    
    private AtomicBoolean inited = new AtomicBoolean(false);
    
    @Getter
    private SQLService sqlService;
    
    @Getter
    private PropertyService propertyService;
    
    @Override
    public ResultSupport<List<Map<String, Object>>> select(String tableName, Map<String, Object> selectParams) {
        
        ResultSupport<List<Map<String, Object>>> ret = new ResultSupport<List<Map<String, Object>>>();
        
        ResultSupport<Boolean> registeredSQLStatementRet = registeredSQLStatement(tableName);
        if(!registeredSQLStatementRet.isSuccess()) {
            return ret.fail(registeredSQLStatementRet.getErrCode(), registeredSQLStatementRet.getErrMsg());
        }
        
        ResultSupport<Pair<String, Map<Integer, PreparedStatementValue>>> selectRet = sqlService.getSelect(tableName, selectParams);
        if(!selectRet.isSuccess()) {
            return ret.fail(selectRet.getErrCode(), selectRet.getErrMsg());
        }
        
        ResultSupport<List<Map<String, Object>>> executeQueryRet = executeQuery(
                tableName,
                selectRet.getModel().fst, 
                selectRet.getModel().snd
                );
        if(!executeQueryRet.isSuccess()) {
            return ret.fail(executeQueryRet.getErrCode(), executeQueryRet.getErrMsg());
        }
        
        return ret.success(executeQueryRet.getModel());
        
    }

    @Override
    public ResultSupport<Long> update(String tableName, Map<String, Object> updateParams) {
        
        ResultSupport<Long> ret = new ResultSupport<Long>();
        
        ResultSupport<Boolean> registeredSQLStatementRet = registeredSQLStatement(tableName);
        if(!registeredSQLStatementRet.isSuccess()) {
            return ret.fail(registeredSQLStatementRet.getErrCode(), registeredSQLStatementRet.getErrMsg());
        }
        
        ResultSupport<Pair<String, Map<Integer, PreparedStatementValue>>> updateRet = sqlService.getUpdate(tableName, updateParams);
        if(!updateRet.isSuccess()) {
            return ret.fail(updateRet.getErrCode(), updateRet.getErrMsg());
        }
        
        ResultSupport<Long> executeUpdateRet = executeUpdate(
                tableName,
                updateRet.getModel().fst, 
                updateRet.getModel().snd
                );
        if(!executeUpdateRet.isSuccess()) {
            return ret.fail(executeUpdateRet.getErrCode(), executeUpdateRet.getErrMsg());
        }
        
        return ret.success(executeUpdateRet.getModel());
        
    }

    @Override
    public ResultSupport<Long> insert(String tableName, Map<String, Object> insertParams) {
        ResultSupport<Long> ret = new ResultSupport<Long>();
        
        ResultSupport<Boolean> registeredSQLStatementRet = registeredSQLStatement(tableName);
        if(!registeredSQLStatementRet.isSuccess()) {
            return ret.fail(registeredSQLStatementRet.getErrCode(), registeredSQLStatementRet.getErrMsg());
        }
        
        if(insertParams.get(PreSetColumn.Status) == null) {
            insertParams.put(PreSetColumn.Status, 0);
        }
        if(insertParams.get(PreSetColumn.Version) == null) {
            insertParams.put(PreSetColumn.Version, 0);
        }
        ResultSupport<Pair<String, Map<Integer, PreparedStatementValue>>> insertRet = sqlService.getInsert(tableName, insertParams);
        if(!insertRet.isSuccess()) {
            return ret.fail(insertRet.getErrCode(), insertRet.getErrMsg());
        }
        
        ResultSupport<Long> executeInsertRet = executeInsert(
                tableName,
                insertRet.getModel().fst, 
                insertRet.getModel().snd 
                );
        
        if(!executeInsertRet.isSuccess()) {
            return ret.fail(executeInsertRet.getErrCode(), executeInsertRet.getErrMsg());
        }
        
        return ret.success(executeInsertRet.getModel());
        
    }

    @Override
    public ResultSupport<Long> delete(String tableName, int id) {
        
        ResultSupport<Long> ret = new ResultSupport<Long>();
        
        ResultSupport<Boolean> registeredSQLStatementRet = registeredSQLStatement(tableName);
        if(!registeredSQLStatementRet.isSuccess()) {
            return ret.fail(registeredSQLStatementRet.getErrCode(), registeredSQLStatementRet.getErrMsg());
        }
        
        Map<String, Object> deleteParams = new HashMap<String, Object>();
        deleteParams.put(PreSetColumn.Id, id);
        
        ResultSupport<Pair<String, Map<Integer, PreparedStatementValue>>> deleteRet = sqlService.getDelete(tableName, deleteParams);
        if(!deleteRet.isSuccess()) {
            return ret.fail(deleteRet.getErrCode(), deleteRet.getErrMsg());
        }
        
        ResultSupport<Long> executeUpdateRet = executeUpdate(
                tableName,
                deleteRet.getModel().fst, 
                deleteRet.getModel().snd
                );
        if(!executeUpdateRet.isSuccess()) {
            return ret.fail(executeUpdateRet.getErrCode(), executeUpdateRet.getErrMsg());
        }
        
        return ret.success(ret.getModel());
        
    }
    
    private ResultSupport<Long> executeInsert(String tableName, String sql, Map<Integer, PreparedStatementValue> params){
        
        ResultSupport<Long> ret = new ResultSupport<Long>();
        try {
            return execute(sql, params, druidDataSource, Statement.RETURN_GENERATED_KEYS ,new PreparedStatementExecutor<Long>() {
                public ResultSupport<Long> call(PreparedStatement preparedStatment) throws SQLException{
                    
                    Preconditions.checkArgument(preparedStatment.executeUpdate() > 0, "table not insert excactly, table is " + tableName);
                    ResultSet insertRowIdResultSet = preparedStatment.getGeneratedKeys();
                    
                    ResultSupport<List<Map<String, Object>>> insertRowIdResultRet = getResult(insertRowIdResultSet);
                    Preconditions.checkArgument(insertRowIdResultRet.isSuccess(), "get last insert row id fail, table is " + tableName);
                    
                    Long rowId = LangUtil.safeLong(insertRowIdResultRet.getModel().get(0).get(Constans.GneratedKey));
                    Preconditions.checkArgument(rowId != null && rowId > 0, "last row id invalid, table is " + tableName);
                    
                    return new ResultSupport<Long>().success(rowId);
                }
            });
        }catch(Exception e) {
            logger.error("title=" + "DataServiceImpl"
                    + "$mode=" + DataServiceModeCode.ExecuteUpdate
                    + "$errCode=" + DataServiceResultCode.ExecuteUpdateException, e);
            return ret.fail(DataServiceResultCode.ExecuteUpdateException, e.getMessage());
        }
    }
    
    private ResultSupport<Long> executeUpdate(String tableName, String sql, Map<Integer, PreparedStatementValue> params){
        
        ResultSupport<Long> ret = new ResultSupport<Long>();
        try {
            return execute(sql, params, druidDataSource, new PreparedStatementExecutor<Long>() {
                public ResultSupport<Long> call(PreparedStatement preparedStatment) throws SQLException{
                    return new ResultSupport<Long>().success(Long.valueOf(preparedStatment.executeUpdate()));
                }
            });
        }catch(Exception e) {
            logger.error("title=" + "DataServiceImpl"
                    + "$mode=" + DataServiceModeCode.ExecuteUpdate
                    + "$errCode=" + DataServiceResultCode.ExecuteUpdateException, e);
            return ret.fail(DataServiceResultCode.ExecuteUpdateException, e.getMessage());
        }
    }
    
    private ResultSupport<List<Map<String, Object>>> executeQuery(String tableName, String sql, Map<Integer, PreparedStatementValue> params){
        
        ResultSupport<List<Map<String, Object>>> ret = new ResultSupport<List<Map<String, Object>>>();
        try {
            return execute(sql, params, druidDataSource, new PreparedStatementExecutor<List<Map<String, Object>>>() {
                public ResultSupport<List<Map<String, Object>>> call(PreparedStatement preparedStatment) throws SQLException{
                    return getResult(preparedStatment.executeQuery());
                }
            });
        }catch(Exception e) {
            logger.error("title=" + "DataServiceImpl"
                    + "$mode=" + DataServiceModeCode.ExecuteQuery
                    + "$errCode=" + DataServiceResultCode.ExecuteQueryException, e);
            return ret.fail(DataServiceResultCode.ExecuteQueryException, e.getMessage());
        }
    }
    
    private ResultSupport<String> getCreateTableDDL(String tableName){
        
        ResultSupport<String> ret = new ResultSupport<String>();
        String querySQL = "show create table " + tableName + ";";
        ResultSupport<List<Map<String, Object>>> executeQueryRet = executeQuery(tableName, querySQL, new HashMap<Integer, PreparedStatementValue>());
        if(!executeQueryRet.isSuccess()) {
            return ret.fail(executeQueryRet.getErrCode(), executeQueryRet.getErrMsg());
        }
        
        String model = LangUtil.convert(executeQueryRet.getModel().get(0).get("Create Table"), String.class);
        if(model == null) {
            throw new RuntimeException("CreateTableDDL Empty, tableName = " + tableName);
        }
        
        return ret.success(model);
                
    }
    
    private static <T> ResultSupport<T> execute(String sql, Map<Integer, PreparedStatementValue> params, 
            DataSource druidDataSource,  PreparedStatementExecutor<T> preparedStatementExecutor) throws Exception{
        return execute(sql, params, druidDataSource, null, preparedStatementExecutor);
    }
    
    private static <T> ResultSupport<T> execute(String sql, Map<Integer, PreparedStatementValue> params, 
            DataSource druidDataSource, Integer autoGeneratedKeys, PreparedStatementExecutor<T> preparedStatementExecutor) throws Exception{
               ResultSupport<T> ret = new ResultSupport<T>();
        
        Connection connection = druidDataSource.getConnection();
        PreparedStatement preparedStatement;
        if(autoGeneratedKeys != null) {
            preparedStatement = connection.prepareStatement(sql, autoGeneratedKeys);
        }else {
            preparedStatement = connection.prepareStatement(sql);
        }
        
        try {
            try {
                for(Map.Entry<Integer, PreparedStatementValue> param: params.entrySet()) {
                    if(param.getValue().getValue() != null) {
                        preparedStatement.setObject(param.getKey(), param.getValue().getValue());
                    }else{
                        
                        SQLColumnDefinition sqlColumnDefinition = param.getValue().getSqlColumnDefinition();
                        
                        JDBCType jdbcType = parseJDBCType(sqlColumnDefinition.getDataType().getName());
                        if(jdbcType == null) {
                            throw new RuntimeException("column type not known, type is " + sqlColumnDefinition.getDataType().getName());
                        }
                        
                        preparedStatement.setNull(param.getKey(), jdbcType.getVendorTypeNumber());
                    }
                    
                }
            }catch(Exception e) {
                logger.error("title=" + "DataServiceImpl"
                          + "$mode=" + DataServiceModeCode.Execute
                        + "$errCode=" + DataServiceResultCode.PreparedStatementSetObjectException, e);
                return ret.fail(DataServiceResultCode.PreparedStatementSetObjectException, e.getMessage());
            }
            
            try {
                ResultSupport<T> callRet = preparedStatementExecutor.call(preparedStatement);
                if(!callRet.isSuccess()) {
                    return ret.fail(callRet.getErrCode(), callRet.getErrMsg());
                }
                return ret.success(callRet.getModel());
            }catch(Exception e) {
                logger.error("title=" + "DataServiceImpl"
                          + "$mode=" + DataServiceModeCode.Execute
                        + "$errCode=" + DataServiceResultCode.PreparedStatementExecutorException, e);
                return ret.fail(DataServiceResultCode.PreparedStatementExecutorException, e.getMessage());
            }
            
        }finally {
            try {
                if(preparedStatement != null) {
                    preparedStatement.close();
                } 
                if(connection != null) {
                    connection.close();
                }
            }catch(Exception e) {
                logger.error("title=" + "DataServiceImpl"
                        + "$mode=" + DataServiceModeCode.Execute
                        + "$errCode=" + DataServiceResultCode.PreparedStatementOrConnectionCloseException, e);
                return ret.fail(DataServiceResultCode.PreparedStatementOrConnectionCloseException, e.getMessage());
            }
        }
        
    }
    
    private ResultSupport<List<Map<String, Object>>> getResult(ResultSet resultSet) {
        ResultSupport<List<Map<String, Object>>> ret = new ResultSupport<List<Map<String, Object>>>();
        List<Map<String, Object>> model = new ArrayList<Map<String, Object>>();
        
        try {
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            int columnCount = resultSetMetaData.getColumnCount();
            
            
            for(;resultSet.next();) {
                Map<String, Object> row = new LinkedHashMap<String, Object>();
                for(int i=1; i <= columnCount; i++) {
                    row.put(resultSetMetaData.getColumnLabel(i), resultSet.getObject(i));
                }
                model.add(row);
            }
        }catch(Exception e) {
            logger.error("title=" + "DataServiceImpl"
                        + "$mode=" + DataServiceModeCode.GetResult
                        + "$errCode=" + DataServiceResultCode.GetResultException, e);
            return ret.fail(DataServiceResultCode.GetResultException, e.getMessage());
        }
        
        return ret.success(model);
    }
    
    private ResultSupport<Boolean> registeredSQLStatement(String tableName) {
        
        ResultSupport<Boolean> ret = new ResultSupport<Boolean> ();
        
        if(sqlService.getSQLStatement(tableName).isSuccess()) {
            return ret.success(Boolean.TRUE);
        }
        
        ResultSupport<String> createTableDDLRet = getCreateTableDDL(tableName);
        if(!createTableDDLRet.isSuccess()) {
            return ret.fail(createTableDDLRet.getErrCode(), createTableDDLRet.getErrMsg());
        }
        
        return sqlService.generateSQLStatement(createTableDDLRet.getModel());
        
    }
    
    public void init() throws Exception{
        
        if(inited.get()) {
            return ;
        }
        
        propertyService = new PropertyServiceImpl();
        ((PropertyServiceImpl)propertyService).init();
        
        Properties properties = mySQLDataSourceProperties();
        
        druidDataSource = DruidDataSourceFactory.createDataSource(properties);
        
        sqlService = new SQLServiceImpl();
        ((SQLServiceImpl)sqlService).init();
        
       
        inited.compareAndSet(false, true);
        
    }
    
    public static Properties h2DataSourceProperties() {
        
        Properties properties = new Properties();
        
        properties.put("driverClassName", "org.h2.Driver");
        properties.put("url", "");
        properties.put("username", "sa");
        properties.put("password", "sa");
        properties.put("filters", "stat");
        properties.put("initialSize", "2");
        properties.put("maxActive", "300");
        properties.put("maxWait", "60000");
        properties.put("timeBetweenEvictionRunsMillis", "60000");
        properties.put("minEvictableIdleTimeMillis", "300000");
        properties.put("validationQuery", "SELECT 1");
        properties.put("testWhileIdle", "true");
        properties.put("testOnBorrow", "false");
        properties.put("testOnReturn", "false");
        properties.put("poolPreparedStatements", "false");
        properties.put("maxPoolPreparedStatementPerConnectionSize", "200");
        
        return properties;
    }
    
    public Properties mySQLDataSourceProperties() {
        
        Properties properties = new Properties();
        
        properties.put("driverClassName", "com.mysql.jdbc.Driver");
        properties.put("url", propertyService.get(PropertieKeys.Mysql.URL).getModel());
        properties.put("username", propertyService.get(PropertieKeys.Mysql.USER).getModel());
        properties.put("password", propertyService.get(PropertieKeys.Mysql.PASSWD).getModel());
        properties.put("filters", "stat");
        properties.put("initialSize", "2");
        properties.put("maxActive", "300");
        properties.put("maxWait", "60000");
        properties.put("timeBetweenEvictionRunsMillis", "60000");
        properties.put("minEvictableIdleTimeMillis", "300000");
        properties.put("validationQuery", "SELECT 1");
        properties.put("testWhileIdle", "true");
        properties.put("testOnBorrow", "false");
        properties.put("testOnReturn", "false");
        properties.put("poolPreparedStatements", "false");
        properties.put("maxPoolPreparedStatementPerConnectionSize", "200");
        
        return properties;
    }
    
    private static JDBCType parseJDBCType(String columnDataTypeName) {
        
        JDBCType jdbcType = null;
        
        for(JDBCType tJDBCType : JDBCType.values()) {
            if(tJDBCType.getName().equalsIgnoreCase(columnDataTypeName)) {
                jdbcType = tJDBCType;
            }
        }
        
        return jdbcType;
    }
    public static void main(String[] args) throws Exception {
        
        //testCollection();
        
        //testSelect();
        
        //testInsert();
        
        //testInsert();
        
        //testInsertO();
        
        //testInsertO();
        
        //testInsert();
        
        //testDelete();
        
        //testUpdate();
        
    }
        
    //@Test
    private static void testInsert() throws Exception {
        
        String tableName = "sys_test";
        Map<String, Object> insertParams = new HashMap<String, Object>();
        insertParams.put("text", "a");
        insertParams.put("time", "2019-01-02 12:34:56");
        
        DataServiceImpl dataServiceImpl = new DataServiceImpl();
        dataServiceImpl.init();
        
        System.out.println(dataServiceImpl.insert(tableName, insertParams));
        
    }
    
    private static void testInsertO() throws Exception {
        
        String tableName = "daily_basic";
        Map<String, Object> insertParams = new HashMap<String, Object>();
        insertParams.put("ts_code", "a");
        
        DataServiceImpl dataServiceImpl = new DataServiceImpl();
        dataServiceImpl.init();
        
        System.out.println(dataServiceImpl.insert(tableName, insertParams));
        
    }
    
    
    //@Test
    private static void testDelete() throws Exception {
        
        String tableName = "sys_test";
        
        DataServiceImpl dataServiceImpl = new DataServiceImpl();
        dataServiceImpl.init();
        
        System.out.println(dataServiceImpl.delete(tableName, 8));
        
    }
    
    //@Test
    private static void testSelect() throws Exception {
        
        String tableName = "sys_test";
        Map<String, Object> selectParams = new HashMap<String, Object>();
        selectParams.put("time.start", "2019-01-02 00:00:00");
        selectParams.put("time.end", "2019-09-10 23:00:00");
        
        DataServiceImpl dataServiceImpl = new DataServiceImpl();
        dataServiceImpl.init();
        
        System.out.println(dataServiceImpl.select(tableName, selectParams));
    }
    
    //@Test
    private static void testUpdate() throws Exception {
        String tableName = "sys_test";
        Map<String, Object> updateParams = new HashMap<String, Object>();
        updateParams.put("id", 3);
        updateParams.put("text", "3");
        updateParams.put("status", "1");
        updateParams.put("time", "2019-09-10 01:02:03");
        updateParams.put("attribute", "a:1;b:2");
        
        DataServiceImpl dataServiceImpl = new DataServiceImpl();
        dataServiceImpl.init();
        
        System.out.println(dataServiceImpl.update(tableName, updateParams));
    }
    
    //@Test
    private static void testCollection() throws Exception {
        
        DataServiceImpl dataServiceImpl = new DataServiceImpl();
        dataServiceImpl.init();
        
        System.out.println(dataServiceImpl.getCreateTableDDL("sys_test"));
        
    }
    
}
