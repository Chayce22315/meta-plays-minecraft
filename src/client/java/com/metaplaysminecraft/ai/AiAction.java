package com.metaplaysminecraft.ai;

public record AiAction(
        String type,
        String target,
        float yaw,
        float pitch,
        float forward,
        float sideways,
        boolean jump,
        boolean sprint,
        boolean crouch,
        int slot,
        int x,
        int y,
        int z,
        int durationTicks,
        String message
) {
    public static AiAction noop() {
        return new AiAction("noop", "", 0, 0, 0, 0, false, false, false, -1, 0, 0, 0, 1, "");
    }

    public boolean isKnown() {
        return switch (type) {
            case "noop", "look", "move", "jump", "chat", "attack", "mine", "place", "use", "interact", "sleep", "craft", "select", "stop" -> true;
            default -> false;
        };
    }

    public int safeDuration() {
        return Math.max(1, Math.min(durationTicks, 200));
    }
}
