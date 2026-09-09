package com.metaplaysminecraft.ai;

public record AiDecision(AiAction action, AiGoal goal) {
    public static AiDecision noop(AiGoal goal) {
        return new AiDecision(AiAction.noop(), goal == null ? AiGoal.idle() : goal.normalized());
    }
}
