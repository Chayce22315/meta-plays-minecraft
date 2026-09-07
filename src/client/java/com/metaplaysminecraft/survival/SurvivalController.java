package com.metaplaysminecraft.survival;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

public final class SurvivalController {
    private SurvivalController() {}

    public static boolean shouldEmergencyEat(LocalPlayer player) {
        if (player.getFoodData().getFoodLevel() > 8 || player.isUsingItem()) return false;
        ItemStack held = player.getMainHandItem();
        return !held.isEmpty()
                && held.has(DataComponents.FOOD)
                && held.has(DataComponents.CONSUMABLE);
    }

    public static void tick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.gui.screen() != null) return;

        if (shouldEmergencyEat(player)) {
            minecraft.options.keyUse.setDown(true);
        }
    }
}
