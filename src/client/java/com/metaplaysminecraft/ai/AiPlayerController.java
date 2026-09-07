package com.metaplaysminecraft.ai;

import com.metaplaysminecraft.actions.AiActionExecutor;
import com.metaplaysminecraft.actions.CraftingController;
import com.metaplaysminecraft.navigation.NavigationController;
import com.metaplaysminecraft.perception.PlayerPerception;
import com.metaplaysminecraft.survival.SurvivalController;
import net.minecraft.client.Minecraft;

import java.util.concurrent.atomic.AtomicReference;

public final class AiPlayerController {
    private static final int THINK_INTERVAL_TICKS = 8;

    private final LocalAiClient aiClient = new LocalAiClient();
    private final AiMemory memory = new AiMemory();
    private final StuckRecovery stuckRecovery = new StuckRecovery();
    private final AtomicReference<AiAction> pendingAction = new AtomicReference<>(AiAction.noop());
    private AiAction currentAction = AiAction.noop();
    private int ticksUntilThink;
    private int actionTicksLeft;
    private boolean thinking;

    public void tick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            AiActionExecutor.clearMovement(minecraft);
            return;
        }

        CraftingController.tick(minecraft);
        SurvivalController.tick(minecraft);

        if (CraftingController.isBusy()) {
            return;
        }

        AiAction next = pendingAction.getAndSet(null);
        if (next != null && next.isKnown()) {
            currentAction = next;
            actionTicksLeft = currentAction.safeDuration();
            memory.rememberAction(currentAction);
        }

        execute(minecraft);
        stuckRecovery.tick(minecraft, currentAction, memory);

        actionTicksLeft--;
        if (actionTicksLeft <= 0) {
            currentAction = AiAction.noop();
        }

        if (--ticksUntilThink <= 0 && !thinking && minecraft.gui.screen() == null) {
            ticksUntilThink = THINK_INTERVAL_TICKS;
            thinking = true;
            String context = PlayerPerception.snapshot(minecraft) + "\n" + memory.context();
            aiClient.decide(context)
                    .whenComplete((action, error) -> {
                        if (error != null || action == null) {
                            memory.remember("ai decision failed; waiting briefly");
                            pendingAction.set(AiAction.noop());
                        } else {
                            pendingAction.set(action);
                        }
                        thinking = false;
                    });
        }
    }

    private void execute(Minecraft minecraft) {
        AiAction action = currentAction;
        if ("move".equals(action.type()) && action.target().startsWith("coords:")) {
            String[] parts = action.target().substring("coords:".length()).split(",");
            if (parts.length == 3) {
                try {
                    NavigationController.moveToward(
                            minecraft,
                            Double.parseDouble(parts[0]),
                            Double.parseDouble(parts[1]),
                            Double.parseDouble(parts[2]),
                            action.sprint());
                    return;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        AiActionExecutor.execute(minecraft, action);
    }

    public AiAction currentAction() {
        return currentAction;
    }

    public AiMemory memory() {
        return memory;
    }
}
