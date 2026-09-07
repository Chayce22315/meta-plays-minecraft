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
        int innerLeft = left + 12;
        int innerWidth = panelWidth - 24;
        int columnGap = 10;
        int columnWidth = (innerWidth - columnGap) / 2;

        if (tab == Tab.PROVIDER) {
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
            int firstActionWidth = Math.min(180, panelWidth / 2 - 5);
            int secondActionX = left + panelWidth - firstActionWidth;
            testButton = addRenderableWidget(Button.builder(Component.literal("test connection"), b -> testConnection())
                    .bounds(left, actionY, firstActionWidth, 20).build());
            addRenderableWidget(Button.builder(Component.literal("reset defaults"), b -> {
                config.reset();
                config.save();
                rebuildWidgets();
                status("defaults restored", 2400L);
            }).bounds(secondActionX, actionY, firstActionWidth, 20).build());
        } else if (tab == Tab.BRAIN) {
            int y = contentTop + 44;
            y = addToggleGridRow("ai controller", config.aiEnabled, "real metabot bridge", config.useRealMetaBot,
                    innerLeft, y, columnWidth, columnGap,
                    value -> config.aiEnabled = value, value -> config.useRealMetaBot = value);
            y = addToggleGridRow("session memory", config.memoryEnabled, "autonomous crafting", config.autonomousCrafting,
                    innerLeft, y + 6, columnWidth, columnGap,
                    value -> config.memoryEnabled = value, value -> config.autonomousCrafting = value);
            addToggle("autonomous food / survival", config.autonomousSurvival, innerLeft, y + 6, innerWidth,
                    value -> config.autonomousSurvival = value);
        } else if (tab == Tab.TUNING) {
            int y = contentTop + 46;
            temperatureBox = addBox("temperature (0.0 - 2.0)", format(config.temperature), innerLeft, y, columnWidth);
            maxTokensBox = addBox("max output tokens", Integer.toString(config.maxTokens), innerLeft + columnWidth + columnGap, y, columnWidth);
            timeoutBox = addBox("request timeout seconds", Integer.toString(config.requestTimeoutSeconds), innerLeft, y + 50, columnWidth);
            intervalBox = addBox("decision interval ticks", Integer.toString(config.decisionIntervalTicks), innerLeft + columnWidth + columnGap, y + 50, columnWidth);
        } else {
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
        }
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
        }).bounds(x, y, width, 20).build());
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

    @Override
    public void onClose() {
        done();
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

        int panelWidth = Math.min(620, this.width - 32);
        int left = Math.max(16, (this.width - panelWidth) / 2);
        int innerLeft = left + 12;
        int innerWidth = panelWidth - 24;
        int bottom = this.height - 24;
        int contentTop = 78;

        graphics.fillGradient(0, 0, this.width, this.height, 0xFF10121A, 0xFF05060A);
        graphics.fill(left - 8, 18, left + panelWidth + 8, bottom, 0xEE151923);
        graphics.fill(left - 8, 18, left + panelWidth + 8, 21, 0xFF6B62FF);
        graphics.text(this.font, "meta plays minecraft", left, 28, 0xFFFFFFFF, true);
        graphics.text(this.font, "ai control center", left, 40, 0xFF9CA3B8);

        String provider = switch (config.provider) {
            case OLLAMA -> "ollama";
            case OPENAI_COMPATIBLE -> "openai compatible";
            case LOCAL -> "local";
        };
        String summary = provider + "  •  " + (config.aiEnabled ? "ai on" : "ai off")
                + "  •  " + (config.useRealMetaBot ? "metabot on" : "metabot off");
        graphics.text(this.font, summary, left + panelWidth - this.font.width(summary), 29, 0xFF7C86FF);

        drawSection(graphics, tab, contentTop, innerLeft, innerWidth);

        if (tab == Tab.PROVIDER) {
            graphics.text(this.font, "provider", innerLeft, 101, 0xFFAEB6CA);
            graphics.text(this.font, "chat completions url", innerLeft, 137, 0xFFAEB6CA);
            graphics.text(this.font, "model name", innerLeft, 185, 0xFFAEB6CA);
            graphics.text(this.font, "api key (optional)", innerLeft, 233, 0xFFAEB6CA);
            drawHint(graphics, "provider controls the endpoint family; fields remain editable", innerLeft, 298, innerWidth);
        } else if (tab == Tab.BRAIN) {
            drawHint(graphics, "changes save immediately and are read during gameplay", innerLeft, 247, innerWidth);
        } else if (tab == Tab.TUNING) {
            drawCard(graphics, "temperature", "lower = steadier, higher = more varied", innerLeft, 229, innerWidth / 2 - 5);
            drawCard(graphics, "max tokens", "limits the size of each ai response", innerLeft + innerWidth / 2 + 5, 229, innerWidth / 2 - 5);
            drawCard(graphics, "timeout", "max wait before a provider request fails", innerLeft, 276, innerWidth / 2 - 5);
            drawCard(graphics, "decision interval", "smaller = faster reactions and more requests", innerLeft + innerWidth / 2 + 5, 276, innerWidth / 2 - 5);
        } else {
            drawHint(graphics, "guardrails are enforced by the action layer, not only the ai prompt", innerLeft, 250, innerWidth);
        }

        if (statusUntil > System.currentTimeMillis()) {
            graphics.text(this.font, status, left, bottom + 6 - this.font.lineHeight, 0xFFB8C0D8);
        }
    }

    private void drawSection(GuiGraphicsExtractor graphics, Tab tab, int y, int x, int width) {
        String title = switch (tab) {
            case PROVIDER -> "connection";
            case BRAIN -> "behavior";
            case TUNING -> "generation & timing";
            case SAFETY -> "guardrails";
        };
        String subtitle = switch (tab) {
            case PROVIDER -> "configure the ai service and verify the connection";
            case BRAIN -> "control the autonomous systems that drive the player";
            case TUNING -> "control how often the brain thinks and how requests behave";
            case SAFETY -> "conservative rules that keep the player from attacking people";
        };
        graphics.fill(x, y, x + width, y + 34, 0xFF1B1F2D);
        graphics.text(this.font, title, x + 8, y + 6, 0xFFEDEFFF, true);
        graphics.text(this.font, fitText(subtitle, width - 16), x + 8, y + 19, 0xFF8D95AA);
    }

    private void drawHint(GuiGraphicsExtractor graphics, String text, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 22, 0xFF1B1F2D);
        graphics.text(this.font, fitText(text, width - 12), x + 6, y + 7, 0xFF8D95AA);
    }

    private void drawCard(GuiGraphicsExtractor graphics, String title, String subtitle, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 40, 0xFF1B1F2D);
        graphics.text(this.font, title, x + 7, y + 6, 0xFFEDEFFF, true);
        graphics.text(this.font, fitText(subtitle, width - 14), x + 7, y + 20, 0xFF8D95AA);
    }

    private String fitText(String text, int width) {
        if (this.font.width(text) <= width) return text;
        String clipped = text;
        while (clipped.length() > 4 && this.font.width(clipped + "...") > width) {
            clipped = clipped.substring(0, clipped.length() - 1);
        }
        return clipped + "...";
    }
}
