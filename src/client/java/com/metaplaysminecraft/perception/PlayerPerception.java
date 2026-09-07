package com.metaplaysminecraft.perception;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

public final class PlayerPerception {
    private PlayerPerception() {
    }

    public static String snapshot(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || player.level() == null) {
            return "{\"ready\":false}";
        }

        BlockPos pos = player.blockPosition();
        String nearestMob = "none";
        double nearestDistance = Double.MAX_VALUE;

        for (LivingEntity entity : player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(16.0),
                candidate -> candidate != player && !(candidate instanceof Player) && candidate.isAlive())) {
            double distance = player.distanceToSqr(entity);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestMob = entity.getType().toString();
            }
        }

        return String.format(Locale.ROOT,
                "{\"ready\":true,\"dimension\":\"%s\",\"x\":%.2f,\"y\":%.2f,\"z\":%.2f,\"yaw\":%.2f,\"pitch\":%.2f,\"health\":%.1f,\"food\":%d,\"on_ground\":%s,\"nearest_hostile_candidate\":\"%s\"}",
                player.level().dimension().identifier(),
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(),
                player.getHealth(), player.getFoodData().getFoodLevel(),
                player.onGround(), nearestMob);
    }
}
