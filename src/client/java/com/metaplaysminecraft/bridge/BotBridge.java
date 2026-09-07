package com.metaplaysminecraft.bridge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;

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
            stop(minecraft);
            return;
        }

        IntegratedServer server = minecraft.getSingleplayerServer();
        if (server == null) {
            message(minecraft, "meta bot: open a singleplayer world first");
            return;
        }

        if (!server.isPublished()) {
            message(minecraft, "meta bot: open your world to LAN first");
            return;
        }

        int port = server.getPort();
        if (port <= 0) {
            message(minecraft, "meta bot: couldn't determine the LAN port");
            return;
        }

        Path botDirectory = Path.of("bot").toAbsolutePath().normalize();
        Path entrypoint = botDirectory.resolve("index.js");
        if (!Files.isRegularFile(entrypoint)) {
            message(minecraft, "meta bot: bot/index.js is missing");
            return;
        }

        if (!STARTING.compareAndSet(false, true)) return;
        message(minecraft, "meta bot: starting...");

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
                        message(minecraft, "meta bot: npm install failed");
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

                message(minecraft, "meta bot: connecting to localhost:" + port);
            } catch (Exception error) {
                message(minecraft, "meta bot: " + error.getMessage());
            } finally {
                STARTING.set(false);
            }
        });
    }

    private static void stop(Minecraft minecraft) {
        Process current = process;
        process = null;
        if (current != null && current.isAlive()) current.destroy();
        message(minecraft, "meta bot: stopped");
    }

    private static void message(Minecraft minecraft, String text) {
        if (minecraft.gui != null && minecraft.gui.hud != null) {
            minecraft.gui.hud.getChat().addClientSystemMessage(Component.literal(text));
        }
    }
}
