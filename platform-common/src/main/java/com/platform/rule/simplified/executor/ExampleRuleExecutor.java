package com.platform.rule.simplified.executor;

import java.util.Map;

import com.platform.entity.ResultSupport;
import com.platform.rule.simplified.SimplifiedDecision;
import com.platform.rule.simplified.SimplifiedRuleExecutor;

public class ExampleRuleExecutor implements SimplifiedRuleExecutor<Map<String, Object>>{
    
    @Override
    public String getCode() {
        return "code^group^1";
    }
    
    @Override
    public String getName() {
        return "example";
    }
    
    @Override
    public ResultSupport<Map<String, Object>> execute(SimplifiedDecision decision) {
        ResultSupport<Map<String, Object>> ret = new ResultSupport<Map<String, Object>>();
        
        Map<String, Object> params = decision.getRuleExecutorParams();
        
        return ret.success(params);
    }
    
    @Override
    public Class<? extends Object> getGenericParamClassType() {
        return Map.class;
    }
    
}
