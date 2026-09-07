package com.metaplaysminecraft.ai;

import com.metaplaysminecraft.actions.AiActionExecutor;
import net.minecraft.client.Minecraft;

/** Detects when navigation has stopped making progress and nudges the agent free. */
public final class StuckRecovery {
    private static final double MIN_PROGRESS_SQUARED = 0.0025D;
    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final int MAX_STUCK_CHECKS = 3;

    private double lastX;
    private double lastY;
    private double lastZ;
    private int ticks;
    private int stuckChecks;

    public void tick(Minecraft minecraft, AiAction currentAction, AiMemory memory) {
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        ticks++;
        if (ticks < CHECK_INTERVAL_TICKS) {
            return;
        }
        ticks = 0;

        double x = minecraft.player.getX();
        double y = minecraft.player.getY();
        double z = minecraft.player.getZ();
        double dx = x - lastX;
        double dy = y - lastY;
        double dz = z - lastZ;
        double progressSquared = dx * dx + dy * dy + dz * dz;

        boolean expectsMovement = currentAction != null &&
                ("move".equals(currentAction.type()) || "jump".equals(currentAction.type()));

        if (expectsMovement && progressSquared < MIN_PROGRESS_SQUARED) {
            stuckChecks++;
            memory.remember("navigation made little progress (check " + stuckChecks + ")");
            recover(minecraft, memory);
        } else {
            stuckChecks = 0;
        }

        lastX = x;
        lastY = y;
        lastZ = z;
    }

    private void recover(Minecraft minecraft, AiMemory memory) {
        if (stuckChecks > MAX_STUCK_CHECKS) {
            memory.remember("stuck too long; stopping current movement");
            AiActionExecutor.clearMovement(minecraft);
            stuckChecks = 0;
            return;
        }

        // Briefly release and re-apply forward movement with a jump. This is
        // deliberately small and uses normal client input instead of teleporting.
        AiActionExecutor.clearMovement(minecraft);
        AiAction recovery = new AiAction(
                "move", "", 0, 0, 1.0F, 0.0F, true, true, false,
                -1, 0, 0, 0, 8, ""
        );
        AiActionExecutor.execute(minecraft, recovery);
    }
}
