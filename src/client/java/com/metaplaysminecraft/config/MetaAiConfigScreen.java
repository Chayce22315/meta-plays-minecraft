package com.metaplaysminecraft.config;

import com.metaplaysminecraft.ai.LocalAiClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class MetaAiConfigScreen extends Screen {
    private enum Tab {
        PROVIDER("provider"),
        BRAIN("brain"),
        SAFETY("safety")
        ;

        private final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    private final Screen parent;
    private final MetaAiConfig config;
    private Tab tab = Tab.PROVIDER;

    private EditBox endpointBox;
    private EditBox modelBox;
    private EditBox apiKeyBox;
    private EditBox temperatureBox;
    private EditBox maxTokensBox;
    private EditBox timeoutBox;
    private EditBox intervalBox;

    private Button ollamaButton;
    private Button openAiButton;
    private Button localButton;
    private Button aiToggle;
    private Button realBotToggle;
    private Button memoryToggle;
    private Button craftingToggle;
    private Button survivalToggle;
    private Button combatToggle;
    private Button testButton;

    private String status = "ready";
    private long statusUntil;

    public MetaAiConfigScreen(Screen parent) {
        super(Component.literal("meta plays minecraft settings"));
        this.parent = parent;
        this.config = MetaAiConfig.get();
    }

    @Override
    protected void init() {
        rebuildWidgets();
    }

    private void rebuildWidgets() {
        clearWidgets();
        int left = Math.max(24, this.width / 2 - 220);
        int top = 88;
        int width = Math.min(440, this.width - 48);

        if (tab == Tab.PROVIDER) {
            ollamaButton = addRenderableWidget(Button.builder(labelFor(MetaAiConfig.Provider.OLLAMA), b -> selectProvider(MetaAiConfig.Provider.OLLAMA)).bounds(left, top, 138, 22).build());
            openAiButton = addRenderableWidget(Button.builder(labelFor(MetaAiConfig.Provider.OPENAI_COMPATIBLE), b -> selectProvider(MetaAiConfig.Provider.OPENAI_COMPATIBLE)).bounds(left + 150, top, 138, 22).build());
            localButton = addRenderableWidget(Button.builder(labelFor(MetaAiConfig.Provider.LOCAL), b -> selectProvider(MetaAiConfig.Provider.LOCAL)).bounds(left + 300, top, 138, 22).build());

            endpointBox = addBox("chat completions url", config.endpoint, left, top + 58, width);
            modelBox = addBox("model name", config.model, left, top + 116, width);
            apiKeyBox = addBox("api key (optional)", config.apiKey, left, top + 174, width);
            apiKeyBox.setValue(config.apiKey);
            apiKeyBox.setMaxLength(512);
            apiKeyBox.setSuggestion(Component.literal("leave blank for local servers"));

            testButton = addRenderableWidget(Button.builder(Component.literal("test connection"), b -> testConnection()).bounds(left, top + 232, 138, 22).build());
            addRenderableWidget(Button.builder(Component.literal("reset defaults"), b -> {
                config.reset();
                config.save();
                rebuildWidgets();
                status("defaults restored", 2400L);
            }).bounds(left + 150, top + 232, 138, 22).build());
        } else if (tab == Tab.BRAIN) {
            aiToggle = addToggle("ai controller", config.aiEnabled, left, top, width, value -> config.aiEnabled = value);
            realBotToggle = addToggle("real metabot bridge", config.useRealMetaBot, left, top + 34, width, value -> config.useRealMetaBot = value);
            memoryToggle = addToggle("session memory", config.memoryEnabled, left, top + 68, width, value -> config.memoryEnabled = value);
            craftingToggle = addToggle("autonomous crafting", config.autonomousCrafting, left, top + 102, width, value -> config.autonomousCrafting = value);
            survivalToggle = addToggle("autonomous food / survival", config.autonomousSurvival, left, top + 136, width, value -> config.autonomousSurvival = value);

            temperatureBox = addBox("temperature (0.0 - 2.0)", format(config.temperature), left, top + 186, width);
            maxTokensBox = addBox("max output tokens", Integer.toString(config.maxTokens), left, top + 244, width);
            timeoutBox = addBox("request timeout seconds", Integer.toString(config.requestTimeoutSeconds), left, top + 302, width);
            intervalBox = addBox("decision interval ticks", Integer.toString(config.decisionIntervalTicks), left, top + 360, width);
        } else {
            combatToggle = addToggle("hostile mob combat", config.hostileMobCombat, left, top, width, value -> config.hostileMobCombat = value);
            addRenderableWidget(Button.builder(Component.literal("player combat: permanently disabled"), b -> status("player targets are hard-blocked", 2600L)).bounds(left, top + 38, width, 22).build());
            addRenderableWidget(Button.builder(Component.literal("distance limits: enabled"), b -> status("actions keep conservative interaction ranges", 2600L)).bounds(left, top + 72, width, 22).build());
            addRenderableWidget(Button.builder(Component.literal("recovery: small jump / forward only"), b -> status("stuck recovery never teleports", 2600L)).bounds(left, top + 106, width, 22).build());
        }

        addRenderableWidget(Button.builder(Component.literal("provider"), b -> changeTab(Tab.PROVIDER)).bounds(left, 53, 108, 22).build());
        addRenderableWidget(Button.builder(Component.literal("brain"), b -> changeTab(Tab.BRAIN)).bounds(left + 114, 53, 108, 22).build());
        addRenderableWidget(Button.builder(Component.literal("safety"), b -> changeTab(Tab.SAFETY)).bounds(left + 228, 53, 108, 22).build());
        addRenderableWidget(Button.builder(Component.literal("done"), b -> done()).bounds(left + 342, 53, 98, 22).build());
    }

    private String labelFor(MetaAiConfig.Provider provider) {
        String title = switch (provider) {
            case OLLAMA -> "ollama";
            case OPENAI_COMPATIBLE -> "openai compatible";
            case LOCAL -> "local server";
        };
        return config.provider == provider ? "✓ " + title : title;
    }

    private void selectProvider(MetaAiConfig.Provider provider) {
        saveFields();
        config.provider = provider;
        config.applyProviderDefaults();
        config.save();
        rebuildWidgets();
        status(providerLabel(provider) + " selected", 1800L);
    }

    private String providerLabel(MetaAiConfig.Provider provider) {
        return switch (provider) {
            case OLLAMA -> "ollama";
            case OPENAI_COMPATIBLE -> "openai compatible";
            case LOCAL -> "local server";
        };
    }

    private void changeTab(Tab next) {
        saveFields();
        config.save();
        tab = next;
        rebuildWidgets();
    }

    private EditBox addBox(String label, String value, int x, int y, int width) {
        EditBox box = new EditBox(this.font, x, y + 15, width, 20, Component.literal(label));
        box.setValue(value == null ? "" : value);
        box.setMaxLength(2048);
        addRenderableWidget(box);
        return box;
    }

    private Button addToggle(String label, boolean value, int x, int y, int width, java.util.function.Consumer<Boolean> setter) {
        Button button = addRenderableWidget(Button.builder(toggleLabel(label, value), b -> {
            boolean next = !b.getMessage().getString().endsWith("on");
            setter.accept(next);
            b.setMessage(toggleLabel(label, next));
            config.save();
        }).bounds(x, y, width, 26).build());
        return button;
    }

    private String toggleLabel(String label, boolean value) {
        return label + "  •  " + (value ? "on" : "off");
    }

    private void saveFields() {
        if (endpointBox != null) config.endpoint = endpointBox.getValue().trim();
        if (modelBox != null) config.model = modelBox.getValue().trim();
        if (apiKeyBox != null) config.apiKey = apiKeyBox.getValue();
        if (temperatureBox != null) config.temperature = parseDouble(temperatureBox.getValue(), config.temperature);
        if (maxTokensBox != null) config.maxTokens = parseInt(maxTokensBox.getValue(), config.maxTokens);
        if (timeoutBox != null) config.requestTimeoutSeconds = parseInt(timeoutBox.getValue(), config.requestTimeoutSeconds);
        if (intervalBox != null) config.decisionIntervalTicks = parseInt(intervalBox.getValue(), config.decisionIntervalTicks);
    }

    private double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private void done() {
        saveFields();
        config.save();
        Minecraft.getInstance().setScreen(parent);
    }

    private void testConnection() {
        saveFields();
        config.save();
        testButton.active = false;
        status("testing...", 0L);
        CompletableFuture.runAsync(() -> LocalAiClient.testConnection())
                .whenComplete((success, error) -> Minecraft.getInstance().execute(() -> {
                    if (testButton != null) testButton.active = true;
                    if (error == null && Boolean.TRUE.equals(success)) {
                        status("connection ok", 2600L);
                    } else {
                        status("connection failed", 3200L);
                    }
                }));
    }

    private void status(String message, long durationMs) {
        status = message;
        statusUntil = durationMs <= 0 ? Long.MAX_VALUE : System.currentTimeMillis() + durationMs;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, this.width, this.height, 0xFF10121A, 0xFF05060A);

        int left = Math.max(24, this.width / 2 - 220);
        int width = Math.min(440, this.width - 48);

        graphics.fill(left - 14, 28, left + width + 14, this.height - 28, 0xEE151923);
        graphics.fill(left - 14, 28, left + width + 14, 31, 0xFF6B62FF);
        graphics.drawString(this.font, "meta plays minecraft", left, 37, 0xFFFFFFFF, true);
        graphics.drawString(this.font, "ai control center", left, 52, 0xFF9CA3B8);
        graphics.drawString(this.font, tab.label, left + width - this.font.width(tab.label), 38, 0xFF7C86FF);

        if (tab == Tab.PROVIDER) {
            drawSection(graphics, "connection", "choose what brain should receive the world state", left, 78, width);
            drawLabel(graphics, "provider", left, 101);
            drawLabel(graphics, "chat completions url", left, 143);
            drawLabel(graphics, "model name", left, 201);
            drawLabel(graphics, "api key (optional)", left, 259);
        } else if (tab == Tab.BRAIN) {
            drawSection(graphics, "behavior", "tune how often and how boldly the ai thinks", left, 78, width);
            drawLabel(graphics, "generation", left, 278);
            drawLabel(graphics, "timing", left, 394);
        } else {
            drawSection(graphics, "guardrails", "safety rules are intentionally conservative", left, 78, width);
        }

        if (statusUntil > System.currentTimeMillis()) {
            graphics.drawString(this.font, status, left, this.height - 47, 0xFFB8C0D8);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawSection(GuiGraphics graphics, String title, String subtitle, int x, int y, int width) {
        graphics.fill(x - 2, y, x + width + 2, y + 39, 0xFF1B1F2D);
        graphics.drawString(this.font, title, x + 8, y + 7, 0xFFEDEFFF, true);
        graphics.drawString(this.font, subtitle, x + 8, y + 21, 0xFF8D95AA);
    }

    private void drawLabel(GuiGraphics graphics, String label, int x, int y) {
        graphics.drawString(this.font, label, x, y, 0xFFAEB6CA);
    }
}
