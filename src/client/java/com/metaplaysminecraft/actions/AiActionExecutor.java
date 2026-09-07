package com.metaplaysminecraft.actions;

import com.metaplaysminecraft.ai.AiAction;
import com.metaplaysminecraft.combat.CombatTargetSelector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class AiActionExecutor {
    private AiActionExecutor() {
    }

    public static void execute(Minecraft minecraft, AiAction action) {
        LocalPlayer player = minecraft.player;
        if (player == null || !action.isKnown()) {
            return;
        }

        switch (action.type()) {
            case "noop" -> clearMovement(minecraft);
            case "look" -> {
                player.setYRot(action.yaw());
                player.setXRot(action.pitch());
            }
            case "move" -> applyMovement(minecraft, action);
            case "jump" -> minecraft.options.keyJump.setDown(true);
            case "chat" -> sendChat(player, action.message());
            case "attack" -> attackHostile(minecraft, player);
            default -> {
            }
        }
    }

    private static void applyMovement(Minecraft minecraft, AiAction action) {
        minecraft.options.keyUp.setDown(action.forward() > 0.15f);
        minecraft.options.keyDown.setDown(action.forward() < -0.15f);
        minecraft.options.keyLeft.setDown(action.sideways() < -0.15f);
        minecraft.options.keyRight.setDown(action.sideways() > 0.15f);
        minecraft.options.keyJump.setDown(action.jump());
    }

    private static void clearMovement(Minecraft minecraft) {
        minecraft.options.keyUp.setDown(false);
        minecraft.options.keyDown.setDown(false);
        minecraft.options.keyLeft.setDown(false);
        minecraft.options.keyRight.setDown(false);
        minecraft.options.keyJump.setDown(false);
    }

    private static void sendChat(LocalPlayer player, String message) {
        String safe = message == null ? "" : message.strip();
        if (!safe.isEmpty() && safe.length() <= 256) {
            player.connection.sendChat(safe);
        }
    }

    private static void attackHostile(Minecraft minecraft, LocalPlayer player) {
        if (minecraft.gameMode == null) {
            return;
        }
        CombatTargetSelector.findHostileTarget(player).ifPresent(target -> {
            if (CombatTargetSelector.isValidHostileTarget(player, target)) {
                minecraft.gameMode.attack(player, target);
            }
        });
    }
}
