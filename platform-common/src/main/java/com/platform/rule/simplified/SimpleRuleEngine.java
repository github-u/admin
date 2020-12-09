package com.platform.rule.simplified;

import com.platform.entity.ResultSupport;
import com.platform.rule.RuleEngine;

public interface SimpleRuleEngine extends RuleEngine<ResultSupport<SimplifiedDecision>, SimplifiedCondition>{
    
    ResultSupport<SimplifiedDecision> execute(SimplifiedCondition context, String expression);
    
    ResultSupport<SimplifiedDecision> execute(SimplifiedCondition context, String expressionName, String expression);
    
    public static final class RC{
        
        public static final String RULE_EXECUTE_EXCEPTION = "RULE_EXECUTE_EXCEPTION";
        
    }
    
}
