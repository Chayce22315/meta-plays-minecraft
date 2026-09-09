package com.metaplaysminecraft.ai;

public record AiGoal(String type, String description, boolean done) {
    public static AiGoal idle() {
        return new AiGoal("survive", "survive and help the teammate", false);
    }

    public AiGoal normalized() {
        String safeType = type == null || type.isBlank() ? "survive" : type.trim().toLowerCase();
        String safeDescription = description == null || description.isBlank() ? safeType : description.trim();
        return new AiGoal(safeType, safeDescription, done);
    }
}
