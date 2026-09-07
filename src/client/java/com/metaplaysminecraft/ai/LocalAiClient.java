package com.metaplaysminecraft.ai;

import com.metaplaysminecraft.MetaPlaysMinecraft;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public final class LocalAiClient {
    private static final String DEFAULT_OLLAMA_HOST = "http://127.0.0.1:11434";
    private static final String DEFAULT_MODEL = "llama3.1:8b";

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final URI endpoint;
    private final String model;

    public LocalAiClient() {
        this(
                URI.create(normalizeHost(System.getenv().getOrDefault("META_MINECRAFT_OLLAMA_HOST", DEFAULT_OLLAMA_HOST)) + "/v1/chat/completions"),
                System.getenv().getOrDefault("META_MINECRAFT_OLLAMA_MODEL", DEFAULT_MODEL)
        );
    }

    public LocalAiClient(URI endpoint) {
        this(endpoint, System.getenv().getOrDefault("META_MINECRAFT_OLLAMA_MODEL", DEFAULT_MODEL));
    }

    public LocalAiClient(URI endpoint, String model) {
        this.endpoint = endpoint;
        this.model = model == null || model.isBlank() ? DEFAULT_MODEL : model.trim();
    }

    public CompletableFuture<AiAction> decide(String perception) {
        String body = "{\"model\":\"" + escape(model) + "\",\"messages\":[" +
                "{\"role\":\"system\",\"content\":\"You are the brain of a cooperative Minecraft player. Behave like a cautious human teammate. Return ONLY one JSON object and never markdown. Choose exactly one action type. Valid types: noop, look, move, jump, chat, attack, mine, place, use, interact, sleep, craft, select, stop. move may set target to coords:x,y,z. mine/place/use/interact use integer x,y,z. select uses slot 0-8. craft uses target recipe name. Keep movement natural, avoid impossible jumps, and prefer existing tools/items. You may fight hostile non-player mobs to protect yourself or your teammate. NEVER attack, target, or intentionally damage players. Player entities are teammates or neutral social actors.\"}," +
                "{\"role\":\"user\",\"content\":\"Current Minecraft state: " + escape(perception) + "\"}" +
                "],\"temperature\":0.35,\"max_tokens\":220,\"stream\":false}";

        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() / 100 != 2) {
                        throw new IllegalStateException("Ollama returned HTTP " + response.statusCode());
                    }
                    return extractAction(response.body());
                })
                .exceptionally(error -> {
                    MetaPlaysMinecraft.LOGGER.debug("Ollama unavailable (model={} endpoint={}): {}", model, endpoint, error.getMessage());
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

    private static String normalizeHost(String host) {
        String value = host == null || host.isBlank() ? DEFAULT_OLLAMA_HOST : host.trim();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }
}
