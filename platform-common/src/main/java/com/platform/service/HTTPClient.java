package com.platform.service;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.google.common.collect.Maps;
import com.platform.utils.IOUtils;
import com.platform.utils.LangUtil;

import lombok.Getter;
import lombok.Setter;

public class HTTPClient {
    
    /**
    private static ThreadLocal<CloseableHttpClient> httpClients = new ThreadLocal<CloseableHttpClient>(){
        protected CloseableHttpClient initialValue() {
            return HttpClientBuilder.create().build();
        }
    };
    */
    
    public static HttpResponse get(String url, Map<String, String> headerKVs,
            Map<String, String> paramKVs, String paramCharSetDisplayName, HttpClient outterHttpClient) throws IOException {
        
        HttpClient httpClient = outterHttpClient;
        
        String paramString = null;
        if(paramKVs != null && !paramKVs.isEmpty()) {
            paramString = URLEncodedUtils.format(
                    paramKVs.entrySet().stream()
                    .map(param->{
                        return new BasicNameValuePair(param.getKey(), param.getValue());
                    })
                    .collect(Collectors.toList()),
                    paramCharSetDisplayName
                    );
        }
        url = paramString != null ? url + "?" + paramString : url;
        
        HttpGet httpGet = new HttpGet(url);
        headerKVs.entrySet().forEach(header->{
            httpGet.addHeader(header.getKey(), header.getValue());
        });
        
        return httpClient.execute(httpGet);
        
    }
    
    public static HttpResponse post(String url, Map<String, String> headerKVs,
            Map<String, String> paramKVs, String paramCharSetDisplayName, HttpClient outterHttpClient) throws IOException {
        
        HttpClient httpClient = outterHttpClient;
        
        HttpPost httpPost = new HttpPost(url);
        
        UrlEncodedFormEntity postEntity = new UrlEncodedFormEntity(
                paramKVs.entrySet().stream()
                .map(param->{
                    return new BasicNameValuePair(param.getKey(), param.getValue());
                })
                .collect(Collectors.toList()),
                paramCharSetDisplayName
        );
        
        httpPost.setEntity(postEntity);
        
        headerKVs.entrySet().forEach(header->{
            httpPost.addHeader(header.getKey(), header.getValue());
        });
        
        return httpClient.execute(httpPost);
        
    }
    
    public static void main(String[] args) throws Exception {
        // 创建HttpClient实例
        /**
        HttpClient client =  HttpClientBuilder.create().build();
        
        Map<String, String> headers = Maps.newHashMap();
        Map<String, String> params = Maps.newHashMap();
        
        HttpResponse response = get(
                "http://localhost:8080/platform/sys/menu/queryAll", 
                headers, params, "UTF-8", 
                client);
        
        System.out.println(response);
        */
        
        /**
        HttpClient client =  HttpClientBuilder.create().build();
        
        Map<String, String> headers = Maps.newHashMap();
        Map<String, String> params = Maps.newHashMap();
        params.put("api_name", "trade_cal");
        params.put("token", "9882ae8dabcd5504112f770d0ca3af8caf47c2348758ca9885a14414");
        params.put("params", "{\"exchange\":\"\", \"start_date\":\"20180901\", \"end_date\":\"20181001\", \"is_open\":\"0\"}");
        params.put("fields", "exchange,cal_date,is_open,pretrade_date");
        
        HttpResponse response = post(
                "http://api.tushare.pro", 
                headers, params, "UTF-8", 
                client);
        
        
        System.out.println(IOUtils.toString(response.getEntity().getContent(), "UTF-8"));
        System.out.println("");
        */
        
        /**
        HttpPost httpPost = new HttpPost("http://api.tushare.pro");
        
        Map<String, String> params = Maps.newLinkedHashMap();
        params.put("api_name", "trade_cal");
        params.put("token", "9882ae8dabcd5504112f770d0ca3af8caf47c2348758ca9885a14414");
        params.put("params", "{\"exchange\":\"\", \"start_date\":\"20180901\", \"end_date\":\"20181001\", \"is_open\":\"0\"}");
        params.put("fields", "exchange,cal_date,is_open,pretrade_date");
        
        StringBuffer sb = new StringBuffer();
        
        
        sb.append("{");
        boolean first = true;
        for(Map.Entry<String, String> param: params.entrySet()) {
            if(first) {
                sb.append("\"" + param.getKey() + "\"").append(":").append("\"" + param.getValue() + "\"");
                first = false;
            }else {
                if("params".equals(param.getKey())) {
                    sb.append(",")
                    .append("\"" + param.getKey() + "\"").append(":").append(param.getValue());
                }else {
                    sb.append(",")
                    .append("\"" + param.getKey() + "\"").append(":").append("\"" + param.getValue() + "\"");
                }
            }
        }
        sb.append("}");
        
        
        httpPost.setEntity(new StringEntity(sb.toString(), ContentType.APPLICATION_JSON));
        
        HttpClient httpClient =  HttpClientBuilder.create().build();
        HttpResponse response = httpClient.execute(httpPost);
        
        System.out.println(IOUtils.toString(response.getEntity().getContent(), "UTF-8"));
        System.out.println("");
        */
        
        Map<String, String> param = Maps.newHashMap();
        param.put("exchange", "");
        param.put("start_date", "20180901");
        param.put("end_date", "20181001");
        param.put("is_open", "0");
        
        A a = new A("trade_cal", "9882ae8dabcd5504112f770d0ca3af8caf47c2348758ca9885a14414", param, "exchange,cal_date,is_open,pretrade_date");
        
        HttpPost httpPost = new HttpPost("http://api.tushare.pro");
        
        httpPost.setEntity(new StringEntity(JSON.toJSONString(a), ContentType.APPLICATION_JSON));
        
        HttpClient httpClient =  HttpClientBuilder.create().build();
        HttpResponse response = httpClient.execute(httpPost);
        
        System.out.println(IOUtils.toString(response.getEntity().getContent(), "UTF-8"));
        
        System.out.println("");
        
        
        
        
        
    }
    
    public static final class A{
        @Getter @Setter @JSONField(name = "api_name", ordinal = 1) String apiName;
        @Getter @Setter @JSONField(ordinal = 2) String token;
        @Getter @JSONField(ordinal = 3) Map<String, String> params;
        @Getter @Setter @JSONField(ordinal = 4)String fields;
        
        public void setParams(String paramsString) {
            if(paramsString == null) {
                params = Maps.newHashMap();
            }
            
            Map<String, Object> paramsJSONObj = JSON.parseObject(paramsString);
            
            params = Maps.newLinkedHashMap();
            
            paramsJSONObj.entrySet().forEach(param->{
                params.put(param.getKey(), LangUtil.convert(param.getValue(), String.class));
            });
            
        }
        
        public A(String apiName, String token, Map<String, String> params, String fileds) {
            this.apiName = apiName;
            this.token = token;
            this.params = params;
            this.fields = fileds;
        }
        
        public A() {}
    }
    
    
    /**
     * 
     * curl -X POST -d 
     * '{"api_name": "trade_cal", "token": "9882ae8dabcd5504112f770d0ca3af8caf47c2348758ca9885a14414", "params": {"exchange":"", "start_date":"20180901", "end_date":"20181001", "is_open":"0"}, "fields": "exchange,cal_date,is_open,pretrade_date"}' 
     * 
     * http://api.tushare.pro
     * 
     * 
     * 
     * */
}
