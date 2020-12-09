package com.platform.rule.simplified;

import java.util.List;

import com.platform.entity.ResultSupport;
import com.platform.rule.RuleService;

import lombok.Getter;
import lombok.Setter;

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