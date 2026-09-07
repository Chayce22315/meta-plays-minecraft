package com.metaplaysminecraft.actions;

import com.metaplaysminecraft.ai.AiAction;
import com.metaplaysminecraft.combat.CombatTargetSelector;
import com.metaplaysminecraft.config.MetaAiConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BedBlock;

public final class AiActionExecutor {
    private AiActionExecutor() {}

    public static void execute(Minecraft minecraft, AiAction action) {
        LocalPlayer player = minecraft.player;
        if (player == null || !ActionSafety.allow(player, action)) {
            clearMovement(minecraft);
            return;
        }

        switch (action.type()) {
            case "noop", "stop" -> clearMovement(minecraft);
            case "look" -> look(player, action.yaw(), action.pitch());
            case "move" -> applyMovement(minecraft, action);
            case "jump" -> {
                clearDirectional(minecraft);
                minecraft.options.keyJump.setDown(true);
            }
            case "chat" -> sendChat(player, action.message());
            case "attack" -> {
                if (MetaAiConfig.get().hostileMobCombat) attackHostile(minecraft, player);
                else clearMovement(minecraft);
            }
            case "mine" -> holdAtBlock(minecraft, player, action, true);
            case "place", "use", "interact" -> holdAtTarget(minecraft, player, action);
            case "sleep" -> sleep(minecraft, player);
            case "craft" -> CraftingController.start(minecraft, player, action.target());
            case "select" -> select(player, action.slot());
            default -> clearMovement(minecraft);
        }
    }

    private static void look(LocalPlayer player, float yaw, float pitch) {
        player.setYRot(yaw);
        player.setXRot(Math.max(-90.0f, Math.min(90.0f, pitch)));
    }

    private static void applyMovement(Minecraft minecraft, AiAction action) {
        minecraft.options.keyUp.setDown(action.forward() > 0.15f);
        minecraft.options.keyDown.setDown(action.forward() < -0.15f);
        minecraft.options.keyLeft.setDown(action.sideways() < -0.15f);
        minecraft.options.keyRight.setDown(action.sideways() > 0.15f);
        minecraft.options.keyJump.setDown(action.jump());
        minecraft.options.keySprint.setDown(action.sprint());
        minecraft.options.keyShift.setDown(action.crouch());
    }

    private static void clearDirectional(Minecraft minecraft) {
        minecraft.options.keyUp.setDown(false);
        minecraft.options.keyDown.setDown(false);
        minecraft.options.keyLeft.setDown(false);
        minecraft.options.keyRight.setDown(false);
        minecraft.options.keySprint.setDown(false);
        minecraft.options.keyShift.setDown(false);
    }

    public static void clearMovement(Minecraft minecraft) {
        clearDirectional(minecraft);
        minecraft.options.keyJump.setDown(false);
        minecraft.options.keyAttack.setDown(false);
        minecraft.options.keyUse.setDown(false);
    }

    private static void sendChat(LocalPlayer player, String message) {
        String safe = message == null ? "" : message.strip();
        if (!safe.isEmpty() && safe.length() <= 256) player.connection.sendChat(safe);
    }

    private static void attackHostile(Minecraft minecraft, LocalPlayer player) {
        if (minecraft.gameMode == null) return;
        CombatTargetSelector.findHostileTarget(player).ifPresent(target -> {
            if (CombatTargetSelector.isValidHostileTarget(player, target)) minecraft.gameMode.attack(player, target);
        });
    }

    private static void holdAtBlock(Minecraft minecraft, LocalPlayer player, AiAction action, boolean breaking) {
        BlockPos pos = new BlockPos(action.x(), action.y(), action.z());
        aimAt(player, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        minecraft.options.keyAttack.setDown(breaking && !player.level().getBlockState(pos).isAir());
        minecraft.options.keyUse.setDown(false);
    }

    private static void holdAtTarget(Minecraft minecraft, LocalPlayer player, AiAction action) {
        BlockPos pos = new BlockPos(action.x(), action.y(), action.z());
        aimAt(player, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        minecraft.options.keyAttack.setDown(false);
        minecraft.options.keyUse.setDown(true);
    }

    private static void sleep(Minecraft minecraft, LocalPlayer player) {
        BlockPos bed = findNearestBed(player, 8);
        if (bed == null) {
            clearMovement(minecraft);
            return;
        }
        aimAt(player, bed.getX() + 0.5, bed.getY() + 0.5, bed.getZ() + 0.5);
        minecraft.options.keyUse.setDown(true);
    }

    private static BlockPos findNearestBed(LocalPlayer player, int radius) {
        BlockPos origin = player.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius), origin.offset(radius, radius, radius))) {
            if (player.level().getBlockState(pos).getBlock() instanceof BedBlock) {
                double distance = player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = pos.immutable();
                }
            }
        }
        return best;
    }

    private static void select(LocalPlayer player, int slot) {
        if (slot >= 0 && slot < 9) player.getInventory().setSelectedSlot(slot);
    }

    private static void aimAt(LocalPlayer player, double x, double y, double z) {
        double dx = x - player.getX();
        double dy = y - (player.getY() + player.getEyeHeight());
        double dz = z - player.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontal)));
        look(player, yaw, pitch);
    }
}
