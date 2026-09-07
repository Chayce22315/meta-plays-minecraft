package com.metaplaysminecraft.ai;

public record AiAction(String type, String target, float yaw, float pitch, float forward, float sideways, boolean jump, String message) {
    public static AiAction noop() {
        return new AiAction("noop", "", 0.0f, 0.0f, 0.0f, 0.0f, false, "");
    }

    public boolean isKnown() {
        return switch (type) {
            case "noop", "look", "move", "jump", "chat", "attack" -> true;
            default -> false;
        };
    }
}
