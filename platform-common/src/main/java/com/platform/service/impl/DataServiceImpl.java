package com.platform.service.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.platform.entity.ResultSupport;
import com.platform.service.DataService;

public class DataServiceImpl implements DataService {
    
    private static Logger logger = LoggerFactory.getLogger(DataServiceImpl.class);
    
    private DataSource druidDataSource;
    
    private AtomicBoolean inited = new AtomicBoolean(false);
    
    @Override
    public ResultSupport<List<Map<String, Object>>> select(String tableName, Map<String, Object> params) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSupport<Integer> update(String tableName, Map<String, Object> params) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSupport<Integer> insert(String tableName, Map<String, Object> params) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSupport<Integer> delete(String tableName, int id) {
        // TODO Auto-generated method stub
        return null;
    }
    
    private ResultSupport<Integer> executeUpdate(String sql, Map<Integer, Object> params, 
            DataSource druidDataSource, PreparedStatementExecutor<Integer> preparedStatementExecutor) throws Exception{
        return execute(sql, params, druidDataSource, new PreparedStatementExecutor<Integer>() {
            public ResultSupport<Integer> call(PreparedStatement preparedStatment) throws SQLException{
                return new ResultSupport<Integer>().success(preparedStatment.executeUpdate());
            }
        });
    }
    
    private ResultSupport<ResultSet> executeQuery(String sql, Map<Integer, Object> params, 
            DataSource druidDataSource, PreparedStatementExecutor<ResultSet> preparedStatementExecutor) throws Exception{
        return execute(sql, params, druidDataSource, new PreparedStatementExecutor<ResultSet>() {
            public ResultSupport<ResultSet> call(PreparedStatement preparedStatment) throws SQLException{
                return new ResultSupport<ResultSet>().success(preparedStatment.executeQuery());
            }
        });
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
    
    public void init() throws Exception{
        
        if(inited.get()) {
            return ;
        }
        
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
        
        druidDataSource = DruidDataSourceFactory.createDataSource(properties);
        
        inited.compareAndSet(false, true);
        
    }
    
   
    public static void main(String[] args) {}
}
