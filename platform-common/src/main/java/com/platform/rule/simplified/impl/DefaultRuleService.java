package com.platform.rule.simplified.impl;

import java.util.Map;
import java.util.Optional;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.platform.entity.ResultSupport;
import com.platform.rule.RuleExecutor;
import com.platform.rule.simplified.SimpleRuleEngine;
import com.platform.rule.simplified.SimplifiedCondition;
import com.platform.rule.simplified.SimplifiedDecision;
import com.platform.rule.simplified.SimplifiedRule;
import com.platform.rule.simplified.SimplifiedRuleExecutor;
import com.platform.rule.simplified.SimplifiedRuleService;
import com.platform.rule.simplified.entity.DefaultSimplifiedCondition;
import com.platform.rule.simplified.entity.DefaultSimplifiedDecision;
import com.platform.rule.simplified.entity.DefaultSimplifiedRule;
import com.platform.rule.simplified.executor.ExampleRuleExecutor;
import com.platform.utils.Pair;

public class DefaultRuleService implements SimplifiedRuleService<Map<String, Object>>{
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultRuleEngine.class);

    private Map<String, SimplifiedRule> rules = Maps.newHashMap();

    private Map<String, SimplifiedRuleExecutor<Map<String, Object>>> ruleExecutors = Maps.newHashMap();

    private static final String RULE_CODE_GROUP_JOINER = "^";

    private static final String EXECUTOR_TYPE_NAME_JOINER = "^";

    private SimpleRuleEngine simpleRuleEngine;

    public DefaultRuleService(SimpleRuleEngine simpleRuleEngine) {
        this.simpleRuleEngine = simpleRuleEngine;
    }

    @Override
    public ResultSupport<SimplifiedDecision> getDecision(SimplifiedCondition condition) {
        
        Preconditions.checkNotNull(condition);
        
        ResultSupport<SimplifiedDecision> ret = new ResultSupport<SimplifiedDecision>();
        ResultSupport<Map<String, SimplifiedRule>> rulesRet = getRules();
        if(!rulesRet.isSuccess()) {
            return ret.fail(rulesRet.getErrCode(), rulesRet.getErrMsg());
        }
        
        ResultSupport<SimplifiedDecision> resolveRulesRet = resolveRules(rules, condition);
        if(!resolveRulesRet.isSuccess()) {
            return ret.fail(resolveRulesRet.getErrCode(), resolveRulesRet.getErrMsg());
        }
        
        return ret.success(resolveRulesRet.getModel());
        
    }
    
    @Override
    public ResultSupport<RuleExecutor<ResultSupport<Map<String, Object>>, SimplifiedDecision>> getRuleExecutor(SimplifiedDecision decision) {
        
        Preconditions.checkArgument(decision != null && decision.getExecutorType() != null && decision.getRuleExecutorName() != null);
        String executorName = decision.getExecutorType() + EXECUTOR_TYPE_NAME_JOINER + decision.getRuleExecutorName();
        
        SimplifiedRuleExecutor<Map<String, Object>> ruleExecutor = ruleExecutors.get(executorName);
        ResultSupport<RuleExecutor<ResultSupport<Map<String, Object>>, SimplifiedDecision>> ret = new ResultSupport<RuleExecutor<ResultSupport<Map<String, Object>>, SimplifiedDecision>>();
        if(ruleExecutor != null) {
            return ret.success(ruleExecutor);
        }else {
            logger.error("title=DefaultRuleService"
                    + "$mode=getRuleExecutor"
                    + "$errCode=" + RC.NEED_AT_LEAST_ONE_RULE_EXECUTOR
                    + "$errMsg=" + executorName);
            return ret.fail(RC.NEED_AT_LEAST_ONE_RULE_EXECUTOR, executorName);
        }
    }
    
    @Override
    public boolean registerRuleExecutor(SimplifiedRuleExecutor<Map<String, Object>> simplifiedRuleExecutor) {
        
        Preconditions.checkNotNull(simplifiedRuleExecutor);
        Preconditions.checkNotNull(simplifiedRuleExecutor.getCode());
        
        String key = simplifiedRuleExecutor.getCode();
        if(ruleExecutors.get(key) != null) {
            return Boolean.FALSE;
        }else {
            ruleExecutors.put(key, simplifiedRuleExecutor);
            return Boolean.TRUE;
        }
        
    }
    
    @Override
    public Class<? extends Object> getGenericParamClassType() {
        return Map.class;
    }
    
    @Override
    public ResultSupport<Map<String, SimplifiedRule>> getRules() {
        
        ResultSupport<Map<String, SimplifiedRule>> ret = new ResultSupport<Map<String, SimplifiedRule>>();
        
        return ret.success(rules);
        
    }
    
    private ResultSupport<SimplifiedDecision> resolveRules(Map<String, SimplifiedRule> rules, SimplifiedCondition condition){
        
        ResultSupport<SimplifiedDecision> ret = new ResultSupport<SimplifiedDecision>();

        Optional<ResultSupport<SimplifiedDecision>> optRet = 
                rules.entrySet().stream()
                .map(kv -> {

                    String ruleId = kv.getKey();
                    SimplifiedRule simplifiedRule = kv.getValue();

                    return simpleRuleEngine.execute(
                            condition, 
                            ruleId, 
                            simplifiedRule.getExpression()
                            );

                })
                .filter(decisionResult -> {
                    return decisionResult.isSuccess();
                })
                .findFirst();

        if(optRet.isPresent()) {
            return optRet.get();
        }else {
            return ret.fail(RC.NEED_AT_LEAST_ONE_RULE, "");
        }

    }

    @Override
    public boolean registerRule(SimplifiedRule simplifiedRule) {
        Preconditions.checkNotNull(simplifiedRule);
        Preconditions.checkNotNull(simplifiedRule.getCode());
        Preconditions.checkNotNull(simplifiedRule.getGroup());

        String key = simplifiedRule.getCode() + RULE_CODE_GROUP_JOINER + simplifiedRule.getGroup();
        if(rules.get(key) != null) {
            return Boolean.FALSE;
        }else {
            rules.put(key, simplifiedRule);
            return Boolean.TRUE;
        }
    }
    
    @FixMethodOrder(MethodSorters.NAME_ASCENDING)
    public static class TC{
        
        private static SimplifiedRuleService<Map<String, Object>> simplifiedRuleService;
        
        @BeforeClass
        public static void init() {
            if(simplifiedRuleService == null) {
                SimpleRuleEngine simpleRuleEngine = new DefaultRuleEngine();
                simplifiedRuleService = new DefaultRuleService(simpleRuleEngine);
            }
         
        }
        
        //TC1 register rule
        @Test
        public void _1_test_registerRule() {
            SimplifiedRule simplifiedRule = new DefaultSimplifiedRule(rule1Expression(), "code", "group", "");
            Preconditions.checkArgument(simplifiedRuleService.registerRule(simplifiedRule));
        }
        
        //TC1 register rule
        @Test
        public void _2_test_registerRule() {
            SimplifiedRule simplifiedRule = new DefaultSimplifiedRule(rule1Expression(), "code", "group", "");
            Preconditions.checkArgument(!simplifiedRuleService.registerRule(simplifiedRule));
        }
        
        //TC2 register rule executor
        @Test
        public void _3_test_registerRuleExecutor() {
            SimplifiedRuleExecutor<Map<String, Object>> exampleRuleExecutor = new ExampleRuleExecutor();
            Preconditions.checkArgument(simplifiedRuleService.registerRuleExecutor(exampleRuleExecutor));
        }
        
        //TC2 register rule executor
        @Test
        public void _4_test_registerRuleExecutor() {
            SimplifiedRuleExecutor<Map<String, Object>> exampleRuleExecutor = new ExampleRuleExecutor();
            Preconditions.checkArgument(!simplifiedRuleService.registerRuleExecutor(exampleRuleExecutor));
        }
        
        //TC3 execute
        @Test
        public void _5_test_execute() {
            SimplifiedCondition simplifiedCondition = new DefaultSimplifiedCondition("SampleCondition");
            ResultSupport<Pair<ResultSupport<Map<String, Object>>, SimplifiedDecision>> executeRet = simplifiedRuleService.execute(simplifiedCondition);
            Preconditions.checkArgument(executeRet.isSuccess());
            Preconditions.checkArgument(executeRet.getModel() != null);
            
            ResultSupport<Map<String, Object>> executeResult = executeRet.getModel().fst;
            SimplifiedDecision simplifiedDecision = executeRet.getModel().snd;
            
            Preconditions.checkArgument(executeResult != null && executeResult.isSuccess());
            Preconditions.checkArgument(simplifiedDecision != null 
                    && simplifiedDecision.getExecutorType().equals("code^group")
                    && simplifiedDecision.getDecisionCode().equals("SampleDecition"));
        }
        
        private String rule1Expression() {
            return 
                    "if(\"SampleCondition\".equals(((com.platform.rule.simplified.SimplifiedCondition)$1.get(\"condition\")).getConditionCode())) {\n" +
                    "   return new com.platform.rule.simplified.entity.DefaultSimplifiedDecision(\"SampleDecition\", \"1\", \"code^group\", null);\n" +
                    "}else {\n" +
                    "   return null;\n" +
                    "}";
        }
    }
    
    public static final class ExpressionSample{
        public SimplifiedDecision _1_expression() {
            Map<String, Object> m = Maps.newHashMap();
            if("SampleCondition".equals(((SimplifiedCondition)m.get("condition")).getConditionCode())) {
                return new DefaultSimplifiedDecision(null, null, "code^group", null);
            }else {
                return null;
            }
        }
    }
    
    public static void main(String[] args) {
        String str = "Map<String, Object> m = Maps.newHashMap();\n" +
                "            if(\"SampleCondition\".equals(((SimplifiedCondition)m.get(\"condition\")).getConditionCode())) {\n" +
                "                return new DefaultSimplifiedDecision(null, null, \"code^group\", null);\n" +
                "            }else {\n" +
                "                return null;\n" +
                "            }";
    }
}