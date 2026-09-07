package com.metaplaysminecraft.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class AiActionParser {
    private AiActionParser() {
    }

    public static AiAction parse(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String type = value(root, "type", "noop").toLowerCase();
            String target = value(root, "target", "");
            float yaw = number(root, "yaw", 0.0f);
            float pitch = number(root, "pitch", 0.0f);
            float forward = clamp(number(root, "forward", 0.0f), -1.0f, 1.0f);
            float sideways = clamp(number(root, "sideways", 0.0f), -1.0f, 1.0f);
            boolean jump = root.has("jump") && root.get("jump").getAsBoolean();
            String message = value(root, "message", "");
            return new AiAction(type, target, yaw, pitch, forward, sideways, jump, message);
        } catch (RuntimeException ignored) {
            return AiAction.noop();
        }
    }

    private static String value(JsonObject root, String key, String fallback) {
        return root.has(key) && !root.get(key).isJsonNull() ? root.get(key).getAsString() : fallback;
    }

    private static float number(JsonObject root, String key, float fallback) {
        return root.has(key) && !root.get(key).isJsonNull() ? root.get(key).getAsFloat() : fallback;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
