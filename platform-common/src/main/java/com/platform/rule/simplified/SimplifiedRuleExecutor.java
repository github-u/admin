package com.platform.rule.simplified;

import com.platform.entity.ResultSupport;
import com.platform.rule.RuleExecutor;

public interface SimplifiedRuleExecutor<T> extends RuleExecutor<ResultSupport<T>, SimplifiedDecision>{
    
    String getCode();
    
    String getName();
    
    Class<? extends Object> getGenericParamClassType();
    
}
