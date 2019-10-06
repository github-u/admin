package com.platform.controller;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.platform.entity.ResultSupport;
import com.platform.service.DataService;
import com.platform.service.impl.DataServiceImpl;
import com.platform.utils.ConsoleUtil;
import com.platform.utils.LangUtil;
import com.platform.utils.Pair;

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

@Controller
public class PageController {
    
    private DataService dataService;
    
    private AtomicBoolean inited = new AtomicBoolean(false);
    
    private static Logger logger = LoggerFactory.getLogger(PageController.class);
    
    public static class Constants{
        
        public static final String TableName = "tableName";
        
        public static final String Condition = "condition";
        
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
    public void meta(HttpServletResponse response) throws ServletException, IOException {
        
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
        
        inited.compareAndSet(false, true);
    }
    
    public static void main(String[] args) {
        Object obj = JSON.parseObject("{}");
        System.out.println(obj);
        
    }
    
}
