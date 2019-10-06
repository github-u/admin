package com.platform.service.impl;

import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.platform.entity.ResultSupport;
import com.platform.entity.tushare.TuShareData;
import com.platform.entity.tushare.TuShareParam;
import com.platform.entity.tushare.TuShareResult;
import com.platform.service.TuShareService;
import com.platform.utils.BeanUtil;
import com.platform.utils.IOUtils;

public class TuShareServiceImpl implements TuShareService {
    
    public static final String TUSHARE_URL = "http://api.tushare.pro";
    
    private static Logger logger = LoggerFactory.getLogger(TuShareServiceImpl.class);
            
    private static ThreadLocal<HttpClient> httpClients = new ThreadLocal<HttpClient>(){
        protected HttpClient initialValue() {
            return HttpClientBuilder.create().build();
        }
    };
    
    @Override
    public ResultSupport<TuShareData> getData(TuShareParam tuShareParam){
        
        ResultSupport<TuShareData> ret = new ResultSupport<TuShareData> ();
        
        HttpPost httpPost = new HttpPost(TUSHARE_URL);
        httpPost.setEntity(new StringEntity(JSON.toJSONString(tuShareParam), ContentType.APPLICATION_JSON));
        
        HttpClient httpClient =  httpClients.get();
        try {
            HttpResponse response = httpClient.execute(httpPost);
            if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                logger.error("title=" + "TuShareService"
                        + "$mode=" + "getData"
                        + "$errCode=" + ResultCode.HTTP_STATUS_ILLEGAL
                        + "$errMsg=" + JSON.toJSONString(response.getStatusLine()));
                return ret.fail(ResultCode.HTTP_STATUS_ILLEGAL, JSON.toJSONString(response.getStatusLine()));
            }
            
            String httpEntity = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
            
            JSONObject jsonTuShareResult = JSON.parseObject(httpEntity);
            JSONObject jsonTuShareData = jsonTuShareResult.getJSONObject("data");
            
            TuShareResult tuShareResult = BeanUtil.copyPropertiesFrom(jsonTuShareResult, TuShareResult.class);
            if(tuShareResult.getCode() != TuShareResultCode.SUC) {
                logger.error("title=" + "TuShareService"
                        + "$mode=" + "getData"
                        + "$errCode=" + ResultCode.TUSHARE_SERVER_ERROR
                        + "$errMsg=" + JSON.toJSONString(tuShareResult));
                return ret.fail(ResultCode.TUSHARE_SERVER_ERROR, JSON.toJSONString(tuShareResult));
            }
            TuShareData tuShareData = BeanUtil.copyPropertiesFrom(jsonTuShareData, TuShareData.class);
            
            return ret.success(tuShareData);
            
        } catch (Exception e) {
            logger.error("title=" + "TuShareService"
                        + "$mode=" + "getData"
                        + "$errCode=" + ResultCode.HTTP_CLIENT_EXECUTE_EXCEPTION
                        + "$errMsg=", e);
            return ret.fail(ResultCode.HTTP_CLIENT_EXECUTE_EXCEPTION, e.getMessage());
        }
        
    }
    
    public static void main(String[] args) {
        
        sample();
        
    }
    
    public static void sample() {
        
        TuShareService tuShareService = new TuShareServiceImpl();
        
        Map<String, String> param = Maps.newHashMap();
        param.put("exchange", "");
        param.put("start_date", "20180901");
        param.put("end_date", "20181001");
        param.put("is_open", "0");
        TuShareParam tuShareParam = new TuShareParam("trade_cal", "9882ae8dabcd5504112f770d0ca3af8caf47c2348758ca9885a14414", param, "exchange,cal_date,is_open,pretrade_date");
        
        ResultSupport<TuShareData> r = tuShareService.getData(tuShareParam);
        
        System.out.println(r);
        
    }
    
}
