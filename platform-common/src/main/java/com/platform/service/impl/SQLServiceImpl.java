package com.platform.service.impl;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collector;

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
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlCreateTableStatement;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlOutputVisitor;
import com.google.common.collect.Lists;
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
        createTableDDLSQLStatements.put(sqlIdentifierExpr.getName().replace(Constans.ASCII126, ""), createTableSQLStatementRet.getModel());
        
        return ret.success(Boolean.TRUE);
    }
    
    @Override
    public ResultSupport<Pair<String, Map<Integer, PreparedStatementValue>>> getSelect(String tableName, Map<String, Object> params) {
        
        ResultSupport<Pair<String, Map<Integer, PreparedStatementValue>>> ret = new ResultSupport<Pair<String, Map<Integer, PreparedStatementValue>>> (); 
        
        Map<String, SQLColumnDefinition> tableColumnElements = getTableColumnElements(tableName);
        
        Map<SQLColumnDefinition, Object> tableCoditionMap = getTableCodition(tableColumnElements, params);
        
        VelocityContext velocityContext = getSelectVelocityContext(tableName, tableColumnElements, tableCoditionMap, params);
        
        ResultSupport<String> templateSqlRet = templateSql(Constans.DefaultSelectVmPath, velocityContext);
        if(!templateSqlRet.isSuccess()) {
            return ret.fail(templateSqlRet.getErrCode(), templateSqlRet.getErrMsg());
        }
        
        ResultSupport<Map<Integer, PreparedStatementValue>> preparedStatementParamsRet = preparedStatementParams(tableCoditionMap);
        if(!preparedStatementParamsRet.isSuccess()) {
            return ret.fail(preparedStatementParamsRet.getErrCode(), preparedStatementParamsRet.getErrMsg());
        }
        
        preparedStatementParamsRet.getModel().put(preparedStatementParamsRet.getModel().size() + 1, 
                new PreparedStatementValue(new SQLColumnDefinition(), params.get(VelocityContextKey.Start) != null ? get(VelocityContextKey.Start, params) : Constans.DefaultStart));
        preparedStatementParamsRet.getModel().put(preparedStatementParamsRet.getModel().size() + 1, 
                new PreparedStatementValue(new SQLColumnDefinition(), params.get(VelocityContextKey.Limit) != null ? get(VelocityContextKey.Limit, params) : Constans.DefaultLimit));
        
        return ret.success(Pair.of(templateSqlRet.getModel(), preparedStatementParamsRet.getModel()));
        
    }

    @Override
    public ResultSupport<Pair<String, Map<Integer, PreparedStatementValue>>> getUpdate(String tableName, Map<String, Object> params) {
        
        ResultSupport<Pair<String, Map<Integer, PreparedStatementValue>>> ret = new ResultSupport<Pair<String, Map<Integer, PreparedStatementValue>>> (); 
        
        Map<String, SQLColumnDefinition> tableColumnElements = getUpdateTableColumnElements(tableName);
        
        Map<SQLColumnDefinition, Object> tableUpdateColumnMap = getTableUpdateColumn(tableColumnElements, params);
        
        VelocityContext velocityContext = getUpdateVelocityContext(tableName, tableColumnElements, tableUpdateColumnMap, params);
        
        ResultSupport<String> templateSqlRet = templateSql(Constans.DefaultUpdateVmPath, velocityContext);
        if(!templateSqlRet.isSuccess()) {
            return ret.fail(templateSqlRet.getErrCode(), templateSqlRet.getErrMsg());
        }
        
        ResultSupport<Map<Integer, PreparedStatementValue>> preparedStatementParamsRet = preparedStatementParams(tableUpdateColumnMap);
        if(!preparedStatementParamsRet.isSuccess()) {
            return ret.fail(preparedStatementParamsRet.getErrCode(), preparedStatementParamsRet.getErrMsg());
        }
        
        return ret.success(Pair.of(templateSqlRet.getModel(), preparedStatementParamsRet.getModel()));
        
    }

    @Override
    public ResultSupport<Pair<String, Map<Integer, PreparedStatementValue>>> getInsert(String tableName, Map<String, Object> params) {
        
        ResultSupport<Pair<String, Map<Integer, PreparedStatementValue>>> ret = new ResultSupport<Pair<String, Map<Integer, PreparedStatementValue>>> (); 
        
        Map<String, SQLColumnDefinition> tableColumnElements = getInsertTableColumnElements(tableName);
        
        Map<SQLColumnDefinition, Object> tableInsertColumnMap = getTableInsertColumn(tableColumnElements, params);
        
        VelocityContext velocityContext = getInsertVelocityContext(tableName, tableColumnElements, tableInsertColumnMap, params);
        
        ResultSupport<String> templateSqlRet = templateSql(Constans.DefaultInsertVmPath, velocityContext);
        if(!templateSqlRet.isSuccess()) {
            return ret.fail(templateSqlRet.getErrCode(), templateSqlRet.getErrMsg());
        }
        
        ResultSupport<Map<Integer, PreparedStatementValue>> preparedStatementParamsRet = preparedStatementParams(tableInsertColumnMap);
        if(!preparedStatementParamsRet.isSuccess()) {
            return ret.fail(preparedStatementParamsRet.getErrCode(), preparedStatementParamsRet.getErrMsg());
        }
        
        return ret.success(Pair.of(templateSqlRet.getModel(), preparedStatementParamsRet.getModel()));
        
    }

    @Override
    public ResultSupport<Pair<String, Map<Integer, PreparedStatementValue>>> getDelete(String tableName, Map<String, Object> params) {
        
        Map<String, Object> tParams = new HashMap<String, Object>();
        tParams.put(PreSetColumn.Id, get(PreSetColumn.Id, params));
        tParams.put(PreSetColumn.Status, Constans.DeleteStatus);
        
        return getUpdate(tableName, tParams);
        
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
    
    private ResultSupport<Map<Integer, PreparedStatementValue>> preparedStatementParams(Map<SQLColumnDefinition, Object> tableCoditionMap){
        
        ResultSupport<Map<Integer, PreparedStatementValue>> ret = new ResultSupport<Map<Integer, PreparedStatementValue>>();
        
        AtomicInteger index = new AtomicInteger(1);
        Map<Integer, PreparedStatementValue> model = tableCoditionMap.entrySet().stream().collect(
                Collector.of(  
                                ()->{
                                    Map<Integer, PreparedStatementValue> s = new LinkedHashMap<Integer, PreparedStatementValue>();
                                    return s;
                                },
                                (s, e)->{
                                    if(e.getValue() instanceof DateValue) {
                                        if(((DateValue)e.getValue()).getStart() != null) {
                                            s.put(index.getAndIncrement(), new PreparedStatementValue(e.getKey(), ((DateValue) e.getValue()).getStart()));
                                        }
                                        if(((DateValue)e.getValue()).getEnd() != null) {
                                            s.put(index.getAndIncrement(), new PreparedStatementValue(e.getKey(), ((DateValue) e.getValue()).getEnd()));
                                        }
                                    }else {
                                        s.put(index.getAndIncrement(), new PreparedStatementValue(e.getKey(), e.getValue()));
                                    }
                                }, 
                                (s1, s2)->{
                                    s1.putAll(s2);
                                    return s1;
                                },
                                Collector.Characteristics.IDENTITY_FINISH
                             )
                );
        
        return ret.success(model);
        
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
    
    private Map<String, SQLColumnDefinition> getUpdateTableColumnElements(String tableName){
        Map<String, SQLColumnDefinition> tableColumnElements = getTableColumnElements(tableName);
        
        tableColumnElements.remove(PreSetColumn.Id);
        tableColumnElements.remove(PreSetColumn.GmtCreate);
        tableColumnElements.remove(PreSetColumn.GmtModified);
        tableColumnElements.remove(PreSetColumn.Version);
        
        return tableColumnElements;
    }
    
    private Map<String, SQLColumnDefinition> getInsertTableColumnElements(String tableName){
        Map<String, SQLColumnDefinition> tableColumnElements = getTableColumnElements(tableName);
        
        tableColumnElements.remove(PreSetColumn.Id);
        tableColumnElements.remove(PreSetColumn.GmtCreate);
        tableColumnElements.remove(PreSetColumn.GmtModified);
        
        return tableColumnElements;
    }
    
    private Map<String, SQLColumnDefinition> getTableColumnElements(String tableName) {
        
        MySqlCreateTableStatement mySqlCreateTableStatement = get(tableName, createTableDDLSQLStatements);
        
        Map<String, SQLColumnDefinition> tableColumnElements = 
                mySqlCreateTableStatement.getTableElementList()
                .stream()
                .filter(tableColumnElement -> tableColumnElement instanceof SQLColumnDefinition)
                .map(tableColumnElement -> (SQLColumnDefinition)tableColumnElement)
                .filter(tableColumnElement -> !tableColumnElement.getName().getSimpleName().replace(Constans.ASCII126 , "").equals(PreSetColumn.GmtCreate)
                        && !tableColumnElement.getName().getSimpleName().replace(Constans.ASCII126 , "").equals(PreSetColumn.GmtModified))
                .collect(
                        Collector.of(
                                ()->{
                                    Map<String, SQLColumnDefinition> s = new LinkedHashMap<String, SQLColumnDefinition>();
                                    return s;
                                },
                                (s, e)->{
                                    s.put(e.getName().getSimpleName().replace(Constans.ASCII126, ""), e);
                                }, 
                                (s1, s2)->{
                                    s1.putAll(s2);
                                    return s1;
                                },
                                Collector.Characteristics.IDENTITY_FINISH
                                )
                        );
        
        return tableColumnElements;
        
    }
    
    private Map<SQLColumnDefinition, Object> getTableCodition(Map<String, SQLColumnDefinition> tableColumnElements, Map<String, Object> params) {
        Map<SQLColumnDefinition, Object> tableCoditionMap = 
                tableColumnElements
                .values()
                .stream()
                .filter(tableColumnElement -> {
                    if(Constans.DateTimeType.equals(tableColumnElement.getDataType().getName())) {
                        return contains(tableColumnElement.getName().getSimpleName() + Constans.DateTimeStartSuffix, params)
                                || contains(tableColumnElement.getName().getSimpleName() + Constans.DateTimeEndSuffix, params);
                    }else {
                        return contains(tableColumnElement.getName().getSimpleName(), params);
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
                                                get(e.getName().getSimpleName() + Constans.DateTimeStartSuffix, params),
                                                get(e.getName().getSimpleName() + Constans.DateTimeEndSuffix, params)
                                                ));
                                    }else if(Constans.StringType.equals(e.getDataType().getName())){
                                        if(get(e.getName().getSimpleName(), params) != null) {
                                            s.put(e, Constans.PercentSign + get(e.getName().getSimpleName(), params) + Constans.PercentSign);
                                        }else {
                                            s.put(e, null);
                                        }
                                    }else {
                                        s.put(e, get(e.getName().getSimpleName(), params));
                                    }
                                }, 
                                (s1, s2)->{
                                    s1.putAll(s2);
                                    return s1;
                                },
                                Collector.Characteristics.IDENTITY_FINISH
                                )
                );
        
        return tableCoditionMap;
    }
    
    private Map<SQLColumnDefinition, Object> getTableUpdateColumn(Map<String, SQLColumnDefinition> tableColumnElements, Map<String, Object> params) {
        Map<SQLColumnDefinition, Object> tableUpdateColumnMap = 
                tableColumnElements
                .values()
                .stream()
                .filter(tableColumnElement -> {
                        return contains(tableColumnElement.getName().getSimpleName(), params);
                })
                .collect(Collector.of(
                                ()->{
                                    Map<SQLColumnDefinition, Object> s = new LinkedHashMap<SQLColumnDefinition, Object>();
                                    return s;
                                },
                                (s, e)->{
                                    s.put(e, get(e.getName().getSimpleName(), params));
                                }, 
                                (s1, s2)->{
                                    s1.putAll(s2);
                                    return s1;
                                },
                                Collector.Characteristics.IDENTITY_FINISH
                                )
                );
        return tableUpdateColumnMap;
    }
    
    private Map<SQLColumnDefinition, Object> getTableInsertColumn(Map<String, SQLColumnDefinition> tableColumnElements, Map<String, Object> params) {
        Map<SQLColumnDefinition, Object> tableInsertColumnMap = 
                tableColumnElements
                .values()
                .stream()
                .collect(Collector.of(
                                ()->{
                                    Map<SQLColumnDefinition, Object> s = new LinkedHashMap<SQLColumnDefinition, Object>();
                                    return s;
                                },
                                (s, e)->{
                                    s.put(e, get(e.getName().getSimpleName(), params));
                                }, 
                                (s1, s2)->{
                                    s1.putAll(s2);
                                    return s1;
                                },
                                Collector.Characteristics.IDENTITY_FINISH
                                )
                );
        return tableInsertColumnMap;
    }
    
    private VelocityContext getSelectVelocityContext(String tableName, Map<String, SQLColumnDefinition> tableColumnElements, 
            Map<SQLColumnDefinition, Object> tableCoditionMap, Map<String, Object> params) {
        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put(VelocityContextKey.TableName, tableName);
        velocityContext.put(VelocityContextKey.Columns, Lists.newArrayList(tableColumnElements.values()));
        velocityContext.put(VelocityContextKey.Conditions, tableCoditionMap);
        velocityContext.put(VelocityContextKey.Start, 
                params.get(VelocityContextKey.Start) != null ? get(VelocityContextKey.Start, params) : Constans.DefaultStart);
        velocityContext.put(VelocityContextKey.Limit, 
                params.get(VelocityContextKey.Limit) != null ? get(VelocityContextKey.Limit, params) : Constans.DefaultLimit);
        return velocityContext;
    }
    
    private VelocityContext getUpdateVelocityContext(String tableName, Map<String, SQLColumnDefinition> tableColumnElements, 
            Map<SQLColumnDefinition, Object> tableUpdateColumnMap, Map<String, Object> params) {
        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put(VelocityContextKey.TableName, tableName);
        velocityContext.put(VelocityContextKey.Columns, Lists.newArrayList(tableColumnElements.values()));
        velocityContext.put(VelocityContextKey.UpdateColumns, tableUpdateColumnMap);
        velocityContext.put(VelocityContextKey.Start, 
                params.get(VelocityContextKey.Start) != null ? get(VelocityContextKey.Start, params) : Constans.DefaultStart);
        velocityContext.put(VelocityContextKey.Limit, 
                params.get(VelocityContextKey.Limit) != null ? get(VelocityContextKey.Limit, params) : Constans.DefaultLimit);
        velocityContext.put(PreSetColumn.Id, 
                params.get(PreSetColumn.Id) != null ? get(PreSetColumn.Id, params) : Constans.DefaultRowId);
        return velocityContext;
    }
    
    private VelocityContext getInsertVelocityContext(String tableName, Map<String, SQLColumnDefinition> tableColumnElements, 
            Map<SQLColumnDefinition, Object> tableInsertColumnMap, Map<String, Object> params) {
        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put(VelocityContextKey.TableName, tableName);
        velocityContext.put(VelocityContextKey.Columns, Lists.newArrayList(tableColumnElements.values()));
        velocityContext.put(VelocityContextKey.InsertColumns, tableInsertColumnMap);
        velocityContext.put(VelocityContextKey.Start, 
                params.get(VelocityContextKey.Start) != null ? get(VelocityContextKey.Start, params) : Constans.DefaultStart);
        velocityContext.put(VelocityContextKey.Limit, 
                params.get(VelocityContextKey.Limit) != null ? get(VelocityContextKey.Limit, params) : Constans.DefaultLimit);
        velocityContext.put(PreSetColumn.Id, 
                params.get(PreSetColumn.Id) != null ? get(PreSetColumn.Id, params) : Constans.DefaultRowId);
        return velocityContext;
    }
    
    private static <T> T get(String key, Map<String, T> params) {
        return params.get(key) != null ? 
                    params.get(key) : 
                    params.get(key.replace(Constans.ASCII126, ""));
    }
    
    private static boolean contains(String key, Map<String, Object> params) {
        return params.containsKey(key) ? 
                    params.containsKey(key) :
                    params.containsKey(key.replace(Constans.ASCII126, ""));
    }
    
    public static void main(String[] args) throws Exception {
        
        LinkedHashMap<String , String> m = new LinkedHashMap<String, String>();
        m.put("1", "1");
        m.put("2", "2");
        m.put("3", "3");
        m.put("4", "4");
        
        System.out.println(Lists.newArrayList(m.values()));
        //testParseTest();
        
        //selectTest();
        
        //updateTest();
        
        //insertTest();
        
        //deleteTest();
    }
    
    public static void testParseTest() {
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
        params.put("role_id", 1);
        params.put("role_name", "abc");
        params.put("create_time.start", "2019-08-20 12:34:56");
        params.put("create_time.end", "2019-09-20 12:34:56");
        params.put("__start", "1");
        params.put("__limit", "10");
        System.out.println(sqlServiceImpl.getSelect("`sys_role`", params).getModel());
    }
    
    @Test
    public static void updateTest() throws Exception {
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
        params.put(PreSetColumn.Id, 1);
        params.put("role_name", "abc");
        params.put("create_time", "2019-08-20 12:34:56");
        System.out.println(sqlServiceImpl.getUpdate("`sys_role`", params).getModel());
    }
    
    @Test
    public static void insertTest() throws Exception {
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
        params.put(PreSetColumn.Id, 1);
        params.put("role_name", "abc");
        params.put("create_time", "2019-08-20 12:34:56");
        System.out.println(sqlServiceImpl.getInsert("`sys_role`", params).getModel());
    }
    
    @Test
    public static void deleteTest() throws Exception {
        String sql = "CREATE TABLE `sys_role` ("
                + "`role_id` bigint(20) NOT NULL AUTO_INCREMENT,"
                +  "`role_name` varchar(100) DEFAULT NULL COMMENT '角色名称',"
                +  "`remark` varchar(100) DEFAULT NULL COMMENT '备注',"
                +  "status bigint(20) DEFAULT NULL COMMENT '状态',"
                +  "`create_user_id` bigint(20) DEFAULT NULL COMMENT '创建者ID',"
                +  "`create_time` datetime DEFAULT NULL COMMENT '创建时间',"
                +  "`dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID',"
                +  "PRIMARY KEY (`role_id`)"
                +  ") ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8 COMMENT='角色'";
        
        SQLServiceImpl sqlServiceImpl = new SQLServiceImpl();
        sqlServiceImpl.init();
        sqlServiceImpl.generateSQLStatement(sql);
        
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(PreSetColumn.Id, 1);
        System.out.println(sqlServiceImpl.getDelete("`sys_role`", params).getModel());
    }
    
}
