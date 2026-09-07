package com.metaplaysminecraft.perception;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PlayerPerception {
    private PlayerPerception() {}

    public static String snapshot(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || player.level() == null) return "{\"ready\":false}";

        List<String> mobs = new ArrayList<>();
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(16), e -> e != player && !(e instanceof Player) && e.isAlive())) {
            if (mobs.size() >= 8) break;
            mobs.add(entity.getType().toString() + "@" + fmt(entity.getX()) + "," + fmt(entity.getY()) + "," + fmt(entity.getZ()));
        }

        List<String> players = new ArrayList<>();
        for (Player other : player.level().players()) {
            if (other != player && players.size() < 8) {
                players.add(other.getGameProfile().name() + "@" + fmt(other.getX()) + "," + fmt(other.getY()) + "," + fmt(other.getZ()));
            }
        }

        List<String> drops = new ArrayList<>();
        for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(12), ItemEntity::isAlive)) {
            if (drops.size() >= 8) break;
            drops.add(item.getItem().getItem().toString() + "x" + item.getItem().getCount());
        }

        BlockPos pos = player.blockPosition();
        String held = player.getMainHandItem().isEmpty() ? "empty" : player.getMainHandItem().getItem().toString() + "x" + player.getMainHandItem().getCount();
        String dimension = String.valueOf(player.level().dimension().identifier());
        String nearbyBlock = player.level().getBlockState(pos.below()).getBlock().toString();
        boolean craftingTableNearby = findBlock(player, Blocks.CRAFTING_TABLE, 6) != null;
        boolean bedNearby = findBed(player, 8) != null;

        return String.format(Locale.ROOT,
                "{\"ready\":true,\"dimension\":\"%s\",\"pos\":[%.2f,%.2f,%.2f],\"rotation\":[%.2f,%.2f],\"health\":%.1f,\"food\":%d,\"air\":%d,\"xp\":%d,\"on_ground\":%s,\"sneaking\":%s,\"sprinting\":%s,\"held\":\"%s\",\"below\":\"%s\",\"mobs\":%s,\"players\":%s,\"drops\":%s,\"crafting_table_nearby\":%s,\"bed_nearby\":%s,\"time\":%d,\"rain\":%s}",
                dimension,
                player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(),
                player.getHealth(), player.getFoodData().getFoodLevel(), player.getAirSupply(), player.totalExperience,
                player.onGround(), player.isShiftKeyDown(), player.isSprinting(),
                escape(held), escape(nearbyBlock), list(mobs), list(players), list(drops),
                craftingTableNearby, bedNearby, player.level().getDayTime(), player.level().isRaining());
    }

    private static BlockPos findBlock(LocalPlayer player, net.minecraft.world.level.block.Block wanted, int radius) {
        BlockPos origin = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius), origin.offset(radius, radius, radius))) {
            if (player.level().getBlockState(pos).is(wanted)) return pos.immutable();
        }
        return null;
    }

    private static BlockPos findBed(LocalPlayer player, int radius) {
        BlockPos origin = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius), origin.offset(radius, radius, radius))) {
            if (player.level().getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.BedBlock) return pos.immutable();
        }
        return null;
    }

    private static String list(List<String> values) {
        return "[\"" + String.join("\",\"", values.stream().map(PlayerPerception::escape).toList()) + "\"]";
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
