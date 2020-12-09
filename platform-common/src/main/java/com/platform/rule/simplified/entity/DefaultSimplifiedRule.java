package com.platform.rule.simplified.entity;

import com.platform.rule.simplified.SimplifiedRule;

import lombok.Setter;

public class DefaultSimplifiedRule implements SimplifiedRule{
    
    public DefaultSimplifiedRule(String expression, String code, String group, String type) {
        this.code = code;
        this.group = group;
        this.type = type;
        this.expression = expression;
    }
    
    public DefaultSimplifiedRule(String expression) {
        this.code = null;
        this.group = null;
        this.type = null;
        this.expression = expression;
    }
    
    @Setter private String code;
    
    @Setter private String group;
    
    @Setter private String type;
    
    @Setter private String expression;
    
    public String getCode() {
        return code;
    }
    
    public String getGroup() {
        return group;
    }
    
    public String getType() {
        return type;
    }
    
    public String getExpression() {
        return expression;
    }
    
}