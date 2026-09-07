package com.metaplaysminecraft.navigation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class NavigationController {
    private NavigationController() {}

    public static void moveToward(Minecraft minecraft, double x, double y, double z, boolean sprint) {
        LocalPlayer player = minecraft.player;
        if (player == null) return;

        double dx = x - player.getX();
        double dy = y - player.getY();
        double dz = z - player.getZ();
        if (dx * dx + dy * dy + dz * dz < 1.5) {
            minecraft.options.keyUp.setDown(false);
            minecraft.options.keySprint.setDown(false);
            return;
        }

        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        player.setYRot(yaw);
        player.setXRot(0.0f);

        boolean blocked = isBlockedAhead(player);
        minecraft.options.keyUp.setDown(true);
        minecraft.options.keyDown.setDown(false);
        minecraft.options.keyLeft.setDown(false);
        minecraft.options.keyRight.setDown(false);
        minecraft.options.keySprint.setDown(sprint && !blocked);
        minecraft.options.keyJump.setDown(blocked || dy > 0.8);
    }

    private static boolean isBlockedAhead(LocalPlayer player) {
        double radians = Math.toRadians(player.getYRot());
        double dx = -Math.sin(radians);
        double dz = Math.cos(radians);
        BlockPos feet = player.blockPosition();
        BlockState front = player.level().getBlockState(feet.offset((int) Math.round(dx), 0, (int) Math.round(dz)));
        BlockState above = player.level().getBlockState(feet.offset((int) Math.round(dx), 1, (int) Math.round(dz)));
        return !front.getCollisionShape(player.level(), feet).isEmpty() && above.getCollisionShape(player.level(), feet).isEmpty();
    }
}
