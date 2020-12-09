package com.platform.rule;

import java.util.Map;

import com.platform.entity.ResultSupport;
import com.platform.utils.Pair;
import com.platform.utils.StackTraceUtil;

public interface RuleService<Result, Decision, Condition, Rule> {
    
    default ResultSupport<Pair<Result, Decision>> execute(Condition condition) {
        ResultSupport<Pair<Result, Decision>> ret = new ResultSupport<Pair<Result, Decision>>();
        
        try {
            ResultSupport<Decision> decisionRet = getDecision(condition);
            if(!decisionRet.isSuccess()) {
                return ret.fail(decisionRet.getErrCode(), decisionRet.getErrMsg());
            }
            
            ResultSupport<RuleExecutor<Result, Decision>> ruleExecutorRet = getRuleExecutor(decisionRet.getModel());
            if(!ruleExecutorRet.isSuccess()) {
                return ret.fail(ruleExecutorRet.getErrCode(), ruleExecutorRet.getErrMsg());
            }
            
            Result result = ruleExecutorRet.getModel().execute(decisionRet.getModel());
            
            return ret.success(Pair.of(result, decisionRet.getModel()));
            
        }catch(Exception e) {
            return ret.fail(RuleServiceRC.EXECUTE_EXCEPTION, StackTraceUtil.stackTrace(e));
        }
    }
    
    ResultSupport<Map<String, Rule>> getRules();
    
    ResultSupport<Decision> getDecision(Condition condition);
    
    ResultSupport<RuleExecutor<Result, Decision>> getRuleExecutor(Decision decision);
    
    public static final class RuleServiceRC{
        public static String EXECUTE_EXCEPTION = "EXECUTE_EXCEPTION";
    }
    
}
