package com.platform.rule.simplified;

import java.util.Map;

public interface SimplifiedDecision{
        
    String getDecisionCode();
    
    String getRuleExecutorName();
    
    String getExecutorType();
    
    Map<String, Object> getRuleExecutorParams();
    
    public enum Type{
        LOCAL,
        HSF,
        TAIR,
        DIAMOND,
    }
    
}