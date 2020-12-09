package com.platform.rule.simplified.entity;

import java.util.Map;

import com.platform.rule.simplified.SimplifiedDecision;

public class DefaultSimplifiedDecision implements SimplifiedDecision {

    private String decisionCode;
    
    private String ruleExecutorName;
    
    private String executorType;
    
    private Map<String, Object> ruleExecutorParams;
    
    public DefaultSimplifiedDecision(String decisionCode, String ruleExecutorName, String executorType, Map<String, Object> ruleExecutorParams) {
        this.decisionCode = decisionCode;
        this.ruleExecutorName = ruleExecutorName;
        this.executorType = executorType;
        this.ruleExecutorParams = ruleExecutorParams;
    }
    
    @Override
    public String getDecisionCode() {
        return decisionCode;
    }

    @Override
    public String getRuleExecutorName() {
        return ruleExecutorName;
    }

    @Override
    public String getExecutorType() {
        return executorType;
    }

    @Override
    public Map<String, Object> getRuleExecutorParams() {
        return ruleExecutorParams;
    }

}
