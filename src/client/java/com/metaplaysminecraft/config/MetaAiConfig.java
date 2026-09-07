package com.metaplaysminecraft.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MetaAiConfig {
    public enum Provider {
        OLLAMA,
        OPENAI_COMPATIBLE,
        LOCAL
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "meta_plays_minecraft.json";
    private static MetaAiConfig instance;

    public Provider provider = Provider.OLLAMA;
    public String endpoint = "http://127.0.0.1:11434/v1/chat/completions";
    public String model = "llama3.1:8b";
    public String apiKey = "";
    public double temperature = 0.35D;
    public int maxTokens = 220;
    public int requestTimeoutSeconds = 15;
    public int decisionIntervalTicks = 8;

    public boolean aiEnabled = true;
    public boolean useRealMetaBot = true;
    public boolean memoryEnabled = true;
    public boolean autonomousCrafting = true;
    public boolean autonomousSurvival = true;
    public boolean hostileMobCombat = true;

    private transient Path file;

    private MetaAiConfig() {
    }

    public static MetaAiConfig get() {
        if (instance == null) {
            instance = new MetaAiConfig();
            instance.file = Path.of("config", FILE_NAME);
            instance.load();
        }
        return instance;
    }

    public void load() {
        try {
            Files.createDirectories(file.getParent());
            if (Files.isRegularFile(file)) {
                MetaAiConfig loaded = GSON.fromJson(Files.readString(file), MetaAiConfig.class);
                if (loaded != null) {
                    instance = loaded;
                    instance.file = file;
                    instance.clamp();
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // Keep safe defaults when the config is missing or malformed.
        }
    }

    public void save() {
        clamp();
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(this));
        } catch (IOException ignored) {
            // Minecraft should still run if the config cannot be persisted.
        }
    }

    public void reset() {
        provider = Provider.OLLAMA;
        endpoint = "http://127.0.0.1:11434/v1/chat/completions";
        model = "llama3.1:8b";
        apiKey = "";
        temperature = 0.35D;
        maxTokens = 220;
        requestTimeoutSeconds = 15;
        decisionIntervalTicks = 8;
        aiEnabled = true;
        useRealMetaBot = true;
        memoryEnabled = true;
        autonomousCrafting = true;
        autonomousSurvival = true;
        hostileMobCombat = true;
    }

    public void applyProviderDefaults() {
        switch (provider) {
            case OLLAMA -> {
                endpoint = "http://127.0.0.1:11434/v1/chat/completions";
                if (model.isBlank() || model.equals("llama3.1:8b")) model = "llama3.1:8b";
            }
            case OPENAI_COMPATIBLE -> {
                endpoint = "https://api.openai.com/v1/chat/completions";
                if (model.isBlank() || model.equals("llama3.1:8b")) model = "gpt-4o-mini";
            }
            case LOCAL -> {
                endpoint = "http://127.0.0.1:8000/v1/chat/completions";
                if (model.isBlank() || model.equals("llama3.1:8b")) model = "meta-plays-minecraft";
            }
        }
    }

    private void clamp() {
        if (provider == null) provider = Provider.OLLAMA;
        if (endpoint == null || endpoint.isBlank()) endpoint = "http://127.0.0.1:11434/v1/chat/completions";
        if (model == null || model.isBlank()) model = "llama3.1:8b";
        if (apiKey == null) apiKey = "";
        temperature = Math.max(0.0D, Math.min(2.0D, temperature));
        maxTokens = Math.max(32, Math.min(2048, maxTokens));
        requestTimeoutSeconds = Math.max(3, Math.min(120, requestTimeoutSeconds));
        decisionIntervalTicks = Math.max(2, Math.min(80, decisionIntervalTicks));
    }
}
