package com.platform.rule.simplified;

public interface SimplifiedRule{
    
    String getCode();
    
    String getGroup();
    
    String getType();
    
    String getExpression();
    
    public enum Type{
        DECIDE_EXECUTOR,
        PARAM_COPY,
        PARAM_LOGIC,
        PARAM_CONSTANT
    }
    
}