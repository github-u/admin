package com.platform.utils;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

public class ConsoleUtil {

    public static final String commContentType = "text/html";

    public static final String commContentTypeWithCharSet = "text/html;charset=";

    public static final String defaultCharset = "UTF-8";
    
    private static Logger logger = LoggerFactory.getLogger(ConsoleUtil.class);
    
    public static void print(HttpServletResponse response, Object obj){
        print(response, obj, defaultCharset);
    }

    public static void print(HttpServletResponse response, Object obj, String charSet){
        
        int feature = JSON.DEFAULT_GENERATE_FEATURE 
                + SerializerFeature.DisableCircularReferenceDetect.getMask() 
                - SerializerFeature.SkipTransientField.getMask();
        
        print(response, obj, feature, charSet);
    }
    
    public static void jsonPrint(HttpServletResponse response, Object obj, int feature){
        
        print(response, obj, feature, defaultCharset);
        
    }
    
    public static void print(HttpServletResponse response, Object obj, int feature, String charSet){

        String contentType = commContentType;
        if(charSet == null){
            //nothing to do
        }else{
            contentType = commContentTypeWithCharSet + charSet;
        }

        response.setContentType(contentType);

        PrintWriter out = null;

        try {

            out = response.getWriter();

            out.print(JSON.toJSONString(obj, feature));

            out.flush();

            out.close();

        } catch (Exception e) {

            logger.error("Json print exception ", e);
        }

    }

}
