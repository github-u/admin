package com.platform.service.impl;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.statement.SQLColumnDefinition;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlCreateTableStatement;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlOutputVisitor;
import com.platform.entity.ResultSupport;
import com.platform.service.SQLService;
import com.platform.utils.Pair;

public class SQLServiceImpl implements SQLService {
    
    private VelocityEngine velocityEngine;
    
    private AtomicBoolean inited = new AtomicBoolean(false);
    
    private Map<String, MySqlCreateTableStatement> createTableDDLSQLStatements = new LinkedHashMap<String, MySqlCreateTableStatement>();
    
    private static Logger logger = LoggerFactory.getLogger(SQLServiceImpl.class);
    
    @Override
    public ResultSupport<Boolean> generateSQLStatement(String createTableDDL) {
        ResultSupport<Boolean> ret = new ResultSupport<Boolean>();
        
        ResultSupport<MySqlCreateTableStatement> createTableSQLStatementRet = createTableSQLStatement(createTableDDL);
        if(!createTableSQLStatementRet.isSuccess()) {
            return ret.fail(createTableSQLStatementRet.getErrCode(), createTableSQLStatementRet.getErrMsg());
        }
        SQLIdentifierExpr sqlIdentifierExpr = (SQLIdentifierExpr) createTableSQLStatementRet.getModel().getTableSource().getExpr();
        createTableDDLSQLStatements.put(sqlIdentifierExpr.getName(), createTableSQLStatementRet.getModel());
        
        return ret.success(Boolean.TRUE);
    }
    
    @Override
    public ResultSupport<String> getSelect(String tableName, Map<String, Object> params) {
        
        MySqlCreateTableStatement mySqlCreateTableStatement = createTableDDLSQLStatements.get(tableName);
        
        List<SQLColumnDefinition> tableColumnElements = 
                mySqlCreateTableStatement.getTableElementList()
                .stream()
                .filter(tableColumnElement -> tableColumnElement instanceof SQLColumnDefinition)
                .map(tableColumnElement -> (SQLColumnDefinition)tableColumnElement)
                .collect(Collectors.toList());
        
        Map<SQLColumnDefinition, Object> tableCoditionMap = 
                tableColumnElements
                .stream()
                .filter(tableColumnElement -> {
                    if(Constans.DateTimeType.equals(tableColumnElement.getDataType().getName())) {
                        return params.containsKey(tableColumnElement.getName().getSimpleName() + Constans.DateTimeStartSuffix)
                                || params.containsKey(tableColumnElement.getName().getSimpleName() + Constans.DateTimeEndSuffix);
                    }else {
                        return params.containsKey(tableColumnElement.getName().getSimpleName());
                    }
                })
                .collect(Collector.of(
                                ()->{
                                    Map<SQLColumnDefinition, Object> s = new LinkedHashMap<SQLColumnDefinition, Object>();
                                    return s;
                                },
                                (s, e)->{
                                    if(Constans.DateTimeType.equals(e.getDataType().getName())) {
                                        s.put(e, new DateValue(
                                                params.get(e.getName().getSimpleName() + Constans.DateTimeStartSuffix),
                                                params.get(e.getName().getSimpleName() + Constans.DateTimeEndSuffix)
                                                ));
                                    }else {
                                        s.put(e, params.get(e.getName().getSimpleName()));
                                    }
                                }, 
                                (s1, s2)->{
                                    s1.putAll(s2);
                                    return s1;
                                },
                                Collector.Characteristics.IDENTITY_FINISH
                                )
                );
        
        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put(VelocityContextKey.TableName, tableName);
        velocityContext.put(VelocityContextKey.Columns, tableColumnElements);
        velocityContext.put(VelocityContextKey.Conditions, tableCoditionMap);
        velocityContext.put(VelocityContextKey.Start, 
                params.get(VelocityContextKey.Start) != null ? params.get(VelocityContextKey.Start) : Constans.DefaultStart);
        velocityContext.put(VelocityContextKey.Limit, 
                params.get(VelocityContextKey.Limit) != null ? params.get(VelocityContextKey.Limit) : Constans.DefaultLimit);
        
        return templateSql(Constans.DefaultSelectVmPath, velocityContext);
        
    }

    @Override
    public ResultSupport<String> getUpdate(String tableName, Map<String, Object> params) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSupport<String> getInsert(String tableName, Map<String, Object> params) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSupport<String> getDelete(String tableName, Map<String, Object> params) {
        // TODO Auto-generated method stub
        return null;
    }
    
    public void init() throws Exception {
        if(inited.get()) {
            return;
        }
        
        velocityEngine = new VelocityEngine();
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        velocityEngine.init();
        
        inited.compareAndSet(false, true);
    }
    
    private ResultSupport<String> templateSql(String templateName, VelocityContext velocityContext){
        
         ResultSupport<String> ret = new ResultSupport<String>();
         
         Template template = velocityEngine.getTemplate(templateName);
         
         StringWriter stringWriter = new StringWriter();
         
         template.merge(velocityContext, stringWriter);
         
         return ret.success(stringWriter.toString());
         
    }
    
    private ResultSupport<MySqlCreateTableStatement> createTableSQLStatement(String createTableDDLSQL){
        
        ResultSupport<MySqlCreateTableStatement> ret = new ResultSupport<MySqlCreateTableStatement>();
        MySqlStatementParser mySqlStatementParser = new MySqlStatementParser(createTableDDLSQL);
        List<SQLStatement> sqlStatements = mySqlStatementParser.parseStatementList();
        
        if(sqlStatements.size() < 1) {
            logger.error("title=" + "SQLServiceImpl"
                        + "$mode=" + SQLServiceModeCode.CreateTableSQLStatement
                        + "$errCode=" + SQLServiceResultCode.NoneCreateTableSQLFound
                        + "$errMsg=" + createTableDDLSQL);
            return ret.fail(SQLServiceResultCode.NoneCreateTableSQLFound, "");
        }
        
        if(!(sqlStatements.get(0) instanceof MySqlCreateTableStatement)) {
            logger.error("title=" + "SQLServiceImpl"
                        + "$mode=" + SQLServiceModeCode.CreateTableSQLStatement
                        + "$errCode=" + SQLServiceResultCode.NoneCreateTableStatementFound
                        + "$errMsg=" + createTableDDLSQL);
            return ret.fail(SQLServiceResultCode.NoneCreateTableStatementFound, "");
        }
        
        return ret.success((MySqlCreateTableStatement) sqlStatements.get(0));
    }
    
    public static void main(String[] args) throws Exception {
        /**
        StringBuilder out = new StringBuilder();
        MySqlOutputVisitor visitor = new MySqlOutputVisitor(out);
        
        String sql = "CREATE TABLE `sys_role` ("
                + "`role_id` bigint(20) NOT NULL AUTO_INCREMENT,"
                +  "`role_name` varchar(100) DEFAULT NULL COMMENT '角色名称',"
                +  "`remark` varchar(100) DEFAULT NULL COMMENT '备注',"
                +  "`create_user_id` bigint(20) DEFAULT NULL COMMENT '创建者ID',"
                +  "`create_time` datetime DEFAULT NULL COMMENT '创建时间',"
                +  "`dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID',"
                +  "PRIMARY KEY (`role_id`)"
                +  ") ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8 COMMENT='角色'";
                        
        MySqlStatementParser mySqlStatementParser = new MySqlStatementParser(sql);
        List<SQLStatement> sqlStatements = mySqlStatementParser.parseStatementList();
        for(SQLStatement sqlStatement : sqlStatements) {
            sqlStatement.accept(visitor);;
            visitor.println();
        }
        
        System.out.println(out.toString());
        */
        
        selectTest();
    }
    
    @Test
    public static void selectTest() throws Exception {
        String sql = "CREATE TABLE `sys_role` ("
                + "`role_id` bigint(20) NOT NULL AUTO_INCREMENT,"
                +  "`role_name` varchar(100) DEFAULT NULL COMMENT '角色名称',"
                +  "`remark` varchar(100) DEFAULT NULL COMMENT '备注',"
                +  "`create_user_id` bigint(20) DEFAULT NULL COMMENT '创建者ID',"
                +  "`create_time` datetime DEFAULT NULL COMMENT '创建时间',"
                +  "`dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID',"
                +  "PRIMARY KEY (`role_id`)"
                +  ") ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8 COMMENT='角色'";
        
        SQLServiceImpl sqlServiceImpl = new SQLServiceImpl();
        sqlServiceImpl.init();
        sqlServiceImpl.generateSQLStatement(sql);
        
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("`role_id`", 1);
        params.put("`role_name`", "abc");
        params.put("`create_time`.start", "2019-08-20 12:34:56");
        params.put("`create_time`.end", "2019-09-20 12:34:56");
        params.put("__start", "1");
        params.put("__limit", "10");
        System.out.println(sqlServiceImpl.getSelect("`sys_role`", params).getModel());
    }
    
}
