package com.platform.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.platform.entity.ResultSupport;
import com.platform.service.CNInfoService;
import com.platform.utils.IOUtils;
import com.platform.utils.LangUtil;

public class CNInfoServiceImpl implements CNInfoService {
    
    private static Logger logger = LoggerFactory.getLogger(CNInfoServiceImpl.class);
    
    private static final String URL = "http://www.cninfo.com.cn/new/disclosure?column=szse_latest&pageNum=1&pageSize=20";
    
    public static void main(String[] args) {
        
        JSONObject pageInfo = annoucements(disclosureUri(1));
        Long pages = LangUtil.convert(pageInfo.get("totalpages"), Long.class);
        List<Object> classifiedAnnouncements = Lists.newArrayList();
        for(int i=1; i < pages; i++) {
            JSONObject a = annoucements(disclosureUri(i));
            List<Object> t = (List<Object>) a.get("classifiedAnnouncements");
            classifiedAnnouncements.addAll(t);
        }
        
        List<JSONObject> l = Lists.newArrayList();
        for(int i = 0; i < classifiedAnnouncements.size(); i++) {
            JSONObject j = (JSONObject) ((List)classifiedAnnouncements.get(i)).get(0);
            String announcementTypeName = LangUtil.convert(j.get("announcementTypeName"), String.class);
            if("业绩预告".equals(announcementTypeName)) {
               l.add(j);
            }
        }
        
        System.out.println(l);
        
    }
    
    public static JSONObject annoucements(String uri){
        
        HttpPost httpGet = new HttpPost(uri);
        //httpGet.setEntity(new StringEntity(JSON.toJSONString(tuShareParam), ContentType.APPLICATION_JSON));
        
        HttpClient httpClient = HttpClientBuilder.create().build();

        try {
            HttpResponse response = httpClient.execute(httpGet);
            if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                logger.error("title=" + "CNInfoServiceImpl"
                        + "$mode=" + "getData"
                        + "$errCode=" + "HTTP_STATUS_ILLEGAL"
                        + "$errMsg=" + JSON.toJSONString(response.getStatusLine()));
                System.out.println(JSON.toJSONString(response.getStatusLine()));
            }
            
            String httpEntity = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
            
            JSONObject jsonResult = JSON.parseObject(httpEntity);
            
            System.out.println(httpEntity);
            
            return jsonResult;
            
        } catch (Exception e) {
            logger.error("title=" + "TuShareService"
                        + "$mode=" + "getData"
                        + "$errCode=" + "HTTP_CLIENT_EXECUTE_EXCEPTION"
                        + "$errMsg=", e);
            
            System.out.println(e);
            
            return null;
        } finally {
            HttpClientUtils.closeQuietly(httpClient);
        }
        
    }
    
    public static String disclosureUri(long pageIndex) {
        return "http://www.cninfo.com.cn/new/disclosure?column=szse_latest"
                + "&pageNum=" + pageIndex
                + "&pageSize=20";
        
    }
}
