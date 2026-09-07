package com.metaplaysminecraft;

import com.metaplaysminecraft.ai.AiPlayerController;
import com.metaplaysminecraft.bridge.BotBridge;
import com.metaplaysminecraft.config.MetaAiConfig;
import com.metaplaysminecraft.perception.ChatMemory;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class MetaPlaysMinecraftClient implements ClientModInitializer {
    private static AiPlayerController controller;
    private static final KeyMapping JOIN_BOT = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.metaplaysminecraft.join_bot",
                    InputConstants.Type.KEYSYM,
                    InputConstants.KEY_F8,
                    KeyMapping.Category.register(Identifier.fromNamespaceAndPath("metaplaysminecraft", "controls"))
            )
    );

    @Override
    public void onInitializeClient() {
        MetaAiConfig.get();
        controller = new AiPlayerController();
        ChatMemory.register();
        ClientTickEvents.END_CLIENT_TICK.register(MetaPlaysMinecraftClient::onClientTick);
        MetaPlaysMinecraft.LOGGER.info("meta plays minecraft full ai player initialized.");
    }

    private static void onClientTick(Minecraft minecraft) {
        while (JOIN_BOT.consumeClick()) {
            BotBridge.toggle(minecraft);
        }
        if (controller != null) {
            controller.tick(minecraft);
        }
    }
}
