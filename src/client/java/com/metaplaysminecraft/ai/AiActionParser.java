package com.metaplaysminecraft.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class AiActionParser {
    private AiActionParser() {
    }

    public static AiAction parse(String json) {
        try {
            return parse(JsonParser.parseString(json).getAsJsonObject());
        } catch (RuntimeException ignored) {
            return AiAction.noop();
        }
    }

    public static AiAction parse(JsonObject root) {
        try {
            String type = value(root, "type", "noop").toLowerCase();
            return new AiAction(
                    type,
                    value(root, "target", ""),
                    number(root, "yaw", 0),
                    number(root, "pitch", 0),
                    clamp(number(root, "forward", 0), -1, 1),
                    clamp(number(root, "sideways", 0), -1, 1),
                    bool(root, "jump", false),
                    bool(root, "sprint", false),
                    bool(root, "crouch", false),
                    integer(root, "slot", -1),
                    integer(root, "x", 0),
                    integer(root, "y", 0),
                    integer(root, "z", 0),
                    integer(root, "duration_ticks", 1),
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

    private static int integer(JsonObject root, String key, int fallback) {
        return root.has(key) && !root.get(key).isJsonNull() ? root.get(key).getAsInt() : fallback;
    }

    private static boolean bool(JsonObject root, String key, boolean fallback) {
        return root.has(key) && !root.get(key).isJsonNull() ? root.get(key).getAsBoolean() : fallback;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
