package com.platform.service.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.alibaba.fastjson.JSON;
import com.platform.entity.ResultSupport;
import com.platform.service.DataService;
import com.platform.service.SQLService;
import com.platform.service.SQLService.PreSetColumn;
import com.platform.utils.LangUtil;

import lombok.Getter;

public class DataServiceImpl implements DataService {
    
    private static Logger logger = LoggerFactory.getLogger(DataServiceImpl.class);
    
    private DataSource druidDataSource;
    
    private AtomicBoolean inited = new AtomicBoolean(false);
    
    @Getter
    private SQLService sqlService;
    
    @Override
    public ResultSupport<List<Map<String, Object>>> select(String tableName, Map<String, Object> selectParams) {
        
        ResultSupport<List<Map<String, Object>>> ret = new ResultSupport<List<Map<String, Object>>>();
        
        ResultSupport<String> selectRet = sqlService.getSelect(tableName, selectParams);
        if(!selectRet.isSuccess()) {
            return ret.fail(selectRet.getErrCode(), selectRet.getErrMsg());
        }
        
        ResultSupport<List<Map<String, Object>>> executeQueryRet = executeQuery(selectRet.getModel(), 
                new HashMap<Integer, Object>() // TODO PreStatmentPromotion
                );
        if(!executeQueryRet.isSuccess()) {
            return ret.fail(executeQueryRet.getErrCode(), executeQueryRet.getErrMsg());
        }
        
        return ret.success(executeQueryRet.getModel());
        
    }

    @Override
    public ResultSupport<Integer> update(String tableName, Map<String, Object> updateParams) {
        
        ResultSupport<Integer> ret = new ResultSupport<Integer>();
        
        ResultSupport<String> updateRet = sqlService.getUpdate(tableName, updateParams);
        if(!updateRet.isSuccess()) {
            return ret.fail(updateRet.getErrCode(), updateRet.getErrMsg());
        }
        
        ResultSupport<Integer> executeUpdateRet = executeUpdate(updateRet.getModel(), 
                new HashMap<Integer, Object>() // TODO PreStatmentPromotion
                );
        if(!executeUpdateRet.isSuccess()) {
            return ret.fail(executeUpdateRet.getErrCode(), executeUpdateRet.getErrMsg());
        }
        
        return ret.success(executeUpdateRet.getModel());
        
    }

    @Override
    public ResultSupport<Integer> insert(String tableName, Map<String, Object> insertParams) {
        ResultSupport<Integer> ret = new ResultSupport<Integer>();
        
        ResultSupport<String> insertRet = sqlService.getInsert(tableName, insertParams);
        if(!insertRet.isSuccess()) {
            return ret.fail(insertRet.getErrCode(), insertRet.getErrMsg());
        }
        
        ResultSupport<Integer> executeInsertRet = executeUpdate(insertRet.getModel(), 
                new HashMap<Integer, Object>() // TODO PreStatmentPromotion
                );
        if(!executeInsertRet.isSuccess()) {
            return ret.fail(executeInsertRet.getErrCode(), executeInsertRet.getErrMsg());
        }
        
        return ret.success(executeInsertRet.getModel());
        
    }

    @Override
    public ResultSupport<Integer> delete(String tableName, int id) {
        
        ResultSupport<Integer> ret = new ResultSupport<Integer>();
        
        Map<String, Object> deleteParams = new HashMap<String, Object>();
        deleteParams.put(PreSetColumn.Id, id);
        
        ResultSupport<String> deleteRet = sqlService.getDelete(tableName, deleteParams);
        if(!deleteRet.isSuccess()) {
            return ret.fail(deleteRet.getErrCode(), deleteRet.getErrMsg());
        }
        
        ResultSupport<Integer> executeUpdateRet = executeUpdate(deleteRet.getModel(), 
                new HashMap<Integer, Object>() // TODO PreStatmentPromotion
                );
        if(!executeUpdateRet.isSuccess()) {
            return ret.fail(executeUpdateRet.getErrCode(), executeUpdateRet.getErrMsg());
        }
        
        return ret.success(ret.getModel());
        
    }
    
    private ResultSupport<Integer> executeUpdate(String sql, Map<Integer, Object> params){
        
        ResultSupport<Integer> ret = new ResultSupport<Integer>();
        try {
            return execute(sql, params, druidDataSource, new PreparedStatementExecutor<Integer>() {
                public ResultSupport<Integer> call(PreparedStatement preparedStatment) throws SQLException{
                    return new ResultSupport<Integer>().success(preparedStatment.executeUpdate());
                }
            });
        }catch(Exception e) {
            logger.error("title=" + "DataServiceImpl"
                    + "$mode=" + DataServiceModeCode.ExecuteUpdate
                    + "$errCode=" + DataServiceResultCode.ExecuteUpdateException, e);
            return ret.fail(DataServiceResultCode.ExecuteUpdateException, e.getMessage());
        }
    }
    
    private ResultSupport<List<Map<String, Object>>> executeQuery(String sql, Map<Integer, Object> params){
        
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
        ResultSupport<List<Map<String, Object>>> executeQueryRet = executeQuery(querySQL, new HashMap<Integer, Object>());
        if(!executeQueryRet.isSuccess()) {
            return ret.fail(executeQueryRet.getErrCode(), executeQueryRet.getErrMsg());
        }
        
        String model = LangUtil.convert(executeQueryRet.getModel().get(0).get("Create Table"), String.class);
        if(model == null) {
            throw new RuntimeException("CreateTableDDL Empty, tableName = " + tableName);
        }
        
        return ret.success(model);
                
    }
    
    private static <T> ResultSupport<T> execute(String sql, Map<Integer, Object> params, 
            DataSource druidDataSource, PreparedStatementExecutor<T> preparedStatementExecutor) throws Exception{
        
        ResultSupport<T> ret = new ResultSupport<T>();
        
        Connection connection = druidDataSource.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        try {
            try {
                for(Map.Entry<Integer, Object> param: params.entrySet()) {
                    preparedStatement.setObject(param.getKey(), param.getValue());
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
    
    public void init() throws Exception{
        
        if(inited.get()) {
            return ;
        }
        
        Properties properties = mySQLDataSourceProperties();
        
        druidDataSource = DruidDataSourceFactory.createDataSource(properties);
        
        sqlService = new SQLServiceImpl();
        ((SQLServiceImpl)sqlService).init();
        
        inited.compareAndSet(false, true);
        
    }
    
    public static Properties h2DataSourceProperties() {
        
        Properties properties = new Properties();
        
        properties.put("driverClassName", "org.h2.Driver");
        properties.put("url", "jdbc:h2:/Users/suxiong.sx/increasement.x/working_db/h2/20190830");
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
    
    public static Properties mySQLDataSourceProperties() {
        
        Properties properties = new Properties();
        
        properties.put("driverClassName", "com.mysql.jdbc.Driver");
        properties.put("url", "jdbc:mysql://127.0.0.1:3306/platform-shop?useUnicode=true&characterEncoding=utf8");
        properties.put("username", "root");
        properties.put("password", "");
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
    
    public static void main(String[] args) throws Exception {
        
        //testCollection();
        
        //testSelect();
        
        //testInsert();
        
        //testDelete();
        
        //testUpdate();
        
    }
        
    @Test
    private static void testInsert() throws Exception {
        
        String tableName = "sys_role";
        Map<String, Object> insertParams = new HashMap<String, Object>();
        
        DataServiceImpl dataServiceImpl = new DataServiceImpl();
        dataServiceImpl.init();
        
        ResultSupport<String> createTableDDLRet = dataServiceImpl.getCreateTableDDL(tableName);
        dataServiceImpl.getSqlService().generateSQLStatement(createTableDDLRet.getModel());
        
        System.out.println(dataServiceImpl.insert(tableName, insertParams));
        
    }
    
    @Test
    private static void testDelete() throws Exception {
        
        String tableName = "sys_role";
        Map<String, Object> deleteParams = new HashMap<String, Object>();
        
        DataServiceImpl dataServiceImpl = new DataServiceImpl();
        dataServiceImpl.init();
        
        ResultSupport<String> createTableDDLRet = dataServiceImpl.getCreateTableDDL(tableName);
        dataServiceImpl.getSqlService().generateSQLStatement(createTableDDLRet.getModel());
        
        System.out.println(dataServiceImpl.insert(tableName, deleteParams));
        
    }
    
    @Test
    private static void testSelect() throws Exception {
        
        String tableName = "sys_role";
        Map<String, Object> selectParams = new HashMap<String, Object>();
        
        DataServiceImpl dataServiceImpl = new DataServiceImpl();
        dataServiceImpl.init();
        
        ResultSupport<String> createTableDDLRet = dataServiceImpl.getCreateTableDDL(tableName);
        dataServiceImpl.getSqlService().generateSQLStatement(createTableDDLRet.getModel());
        
        System.out.println(dataServiceImpl.select(tableName, selectParams));
    }
    
    @Test
    private static void testUpdate() throws Exception {
        String tableName = "sys_role";
        Map<String, Object> updateParams = new HashMap<String, Object>();
        
        DataServiceImpl dataServiceImpl = new DataServiceImpl();
        dataServiceImpl.init();
        
        ResultSupport<String> createTableDDLRet = dataServiceImpl.getCreateTableDDL(tableName);
        dataServiceImpl.getSqlService().generateSQLStatement(createTableDDLRet.getModel());
        
        System.out.println(dataServiceImpl.insert(tableName, updateParams));
    }
    
    @Test
    private static void testCollection() throws Exception {
        
        DataServiceImpl dataServiceImpl = new DataServiceImpl();
        dataServiceImpl.init();
        
        System.out.println(dataServiceImpl.getCreateTableDDL("sys_role"));
        
    }
}

