package com.platform.rule.simplified.impl;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.platform.entity.ResultSupport;
import com.platform.rule.simplified.SimpleRuleEngine;
import com.platform.rule.simplified.SimplifiedCondition;
import com.platform.rule.simplified.SimplifiedDecision;
import com.platform.utils.DynamicRuntimeUtil;

public class DefaultRuleEngine implements SimpleRuleEngine{
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultRuleEngine.class);
    
    @Override
    public ResultSupport<SimplifiedDecision> execute(SimplifiedCondition simplifiedCondition, String expression) {
        return execute(simplifiedCondition, expression, expression);
    }
    
    @Override
    public ResultSupport<SimplifiedDecision> execute(SimplifiedCondition context, String expressionName, String expression) {
        ResultSupport<SimplifiedDecision> ret = new ResultSupport<SimplifiedDecision>();
        
        try {
            Map<String, Object> dynamicRuntimeContext = Maps.newHashMap();
            dynamicRuntimeContext.put("condition", context);
            
            SimplifiedDecision simplifiedDecision = (SimplifiedDecision) DynamicRuntimeUtil.evaluate(
                    expressionName, 
                    expression, 
                    dynamicRuntimeContext
                    );
            
            return ret.success(simplifiedDecision);
        }catch(Exception e) {
            logger.error("title=" +  "DefaultRuleEngine"
                    + "$mode=" + "execute"
                    + "$errCode=" + SimpleRuleEngine.RC.RULE_EXECUTE_EXCEPTION
                    + "$errMsg=" + e.getMessage(), e);
            return ret.fail(SimpleRuleEngine.RC.RULE_EXECUTE_EXCEPTION, e.getMessage());
        }
        
    }
    
}
