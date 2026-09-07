package com.metaplaysminecraft.bridge;

import com.metaplaysminecraft.ai.AiAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BotBridge {
    private static final AtomicBoolean STARTING = new AtomicBoolean(false);
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
    private static final URI ACTION_ENDPOINT = URI.create("http://127.0.0.1:8765/action");
    private static volatile Process process;

    private BotBridge() {}

    public static boolean isRunning() {
        Process current = process;
        return current != null && current.isAlive();
    }

    public static void toggle(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null) return;
        if (isRunning()) { stop(minecraft); return; }

        var server = minecraft.getSingleplayerServer();
        if (server == null) { message(minecraft, "meta bot: open a singleplayer world first"); return; }
        if (!server.isPublished()) { message(minecraft, "meta bot: open your world to LAN first"); return; }

        int port = server.getPort();
        if (port <= 0) { message(minecraft, "meta bot: no LAN port is available"); return; }

        Path botDirectory = Path.of("bot").toAbsolutePath().normalize();
        Path entrypoint = botDirectory.resolve("index.js");
        if (!Files.isRegularFile(entrypoint)) { message(minecraft, "meta bot: bot/index.js is missing"); return; }
        if (!STARTING.compareAndSet(false, true)) return;
        message(minecraft, "meta bot: starting...");

        Thread.startVirtualThread(() -> {
            try {
                Path nodeModules = botDirectory.resolve("node_modules");
                if (!Files.isDirectory(nodeModules)) {
                    Process install = new ProcessBuilder("npm", "install", "--no-fund", "--no-audit")
                            .directory(botDirectory.toFile()).redirectErrorStream(true).inheritIO().start();
                    if (install.waitFor() != 0) { message(minecraft, "meta bot: npm install failed"); return; }
                }
                process = new ProcessBuilder("node", entrypoint.toString(), "--host", "127.0.0.1", "--port", Integer.toString(port), "--username", "MetaBot", "--control-port", "8765")
                        .directory(botDirectory.toFile()).redirectErrorStream(true).inheritIO().start();
                message(minecraft, "meta bot: connecting to localhost:" + port);
            } catch (Exception error) {
                message(minecraft, "meta bot: " + safeMessage(error));
            } finally { STARTING.set(false); }
        });
    }

    public static void sendAction(AiAction action) {
        if (!isRunning() || action == null) return;
        HttpRequest request = HttpRequest.newBuilder(ACTION_ENDPOINT)
                .timeout(Duration.ofSeconds(2)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(actionJson(action))).build();
        HTTP.sendAsync(request, HttpResponse.BodyHandlers.discarding()).exceptionally(error -> null);
    }

    private static void stop(Minecraft minecraft) {
        Process current = process;
        process = null;
        if (current != null && current.isAlive()) current.destroy();
        message(minecraft, "meta bot: stopped");
    }

    private static void message(Minecraft minecraft, String text) {
        if (minecraft.gui != null) minecraft.gui.getChat().addMessage(Component.literal(text));
    }

    private static String safeMessage(Exception error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }

    private static String actionJson(AiAction action) {
        return "{\"type\":\"" + escape(action.type()) + "\",\"target\":\"" + escape(action.target()) + "\",\"yaw\":" + action.yaw()
                + ",\"pitch\":" + action.pitch() + ",\"forward\":" + (action.forward() > 0.15f) + ",\"back\":" + (action.forward() < -0.15f)
                + ",\"left\":" + (action.sideways() < -0.15f) + ",\"right\":" + (action.sideways() > 0.15f) + ",\"jump\":" + action.jump()
                + ",\"sprint\":" + action.sprint() + ",\"sneak\":" + action.crouch() + ",\"slot\":" + action.slot() + ",\"x\":" + action.x()
                + ",\"y\":" + action.y() + ",\"z\":" + action.z() + ",\"message\":\"" + escape(action.message()) + "\"}";
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }
}
