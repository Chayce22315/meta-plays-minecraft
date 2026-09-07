package com.metaplaysminecraft.bridge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BotBridge {
    private static final AtomicBoolean STARTING = new AtomicBoolean(false);
    private static volatile Process process;

    private BotBridge() {}

    public static void toggle(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null) return;

        if (process != null && process.isAlive()) {
            stop(player);
            return;
        }

        var server = minecraft.getSingleplayerServer();
        if (server == null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("meta bot: open a singleplayer world first"), false);
            return;
        }

        int port = server.getServerPort();
        if (port <= 0) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("meta bot: open your world to LAN first"), false);
            return;
        }

        Path botDirectory = Path.of("bot").toAbsolutePath().normalize();
        Path entrypoint = botDirectory.resolve("index.js");
        if (!Files.isRegularFile(entrypoint)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("meta bot: bot/index.js is missing"), false);
            return;
        }

        if (!STARTING.compareAndSet(false, true)) return;
        player.displayClientMessage(net.minecraft.network.chat.Component.literal("meta bot: starting..."), false);

        Thread.startVirtualThread(() -> {
            try {
                Path nodeModules = botDirectory.resolve("node_modules");
                if (!Files.isDirectory(nodeModules)) {
                    Process install = new ProcessBuilder("npm", "install", "--no-fund", "--no-audit")
                            .directory(botDirectory.toFile())
                            .redirectErrorStream(true)
                            .inheritIO()
                            .start();
                    if (install.waitFor() != 0) {
                        player.displayClientMessage(net.minecraft.network.chat.Component.literal("meta bot: npm install failed"), false);
                        return;
                    }
                }

                process = new ProcessBuilder(
                        "node",
                        entrypoint.toString(),
                        "--host", "127.0.0.1",
                        "--port", Integer.toString(port),
                        "--username", "MetaBot",
                        "--control-port", "8765"
                ).directory(botDirectory.toFile()).redirectErrorStream(true).inheritIO().start();

                player.displayClientMessage(net.minecraft.network.chat.Component.literal("meta bot: connecting to localhost:" + port), false);
            } catch (Exception error) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("meta bot: " + error.getMessage()), false);
            } finally {
                STARTING.set(false);
            }
        });
    }

    private static void stop(LocalPlayer player) {
        Process current = process;
        process = null;
        if (current != null && current.isAlive()) current.destroy();
        player.displayClientMessage(net.minecraft.network.chat.Component.literal("meta bot: stopped"), false);
    }
}
