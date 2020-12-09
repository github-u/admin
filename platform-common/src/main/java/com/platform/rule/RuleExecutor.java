package com.platform.rule;

public interface RuleExecutor<Result, Decision> {
    Result execute(Decision decision);
}
