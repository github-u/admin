package com.platform.controller;

import com.alibaba.druid.sql.ast.statement.SQLColumnDefinition;
import com.alibaba.druid.sql.ast.statement.SQLTableElement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlCreateTableStatement;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.platform.entity.ResultSupport;
import com.platform.service.DataService;
import com.platform.service.SQLService;
import com.platform.service.impl.DataServiceImpl;
import com.platform.service.impl.SQLServiceImpl;
import com.platform.utils.ConsoleUtil;
import com.platform.utils.LangUtil;
import com.platform.utils.Pair;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Controller
public class PageController {
    
    private DataService dataService;
    
    private SQLService sqlService;
    
    private AtomicBoolean inited = new AtomicBoolean(false);
    
    private static Logger logger = LoggerFactory.getLogger(PageController.class);
    
    public static class Constants{
        
        public static final String TableName = "tableName";
        
        public static final String Condition = "condition";
        
        public static final String ComponentName = "componentName"; 
        
        public static final String Meta = "meta";
        
    }
    
    public PageController() throws Exception {
        init();
    }
    
    public static class PageControllerMode{
        public static final String Data = "Data";
    }
    
    public static class PageControllerResultCode{
        public static final String GetDataException = "GetDataException";
    }
    
    @RequestMapping(value = "/meta", method = RequestMethod.GET)
    public void meta(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        ResultSupport<List<Map<String, Object>>> ret = new ResultSupport<List<Map<String, Object>>>();
        
        try {
            Map<String, String> params = params(request);
            
            //String componentName = LangUtil.convert(params.get(Constants.ComponentName), String.class);
            String tableName = LangUtil.convert(params.get(Constants.TableName), String.class);
            
            //Map<String, Object> meta = Maps.newConcurrentMap();
            
            ResultSupport<Boolean> registeredSQLStatementRet = dataService.registeredSQLStatement(tableName);
            if(!registeredSQLStatementRet.isSuccess()) {
                ConsoleUtil.print(response, ret.fail(registeredSQLStatementRet.getErrCode(), registeredSQLStatementRet.getErrMsg()));
                return;
            }
            
            ResultSupport<MySqlCreateTableStatement> getSQLStatemenRet = ((DataServiceImpl)dataService).getSqlService().getSQLStatement(tableName);
            if(!getSQLStatemenRet.isSuccess()) {
                ConsoleUtil.print(response, ret.fail(getSQLStatemenRet.getErrCode(), getSQLStatemenRet.getErrMsg()));
                return;
            }
            
            List<FormItem> formItems = 
                    getSQLStatemenRet.getModel().getTableElementList().stream().map(sqlTableElement ->{
                        if(sqlTableElement instanceof SQLColumnDefinition) {
                            String name = ((SQLColumnDefinition) sqlTableElement).getName().getSimpleName();
                            String label = ((SQLColumnDefinition) sqlTableElement).getName().getSimpleName();
                            return new FormItem(name, label);
                        }else {
                            return null;
                        }
                    })
                    .filter(formItem -> formItem != null)
                    .collect(Collectors.toList());
            
            Map<String, Object> meta = Maps.newHashMap();
            meta.put("meta", new TableMeta(new Form(formItems)));
            
            ConsoleUtil.print(addCorsHeaders(request, response), meta);
            
        }catch(Exception e) {
            logger.error("title=" + "PageController"
                        + "$mode=" + PageControllerMode.Data
                        + "$errCode=" + PageControllerResultCode.GetDataException
                        + "$errMsg=" + "", e);
            
            ConsoleUtil.print(response, ret.fail(PageControllerResultCode.GetDataException, e.getMessage()));
        }
    }
    
    @RequestMapping(value = "/data", method = RequestMethod.GET)
    public void data(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        
        ResultSupport<List<Map<String, Object>>> ret = new ResultSupport<List<Map<String, Object>>>();
        
        try {
            Map<String, String> params = params(request);
            
            String tableName = LangUtil.convert(params.get(Constants.TableName), String.class);
            String condition = LangUtil.convert(params.get(Constants.Condition), String.class);
            
            Map<String, Object> conditionMap = JSON.parseObject(condition);
            
            ResultSupport<List<Map<String, Object>>> selectRet = dataService.select(tableName, conditionMap);
            
            ConsoleUtil.print(addCorsHeaders(request, response), selectRet);
        }catch(Exception e) {
            logger.error("title=" + "PageController"
                        + "$mode=" + PageControllerMode.Data
                        + "$errCode=" + PageControllerResultCode.GetDataException
                        + "$errMsg=" + "", e);
            
            ConsoleUtil.print(response, ret.fail(PageControllerResultCode.GetDataException, e.getMessage()));
        }
    }
    
    private Map<String, String> params(HttpServletRequest request){
        
        Map<String, String> ret = Maps.newLinkedHashMap();
        
        Map<String, String[]> requestParams = request.getParameterMap();
        
        ret = requestParams.entrySet().stream()
                .map(entry->{
                    String key = entry.getKey();
                    String[] vals = entry.getValue();
                    String val = vals != null && vals.length > 0 ? vals[0] : null;
                    return Pair.of(key, val);
                })
                .collect(
                        Collector.of(
                                ()->{
                                    Map<String, String> supplier = Maps.newLinkedHashMap();
                                    return supplier;
                                },
                                (supplier, elem)->{
                                    supplier.put(elem.fst, elem.snd);
                                }, 
                                (supplier1, supplier2)->{
                                    supplier1.putAll(supplier2);
                                    return supplier1;
                                },
                                Collector.Characteristics.IDENTITY_FINISH
                                )
                        );
        
        return ret;
    }
    
    private HttpServletResponse addCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        
        response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Headers", "x-requested-with,X-Nideshop-Token,X-URL-PATH");
        
        return response;
    }
    public void init() throws Exception {
        
        if(inited.get()) {
            return;
        }
        
        dataService = new DataServiceImpl();
        ((DataServiceImpl)dataService).init();
        
        sqlService = new SQLServiceImpl();
        ((SQLServiceImpl)sqlService).init();
        
        inited.compareAndSet(false, true);
    }
    
    public static void main(String[] args) {
        Object obj = JSON.parseObject("{}");
        System.out.println(obj);
        
    }
    
    public static class AbstractMeta{
        
    }
    
    public static final class TableMeta extends AbstractMeta{
        @Getter @Setter private Form form;
        public TableMeta(Form form) {
            this.form = form;
        }
    }
    
    public static final class Form{
        @Getter @Setter private List<FormItem> formItems = Lists.newArrayList();
        public Form(List<FormItem> formItems){
            this.formItems = formItems;
        }
    }
    
    public static final class FormItem{
        @Getter @Setter private String prop;
        @Getter @Setter private String label;
        
        public FormItem(String prop, String label) {
            this.prop = prop;
            this.label = label;
        }
    }
    
    
}
