package com.metaplaysminecraft.ai;

import com.metaplaysminecraft.actions.AiActionExecutor;
import com.metaplaysminecraft.perception.PlayerPerception;
import net.minecraft.client.Minecraft;

public final class AiPlayerController {
    private static final int THINK_INTERVAL_TICKS = 10;

    private final LocalAiClient aiClient;
    private int ticksUntilThink;
    private boolean thinking;
    private AiAction currentAction = AiAction.noop();

    public AiPlayerController() {
        this.aiClient = new LocalAiClient();
        this.ticksUntilThink = 0;
    }

    public void tick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            currentAction = AiAction.noop();
            AiActionExecutor.clearTransientKeys(minecraft);
            return;
        }

        AiActionExecutor.execute(minecraft, currentAction);
        AiActionExecutor.clearTransientKeys(minecraft);

        if (--ticksUntilThink <= 0 && !thinking) {
            ticksUntilThink = THINK_INTERVAL_TICKS;
            thinking = true;
            String perception = PlayerPerception.snapshot(minecraft);
            aiClient.decide(perception).thenAccept(action -> {
                currentAction = action;
                thinking = false;
            });
        }
    }

    public AiAction currentAction() {
        return currentAction;
    }
}
