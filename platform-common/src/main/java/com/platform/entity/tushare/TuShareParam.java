package com.platform.entity.tushare;

import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.google.common.collect.Maps;
import com.platform.utils.LangUtil;

import lombok.Getter;
import lombok.Setter;

public class TuShareParam {
    
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
    
    public TuShareParam(String apiName, String token, Map<String, String> params, String fileds) {
        this.apiName = apiName;
        this.token = token;
        this.params = params;
        this.fields = fileds;
    }
    
    public TuShareParam() {}
    
}
