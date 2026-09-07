package com.metaplaysminecraft.perception;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

import java.util.ArrayDeque;
import java.util.Deque;

public final class ChatMemory {
    private static final int MAX_MESSAGES = 12;
    private static final Deque<String> RECENT = new ArrayDeque<>();

    private ChatMemory() {}

    public static void register() {
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, timestamp) -> {
            String name = sender instanceof GameProfile profile ? profile.name() : "player";
            add(name + ": " + message.getString());
        });
    }

    public static synchronized void add(String message) {
        RECENT.addLast(message.replace("\n", " "));
        while (RECENT.size() > MAX_MESSAGES) RECENT.removeFirst();
    }

    public static synchronized String snapshot() {
        return String.join(" | ", RECENT);
    }
}
