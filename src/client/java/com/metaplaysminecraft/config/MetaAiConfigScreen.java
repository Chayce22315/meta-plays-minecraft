package com.metaplaysminecraft.config;

import com.metaplaysminecraft.ai.LocalAiClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.Consumer;

public final class MetaAiConfigScreen extends Screen {
    private enum Tab {
        PROVIDER("provider"),
        BRAIN("brain"),
        TUNING("tuning"),
        SAFETY("safety");

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

    @Override
    protected void rebuildWidgets() {
        clearWidgets();
        endpointBox = null;
        modelBox = null;
        apiKeyBox = null;
        temperatureBox = null;
        maxTokensBox = null;
        timeoutBox = null;
        intervalBox = null;
        testButton = null;

        int panelWidth = Math.min(620, this.width - 32);
        int left = Math.max(16, (this.width - panelWidth) / 2);
        int navY = 47;
        int gap = 6;
        int navWidth = (panelWidth - (gap * 4)) / 5;

        addRenderableWidget(Button.builder(Component.literal("provider"), b -> changeTab(Tab.PROVIDER))
                .bounds(left, navY, navWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("brain"), b -> changeTab(Tab.BRAIN))
                .bounds(left + navWidth + gap, navY, navWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("tuning"), b -> changeTab(Tab.TUNING))
                .bounds(left + (navWidth + gap) * 2, navY, navWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("safety"), b -> changeTab(Tab.SAFETY))
                .bounds(left + (navWidth + gap) * 3, navY, navWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("done"), b -> done())
                .bounds(left + (navWidth + gap) * 4, navY, navWidth, 20).build());

        int contentTop = 78;
        int contentWidth = panelWidth;
        int innerLeft = left + 12;
        int innerWidth = contentWidth - 24;
        int columnGap = 10;
        int columnWidth = (innerWidth - columnGap) / 2;

        if (tab == Tab.PROVIDER) {
            drawlessSectionButton("connection", "configure the ai service and verify it works", left, contentTop, panelWidth);

            int providerY = contentTop + 44;
            int providerGap = 6;
            int providerWidth = (panelWidth - providerGap * 2) / 3;
            addRenderableWidget(Button.builder(labelFor(MetaAiConfig.Provider.OLLAMA), b -> selectProvider(MetaAiConfig.Provider.OLLAMA))
                    .bounds(left, providerY, providerWidth, 20).build());
            addRenderableWidget(Button.builder(labelFor(MetaAiConfig.Provider.OPENAI_COMPATIBLE), b -> selectProvider(MetaAiConfig.Provider.OPENAI_COMPATIBLE))
                    .bounds(left + providerWidth + providerGap, providerY, providerWidth, 20).build());
            addRenderableWidget(Button.builder(labelFor(MetaAiConfig.Provider.LOCAL), b -> selectProvider(MetaAiConfig.Provider.LOCAL))
                    .bounds(left + (providerWidth + providerGap) * 2, providerY, providerWidth, 20).build());

            endpointBox = addBox("chat completions url", config.endpoint, innerLeft, providerY + 40, innerWidth);
            modelBox = addBox("model name", config.model, innerLeft, providerY + 88, innerWidth);
            apiKeyBox = addBox("api key (optional)", config.apiKey, innerLeft, providerY + 136, innerWidth);
            apiKeyBox.setMaxLength(512);
            apiKeyBox.setSuggestion("leave blank for local servers");

            int actionY = providerY + 188;
            testButton = addRenderableWidget(Button.builder(Component.literal("test connection"), b -> testConnection())
                    .bounds(left, actionY, Math.min(180, panelWidth / 2 - 5), 20).build());
            addRenderableWidget(Button.builder(Component.literal("reset defaults"), b -> {
                config.reset();
                config.save();
                rebuildWidgets();
                status("defaults restored", 2400L);
            }).bounds(left + Math.min(190, panelWidth / 2 + 5), actionY,
                    Math.min(180, panelWidth / 2 - 5), 20).build());

            drawlessHint("provider controls the endpoint family; the fields below stay editable", innerLeft, actionY + 29, innerWidth);
        } else if (tab == Tab.BRAIN) {
            drawlessSectionButton("behavior", "turn major autonomous systems on or off", left, contentTop, panelWidth);

            int y = contentTop + 44;
            y = addToggleGridRow("ai controller", config.aiEnabled, "real metabot bridge", config.useRealMetaBot,
                    innerLeft, y, columnWidth, columnGap,
                    value -> config.aiEnabled = value, value -> config.useRealMetaBot = value);
            y = addToggleGridRow("session memory", config.memoryEnabled, "autonomous crafting", config.autonomousCrafting,
                    innerLeft, y + 6, columnWidth, columnGap,
                    value -> config.memoryEnabled = value, value -> config.autonomousCrafting = value);
            addToggle("autonomous food / survival", config.autonomousSurvival, innerLeft, y + 6, innerWidth,
                    value -> config.autonomousSurvival = value);

            drawlessHint("changes save immediately, and the ai controller reads them during gameplay", innerLeft, y + 39, innerWidth);
        } else if (tab == Tab.TUNING) {
            drawlessSectionButton("generation & timing", "control how frequently the brain thinks and how long requests can run", left, contentTop, panelWidth);

            int y = contentTop + 46;
            temperatureBox = addBox("temperature (0.0 - 2.0)", format(config.temperature), innerLeft, y, columnWidth);
            maxTokensBox = addBox("max output tokens", Integer.toString(config.maxTokens), innerLeft + columnWidth + columnGap, y, columnWidth);
            timeoutBox = addBox("request timeout seconds", Integer.toString(config.requestTimeoutSeconds), innerLeft, y + 50, columnWidth);
            intervalBox = addBox("decision interval ticks", Integer.toString(config.decisionIntervalTicks), innerLeft + columnWidth + columnGap, y + 50, columnWidth);

            drawlessCard("temperature", "lower values are steadier; higher values are more varied", innerLeft, y + 105, columnWidth);
            drawlessCard("max tokens", "caps the size of each ai decision response", innerLeft + columnWidth + columnGap, y + 105, columnWidth);
            drawlessCard("timeout", "how long a provider request may take before failing", innerLeft, y + 166, columnWidth);
            drawlessCard("decision interval", "smaller values react faster but create more requests", innerLeft + columnWidth + columnGap, y + 166, columnWidth);
        } else {
            drawlessSectionButton("guardrails", "safety rules stay conservative even when options are enabled", left, contentTop, panelWidth);

            int y = contentTop + 46;
            addToggle("hostile mob combat", config.hostileMobCombat, innerLeft, y, innerWidth,
                    value -> config.hostileMobCombat = value);
            addRenderableWidget(Button.builder(Component.literal("player combat: permanently disabled"),
                            b -> status("player targets are hard-blocked", 2600L))
                    .bounds(innerLeft, y + 34, innerWidth, 20).build());
            addRenderableWidget(Button.builder(Component.literal("distance limits: enabled"),
                            b -> status("actions keep conservative interaction ranges", 2600L))
                    .bounds(innerLeft, y + 62, innerWidth, 20).build());
            addRenderableWidget(Button.builder(Component.literal("recovery: small jump / forward only"),
                            b -> status("stuck recovery never teleports", 2600L))
                    .bounds(innerLeft, y + 90, innerWidth, 20).build());
            addRenderableWidget(Button.builder(Component.literal("hostile-only targeting"),
                            b -> status("the target selector excludes player entities", 2600L))
                    .bounds(innerLeft, y + 118, innerWidth, 20).build());

            drawlessHint("these guardrails are enforced by the action layer, not just the ai prompt", innerLeft, y + 151, innerWidth);
        }
    }

    private void drawlessSectionButton(String title, String subtitle, int x, int y, int width) {
        addRenderableWidget(Button.builder(Component.literal(title + "  •  " + subtitle), b -> status(title + " section", 1200L))
                .bounds(x, y, width, 26).build());
    }

    private int addToggleGridRow(String leftLabel, boolean leftValue, String rightLabel, boolean rightValue,
                                 int x, int y, int columnWidth, int gap,
                                 Consumer<Boolean> leftSetter, Consumer<Boolean> rightSetter) {
        addToggle(leftLabel, leftValue, x, y, columnWidth, leftSetter);
        addToggle(rightLabel, rightValue, x + columnWidth + gap, y, columnWidth, rightSetter);
        return y + 26;
    }

    private Component labelFor(MetaAiConfig.Provider provider) {
        String title = switch (provider) {
            case OLLAMA -> "ollama";
            case OPENAI_COMPATIBLE -> "openai compatible";
            case LOCAL -> "local server";
        };
        return Component.literal(config.provider == provider ? "✓ " + title : title);
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
        EditBox box = new EditBox(this.font, x, y + 14, width, 20, Component.literal(label));
        box.setValue(value == null ? "" : value);
        box.setMaxLength(2048);
        addRenderableWidget(box);
        return box;
    }

    private Button addToggle(String label, boolean value, int x, int y, int width, Consumer<Boolean> setter) {
        return addRenderableWidget(Button.builder(toggleLabel(label, value), b -> {
            boolean next = !b.getMessage().getString().endsWith("on");
            setter.accept(next);
            b.setMessage(toggleLabel(label, next));
            config.save();
        }).bounds(x, y, width, 26).build());
    }

    private Component toggleLabel(String label, boolean value) {
        return Component.literal(label + "  •  " + (value ? "on" : "off"));
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
        Minecraft.getInstance().gui.setScreen(parent);
    }

    private void onCloseToParent() {
        done();
    }

    @Override
    public void onClose() {
        onCloseToParent();
    }

    private void testConnection() {
        saveFields();
        config.save();
        if (testButton == null) return;
        testButton.active = false;
        status("testing...", 0L);
        LocalAiClient.testConnection().whenComplete((success, error) -> Minecraft.getInstance().execute(() -> {
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        graphics.fillGradient(0, 0, this.width, this.height, 0xFF10121A, 0xFF05060A);

        int panelWidth = Math.min(620, this.width - 32);
        int left = Math.max(16, (this.width - panelWidth) / 2);
        int bottom = this.height - 24;

        graphics.fill(left - 8, 18, left + panelWidth + 8, bottom, 0xEE151923);
        graphics.fill(left - 8, 18, left + panelWidth + 8, 21, 0xFF6B62FF);
        graphics.text(this.font, "meta plays minecraft", left, 28, 0xFFFFFFFF, true);
        graphics.text(this.font, "ai control center", left, 40, 0xFF9CA3B8);

        String provider = switch (config.provider) {
            case OLLAMA -> "ollama";
            case OPENAI_COMPATIBLE -> "openai compatible";
            case LOCAL -> "local";
        };
        String liveState = config.aiEnabled ? "ai on" : "ai off";
        String bridgeState = config.useRealMetaBot ? "metabot on" : "metabot off";
        String summary = provider + "  •  " + liveState + "  •  " + bridgeState;
        graphics.text(this.font, summary, left + panelWidth - this.font.width(summary), 29, 0xFF7C86FF);

        if (statusUntil > System.currentTimeMillis()) {
            graphics.text(this.font, status, left, bottom + 6 - this.font.lineHeight, 0xFFB8C0D8);
        }
    }

    private void drawlessHint(String text, int x, int y, int width) {
        String clipped = text;
        while (this.font.width(clipped) > width && clipped.length() > 4) {
            clipped = clipped.substring(0, clipped.length() - 4) + "...";
        }
        // Rendered during extractRenderState through a lightweight widget to keep Minecraft 26.2's GUI pipeline happy.
        addRenderableWidget(Button.builder(Component.literal(clipped), b -> status("info", 900L))
                .bounds(x, y, width, 20).build());
    }

    private void drawlessCard(String title, String subtitle, int x, int y, int width) {
        addRenderableWidget(Button.builder(Component.literal(title + "  •  " + subtitle), b -> status(title, 900L))
                .bounds(x, y, width, 42).build());
    }
}
