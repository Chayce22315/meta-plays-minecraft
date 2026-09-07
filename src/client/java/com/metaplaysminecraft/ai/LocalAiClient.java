package com.metaplaysminecraft.ai;

import com.metaplaysminecraft.MetaPlaysMinecraft;
import com.metaplaysminecraft.config.MetaAiConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public final class LocalAiClient {
    private static final String SYSTEM_PROMPT = "You are the brain of a cooperative Minecraft player. Behave like a cautious human teammate. Return ONLY one JSON object and never markdown. Choose exactly one action type. Valid types: noop, look, move, jump, chat, attack, mine, place, use, interact, sleep, craft, select, stop. move may set target to coords:x,y,z. mine/place/use/interact use integer x,y,z. select uses slot 0-8. craft uses target recipe name. Keep movement natural, avoid impossible jumps, and prefer existing tools/items. You may fight hostile non-player mobs to protect yourself or your teammate. NEVER attack, target, or intentionally damage players. Player entities are teammates or neutral social actors.";

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    public CompletableFuture<AiAction> decide(String perception) {
        MetaAiConfig config = MetaAiConfig.get();
        URI endpoint = parseEndpoint(config.endpoint);
        String body = "{\"model\":\"" + escape(config.model) + "\",\"messages\":[" +
                "{\"role\":\"system\",\"content\":\"" + escape(SYSTEM_PROMPT) + "\"}," +
                "{\"role\":\"user\",\"content\":\"Current Minecraft state: " + escape(perception) + "\"}" +
                "],\"temperature\":" + config.temperature + ",\"max_tokens\":" + config.maxTokens + ",\"stream\":false}";

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(config.requestTimeoutSeconds))
                .header("Content-Type", "application/json");
        if (!config.apiKey.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + config.apiKey);
        }

        return http.sendAsync(requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() / 100 != 2) {
                        throw new IllegalStateException("AI server returned HTTP " + response.statusCode());
                    }
                    return extractAction(response.body());
                })
                .exceptionally(error -> {
                    MetaPlaysMinecraft.LOGGER.debug("AI server unavailable (provider={} model={} endpoint={}): {}",
                            config.provider, config.model, endpoint, error.getMessage());
                    return AiAction.noop();
                });
    }

    public static CompletableFuture<Boolean> testConnection() {
        MetaAiConfig config = MetaAiConfig.get();
        try {
            URI endpoint = parseEndpoint(config.endpoint);
            String body = "{\"model\":\"" + escape(config.model) + "\",\"messages\":[{\"role\":\"user\",\"content\":\"ping\"}],\"temperature\":0,\"max_tokens\":4,\"stream\":false}";
            HttpRequest.Builder builder = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(Math.max(3, Math.min(20, config.requestTimeoutSeconds))))
                    .header("Content-Type", "application/json");
            if (!config.apiKey.isBlank()) builder.header("Authorization", "Bearer " + config.apiKey);
            return CompletableFuture.supplyAsync(() -> {
                try {
                    HttpResponse<String> response = httpClient().send(
                            builder.POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                            HttpResponse.BodyHandlers.ofString());
                    return response.statusCode() / 100 == 2;
                } catch (Exception ignored) {
                    return false;
                }
            });
        } catch (RuntimeException error) {
            return CompletableFuture.completedFuture(false);
        }
    }

    private static HttpClient httpClient() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }

    private static AiAction extractAction(String response) {
        try {
            var root = com.google.gson.JsonParser.parseString(response).getAsJsonObject();
            String content = root.getAsJsonArray("choices").get(0).getAsJsonObject()
                    .getAsJsonObject("message").get("content").getAsString();
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}');
            if (start >= 0 && end > start) return AiActionParser.parse(content.substring(start, end + 1));
        } catch (RuntimeException ignored) {
        }
        return AiAction.noop();
    }

    private static URI parseEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) throw new IllegalArgumentException("AI endpoint is empty");
        return URI.create(endpoint.trim());
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }
}
