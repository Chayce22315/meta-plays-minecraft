package com.metaplaysminecraft.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class AiActionParser {
    private AiActionParser() {
    }

    public static AiAction parse(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            return new AiAction(
                    value(root, "type", "noop").toLowerCase(),
                    value(root, "target", ""),
                    number(root, "yaw", 0.0f),
                    number(root, "pitch", 0.0f),
                    clamp(number(root, "forward", 0.0f), -1.0f, 1.0f),
                    clamp(number(root, "sideways", 0.0f), -1.0f, 1.0f),
                    bool(root, "jump", false),
                    value(root, "message", "")
            );
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

    private static boolean bool(JsonObject root, String key, boolean fallback) {
        return root.has(key) && !root.get(key).isJsonNull() ? root.get(key).getAsBoolean() : fallback;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
