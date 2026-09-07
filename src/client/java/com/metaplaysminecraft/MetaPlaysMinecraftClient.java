package com.metaplaysminecraft;

import net.fabricmc.api.ClientModInitializer;

public final class MetaPlaysMinecraftClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MetaPlaysMinecraft.LOGGER.info("meta plays minecraft client initialized.");
	}
}
