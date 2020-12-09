package com.platform.rule.simplified.entity;

import com.platform.rule.simplified.SimplifiedCondition;

public class DefaultSimplifiedCondition implements SimplifiedCondition{

    private String conditionCode;
    
    @Override
    public String getConditionCode() {
        return conditionCode;
    }
    
    public DefaultSimplifiedCondition(String conditionCode) {
        this.conditionCode = conditionCode;
    } 
}
