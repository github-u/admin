package com.platform.rule;

public interface RuleEngine<Result, Context>{
    Result execute(Context context, String expression);
}