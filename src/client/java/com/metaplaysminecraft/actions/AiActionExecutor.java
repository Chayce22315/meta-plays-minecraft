package com.metaplaysminecraft.actions;

import com.metaplaysminecraft.MetaPlaysMinecraft;
import com.metaplaysminecraft.ai.AiAction;
import com.metaplaysminecraft.combat.CombatTargetSelector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Locale;

public final class AiActionExecutor {
    private AiActionExecutor() {
    }

    public static void execute(Minecraft minecraft, AiAction action) {
        if (minecraft.player == null || !action.isKnown()) {
            return;
        }

        LocalPlayer player = minecraft.player;
        switch (action.type()) {
            case "noop" -> clearMovement(minecraft);
            case "look" -> {
                player.setYRot(action.yaw());
                player.setXRot(action.pitch());
            }
            case "move" -> applyMovement(minecraft, action);
            case "jump" -> {
                clearMovement(minecraft);
                minecraft.options.keyJump.setDown(true);
            }
            case "chat" -> sendChat(player, action.message());
            case "attack" -> attackHostile(minecraft, player);
            default -> {
            }
        }
    }

    public static void clearTransientKeys(Minecraft minecraft) {
        minecraft.options.keyJump.setDown(false);
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
