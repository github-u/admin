package com.platform.entity.tushare;

import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.annotation.JSONField;
import com.platform.annotation.FieldAnnotation;

import lombok.Getter;
import lombok.Setter;

public class TuShareData {
    
    @Getter @Setter List<String> fields;
    
    @Getter @Setter List<Map<String, String>> items;
    
    @Getter @Setter @FieldAnnotation(alias = "has_more") Boolean hasMore;
}
