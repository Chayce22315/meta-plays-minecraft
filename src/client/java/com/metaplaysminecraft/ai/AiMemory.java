package com.metaplaysminecraft.ai;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Small bounded memory that keeps the model aware of what it recently tried.
 * It intentionally stays in-memory so the mod never writes player/chat data
 * to disk unless a future feature explicitly opts into persistence.
 */
public final class AiMemory {
    private static final int MAX_EVENTS = 16;
    private static final int MAX_EVENT_LENGTH = 180;

    private final Deque<String> events = new ArrayDeque<>();

    public void remember(String event) {
        if (event == null || event.isBlank()) {
            return;
        }

        String compact = event.replace('\n', ' ').trim();
        if (compact.length() > MAX_EVENT_LENGTH) {
            compact = compact.substring(0, MAX_EVENT_LENGTH - 3) + "...";
        }

        events.addLast(compact);
        while (events.size() > MAX_EVENTS) {
            events.removeFirst();
        }
    }

    public void rememberAction(AiAction action) {
        if (action == null || !action.isKnown() || "noop".equals(action.type())) {
            return;
        }
        String detail = action.target() == null || action.target().isBlank()
                ? ""
                : " -> " + action.target();
        remember("tried " + action.type() + detail);
    }

    public String context() {
        if (events.isEmpty()) {
            return "Recent memory: none.";
        }

        StringBuilder out = new StringBuilder("Recent memory:\n");
        for (String event : events) {
            out.append("- ").append(event).append('\n');
        }
        return out.toString().trim();
    }
}
