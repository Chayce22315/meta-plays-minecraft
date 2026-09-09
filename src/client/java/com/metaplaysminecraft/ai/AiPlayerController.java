package com.metaplaysminecraft.ai;

import com.metaplaysminecraft.actions.AiActionExecutor;
import com.metaplaysminecraft.actions.CraftingController;
import com.metaplaysminecraft.config.MetaAiConfig;
import com.metaplaysminecraft.navigation.NavigationController;
import com.metaplaysminecraft.perception.PlayerPerception;
import com.metaplaysminecraft.survival.SurvivalController;
import net.minecraft.client.Minecraft;

import java.util.concurrent.atomic.AtomicReference;

public final class AiPlayerController {
    private final LocalAiClient aiClient = new LocalAiClient();
    private final AiMemory memory = new AiMemory();
    private final StuckRecovery stuckRecovery = new StuckRecovery();
    private final AtomicReference<AiDecision> pendingDecision = new AtomicReference<>();
    private AiAction currentAction = AiAction.noop();
    private AiGoal currentGoal = AiGoal.idle();
    private int ticksUntilThink;
    private int actionTicksLeft;
    private boolean thinking;

    public void tick(Minecraft minecraft) {
        MetaAiConfig config = MetaAiConfig.get();
        if (!config.aiEnabled || minecraft.player == null || minecraft.level == null) {
            AiActionExecutor.clearMovement(minecraft);
            return;
        }

        if (config.autonomousCrafting) CraftingController.tick(minecraft);
        if (config.autonomousSurvival) SurvivalController.tick(minecraft);

        if (CraftingController.isBusy()) return;

        AiDecision next = pendingDecision.getAndSet(null);
        if (next != null) {
            currentGoal = next.goal() == null ? currentGoal : next.goal().normalized();
            if (next.action() != null && next.action().isKnown()) {
                currentAction = next.action();
                actionTicksLeft = currentAction.safeDuration();
                memory.rememberAction(currentAction);
            }
            memory.remember("goal: " + currentGoal.type() + " - " + currentGoal.description() +
                    (currentGoal.done() ? " [complete]" : ""));
        }

        execute(minecraft);
        stuckRecovery.tick(minecraft, currentAction, memory);

        actionTicksLeft--;
        if (actionTicksLeft <= 0) currentAction = AiAction.noop();

        if (--ticksUntilThink <= 0 && !thinking && minecraft.gui.screen() == null) {
            ticksUntilThink = config.decisionIntervalTicks;
            thinking = true;
            String context = PlayerPerception.snapshot(minecraft) + "\n" +
                    "Current high-level goal: " + currentGoal.type() + " - " + currentGoal.description() +
                    " (done=" + currentGoal.done() + ")\n" +
                    (config.memoryEnabled ? memory.context() : "");
            aiClient.decide(context, currentGoal).whenComplete((decision, error) -> {
                if (error != null || decision == null) {
                    memory.remember("ai decision failed; holding goal and waiting briefly");
                    pendingDecision.set(AiDecision.noop(currentGoal));
                } else {
                    pendingDecision.set(decision);
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
                    NavigationController.moveToward(minecraft,
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

    public AiGoal currentGoal() {
        return currentGoal;
    }

    public AiMemory memory() {
        return memory;
    }
}
