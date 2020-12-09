package com.platform.service;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.aspectj.apache.bcel.generic.RET;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.platform.entity.ResultSupport;
import com.platform.utils.DynamicRuntimeUtil;
import com.platform.utils.Pair;
import com.platform.utils.StackTraceUtil;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import lombok.Getter;
import lombok.Setter;

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
    
    public interface RuleEngine<Result, Context>{
        Result execute(Context context, String expression);
    }
    
    public interface RuleExecutor<Result, Decision> {
        Result execute(Decision decision);
    }
    
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
    
    public interface SimplifiedCondition{
        
        String getConditionCode();
        
    }
    
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
    
    public interface SimplifiedRuleExecutor<T> extends RuleExecutor<ResultSupport<T>, SimplifiedDecision>{
        
        String getCode();
        
        String getName();
        
        Class<? extends Object> getGenericParamClassType();
        
    }
    
    public interface SimpleRuleEngine extends RuleEngine<ResultSupport<SimplifiedDecision>, SimplifiedCondition>{
        
        ResultSupport<SimplifiedDecision> execute(SimplifiedCondition context, String expression);
        
        ResultSupport<SimplifiedDecision> execute(SimplifiedCondition context, String expressionName, String expression);
        
        public static final class RC{
            
            public static final String RULE_EXECUTE_EXCEPTION = "RULE_EXECUTE_EXCEPTION";
            
        }
    }
    
    public interface SimplifiedRuleService<T> extends RuleService<ResultSupport<T>, SimplifiedDecision, SimplifiedCondition, SimplifiedRule>{
        
        boolean registerRule(SimplifiedRule simplifiedRule);
        
        boolean registerRuleExecutor(SimplifiedRuleExecutor<T> simplifiedRuleExecutor);
        
        Class<? extends Object> getGenericParamClassType();
        
        public static final class RC{
            
            public static final String NEED_AT_LEAST_ONE_RULE = "NEED_AT_LEAST_ONE_RULE";
            
            public static final String NEED_AT_LEAST_ONE_RULE_EXECUTOR = "NEED_AT_LEAST_ONE_RULE_EXECUTOR";
        }
        
        public static final class ParamStrategy{
            
            @Getter @Setter String name;
            
            @Getter @Setter int priority;
            
            @Getter @Setter Strategy strategy;
            
            @Getter @Setter List<Valid> valid;
            
            public enum Strategy{
                CONSTANT, //常量
                PASSING,  //透传
                LOGIC     //逻辑计算
            }
            
            public enum Valid{
                NOT_NULL,
            }
            
        }
    }
    
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
            /***
            SimplifiedDecision simplifiedDecision = new DefaultSimplifiedDecision();
            String executorType;
            Map<String, Object> ruleExecutorParams = Maps.newLinkedHashMap();
            
                
                String ruleId = kv.getKey();
                SimplifiedRule rule = kv.getValue();
                
                String expression = rule.getExpression();
                String type = rule.getType();
                
                if(SimplifiedRule.Type.DECIDE_EXECUTOR.toString().equals(type)) {
                    
                }
                else if(SimplifiedRule.Type.PARAM_COPY.toString().equals(type)) {
                    
                }
                else if(SimplifiedRule.Type.PARAM_LOGIC.toString().equals(type)) {
                    
                }
                else if(SimplifiedRule.Type.PARAM_CONSTANT.toString().equals(type)) {
                    
                }
                else {
                    //ignore
                }
            
                //processRule(String ruleName, SimplifiedRule simplifiedRule);
            
            */
            
            
        }
            /**
            return new ResultSupport<RuleDecision>().success(new RuleDecision() {
                @Override
                public String getDecisionCode() {
                    return "DEFAULT_DECISION";
                }
                
                @Override
                public String getDecisionName() {
                    return "DEFAULT_DECISION_NAME";
                }
                
                @Override
                public String getRuleExecutorName() {
                    // TODO Auto-generated method stub
                    return null;
                }
                @Override
                public Map<String, Object> getRuleExecutorParams() {
                    // TODO Auto-generated method stub
                    return null;
                }
            });
        }
        */
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
            
            return ret.success(Maps.newHashMap());
            
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
        
    }
    
    public static final class DefaultSimplifiedRule implements SimplifiedRule{

        public DefaultSimplifiedRule(String expression, String code, String group, String type) {
            this.code = code;
            this.group = group;
            this.type = type;
            this.expression = expression;
        }
        
        public DefaultSimplifiedRule(String expression) {
            this.code = null;
            this.group = null;
            this.type = null;
            this.expression = expression;
        }
        
        @Setter private String code;
        
        @Setter private String group;
        
        @Setter private String type;
        
        @Setter private String expression;
        
        public String getCode() {
            return code;
        }
        
        public String getGroup() {
            return group;
        }
        
        public String getType() {
            return type;
        }
        
        public String getExpression() {
            return expression;
        }
        
    }
    
    public static final class DefaultSimplifiedDecision implements SimplifiedDecision{
        
        @Getter @Setter private String decisionCode;
        
        @Getter @Setter private String ruleExecutorName;
        
        @Getter @Setter private String executorType;
        
        @Getter @Setter private Map<String, Object> ruleExecutorParams;
        
        public static DefaultSimplifiedDecision localSimplifiedDecision(Map<String, Object> ruleExecutorParams) {
            
            DefaultSimplifiedDecision defaultSimplifiedDecision = new DefaultSimplifiedDecision();
            
            defaultSimplifiedDecision.decisionCode = SimplifiedDecision.Type.LOCAL.toString();
            defaultSimplifiedDecision.ruleExecutorName = SimplifiedDecision.Type.LOCAL.toString();
            defaultSimplifiedDecision.executorType = SimplifiedDecision.Type.LOCAL.toString();
            defaultSimplifiedDecision.setRuleExecutorParams(ruleExecutorParams);
            
            return defaultSimplifiedDecision;
        }
        
    }
    
    public static final class DefaultRuleEngine implements SimpleRuleEngine{
        
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
    
    public static final class RuleServiceRC{
        
        public static String EXECUTE_EXCEPTION = "EXECUTE_EXCEPTION";
        
    }
    
    public static void main(String[] args) throws CannotCompileException, NotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException, IOException {
        /**
        String sExpression = "a.a == b.c";
        Expression expression = AviatorEvaluator.compile(sExpression);
        
        Map<String, Object> env = Maps.newHashMap();
        env.put("a", new AContext());
        env.put("b", new BContext());
        
        System.out.println(expression.execute(env));
        */
        String packagePathPrefix = "com.su.package.class.make.";
        int counter = 0;
        ClassPool classPool = ClassPool.getDefault();
        CtClass ctClass = classPool.makeClass(packagePathPrefix + "script" + counter++);
        
        classPool.importPackage("java.util");
        
        CtMethod ctMethod = CtNewMethod.make(
                classPool.get("java.util.Map"), 
                "invoke", 
                new CtClass[] {}, 
                new CtClass[] {}, 
                "{System.out.println(\"abc\");return null;}",
                ctClass);
        
        ctClass.addMethod(ctMethod);
        
        ctClass.writeFile("/Users/suxiong.sx/increasement.x/working_code/gitee/platform/");
        
        System.out.println("write");
        Object ctClassObj = ctClass.toClass().newInstance();
        Method $invoke = ctClassObj.getClass().getMethod("invoke", new Class[] {});
        
        $invoke.invoke(ctClassObj, null);
        System.out.println("ok");
        /**
         * 名称：规则名称
         * 规则：表达式（需要有一个后台做组装, 表达式参数 --> <executor，参数>）
         * 结果：结果集合
         * */
    }
    
    public static class AContext {
        
        public AContext() {
            d.put("g", "g");
        } 
        
        @Getter @Setter private String a = "a";
        @Getter @Setter private String b = "b" ;
        
        @Getter @Setter private Map<String, Object> d = Maps.newHashMap();
        
        @Getter @Setter private CContext c = new CContext();
        public static final class CContext{
             @Setter private String f = "f";
            
            public String getF() {
                return "fFun";
            }
        }
    }
    
    public static class BContext {
        @Getter @Setter private String c = "a" ;
        @Getter @Setter private String d = "d" ;
        
    }
    /**
    public static final class SelfDefineFunction implements AviatorFunction{

        public AviatorObject call(Map<String, Object> env, AviatorObject... args) {
            return null;
        } 
        

        @Override
        public void run() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public String getName() {
            // TODO Auto-generated method stub
            return null;
        }

       
    }
    */
    
    
}
