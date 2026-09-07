package com.metaplaysminecraft.actions;

import com.metaplaysminecraft.ai.AiAction;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class ActionSafety {
    private ActionSafety() {}

    public static boolean allow(LocalPlayer player, AiAction action) {
        if (!action.isKnown()) return false;

        if ("attack".equals(action.type())) {
            // CombatTargetSelector is the authoritative mob-only target selector.
            return true;
        }

        if ("chat".equals(action.type()) || "move".equals(action.type()) || "look".equals(action.type())
                || "jump".equals(action.type()) || "stop".equals(action.type()) || "noop".equals(action.type())
                || "select".equals(action.type()) || "craft".equals(action.type()) || "sleep".equals(action.type())) {
            return true;
        }

        double dx = action.x() + 0.5 - player.getX();
        double dy = action.y() + 0.5 - player.getY();
        double dz = action.z() + 0.5 - player.getZ();
        return dx * dx + dy * dy + dz * dz <= 25.0;
    }

    public static boolean isPlayer(Entity entity) {
        return entity instanceof Player;
    }
}
