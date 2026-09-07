package com.metaplaysminecraft;

import com.metaplaysminecraft.ai.AiPlayerController;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public final class MetaPlaysMinecraftClient implements ClientModInitializer {
    private static AiPlayerController controller;

    @Override
    public void onInitializeClient() {
        controller = new AiPlayerController();
        ClientTickEvents.END_CLIENT_TICK.register(MetaPlaysMinecraftClient::onClientTick);
        MetaPlaysMinecraft.LOGGER.info("meta plays minecraft ai controller initialized.");
    }

    private static void onClientTick(Minecraft minecraft) {
        if (controller != null) {
            controller.tick(minecraft);
        }
    }
}
