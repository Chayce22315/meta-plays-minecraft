package com.metaplaysminecraft.ai;

import com.metaplaysminecraft.MetaPlaysMinecraft;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public final class LocalAiClient {
    private static final URI DEFAULT_ENDPOINT = URI.create("http://127.0.0.1:8000/v1/chat/completions");
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final URI endpoint;

    public LocalAiClient() {
        this(DEFAULT_ENDPOINT);
    }

    public LocalAiClient(URI endpoint) {
        this.endpoint = endpoint;
    }

    public CompletableFuture<AiAction> decide(String perception) {
        String body = "{" +
                "\"model\":\"meta-plays-minecraft\",\"messages\":[" +
                "{\"role\":\"system\",\"content\":\"You control a cooperative Minecraft player. Return ONLY one JSON action. Never target, attack, or harm players. Fight hostile non-player mobs only. Valid types: noop, look, move, jump, chat, attack.\"}," +
                "{\"role\":\"user\",\"content\":\"Minecraft state: " + escape(perception) + "\"}" +
                "],\"temperature\":0.4,\"max_tokens\":180}";

        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(12))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() / 100 != 2) {
                        throw new IllegalStateException("AI bridge returned HTTP " + response.statusCode());
                    }
                    return extractAction(response.body());
                })
                .exceptionally(error -> {
                    MetaPlaysMinecraft.LOGGER.debug("AI bridge unavailable: {}", error.getMessage());
                    return AiAction.noop();
                });
    }

    private static AiAction extractAction(String response) {
        try {
            var root = com.google.gson.JsonParser.parseString(response).getAsJsonObject();
            String content = root.getAsJsonArray("choices").get(0).getAsJsonObject()
                    .getAsJsonObject("message").get("content").getAsString();
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return AiActionParser.parse(content.substring(start, end + 1));
            }
        } catch (RuntimeException ignored) {
        }
        return AiAction.noop();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }
}
