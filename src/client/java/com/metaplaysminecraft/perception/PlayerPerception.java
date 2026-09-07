package com.metaplaysminecraft.perception;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
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
        LivingEntity nearest = player.level().getEntitiesOfClass(
                        LivingEntity.class,
                        player.getBoundingBox().inflate(16.0),
                        entity -> entity != player && !(entity instanceof Player) && entity.isAlive())
                .stream()
                .min((a, b) -> Double.compare(player.distanceToSqr(a), player.distanceToSqr(b)))
                .orElse(null);

        String nearestName = nearest == null ? "none" : nearest.getType().toString();
        return String.format(Locale.ROOT,
                "{\"ready\":true,\"dimension\":\"%s\",\"x\":%.2f,\"y\":%.2f,\"z\":%.2f,\"yaw\":%.2f,\"pitch\":%.2f,\"health\":%.1f,\"food\":%d,\"on_ground\":%s,\"nearest_nonplayer_mob\":\"%s\",\"block\":\"%s\"}",
                player.level().dimension().identifier(),
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(), player.getHealth(),
                player.getFoodData().getFoodLevel(), player.onGround(), nearestName, pos);
    }
}
