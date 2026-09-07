package com.metaplaysminecraft.ai;

import com.metaplaysminecraft.actions.AiActionExecutor;
import com.metaplaysminecraft.perception.PlayerPerception;
import net.minecraft.client.Minecraft;

public final class AiPlayerController {
    private static final int THINK_INTERVAL_TICKS = 10;

    private final LocalAiClient aiClient = new LocalAiClient();
    private int ticksUntilThink;
    private boolean thinking;
    private AiAction currentAction = AiAction.noop();

    public void tick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            AiActionExecutor.execute(minecraft, AiAction.noop());
            return;
        }

        AiActionExecutor.execute(minecraft, currentAction);

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
