package com.platform.service;

import java.io.IOException;

import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.context.support.XmlWebApplicationContext;

public class PathMatchingTest extends PathMatchingResourcePatternResolver{
    
    public static void main(String[] args) throws Exception {
        
        String location1 = "classpath:spring-mvc.xml";
        
        String location2 = "classpath*:/spring/platform-*.xml";
        
        XmlWebApplicationContext c = new XmlWebApplicationContext();
        
        c.getResources(location2);
        
    }

}
