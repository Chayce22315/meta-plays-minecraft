package com.metaplaysminecraft.actions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

public final class CraftingController {
    private static Job job;

    private CraftingController() {}

    public static void start(Minecraft minecraft, LocalPlayer player, String recipe) {
        String normalized = recipe == null ? "" : recipe.toLowerCase(Locale.ROOT).trim();
        RecipePlan plan = RecipePlan.byName(normalized);
        if (plan == null) {
            return;
        }
        job = new Job(plan, 0);
        if (!(minecraft.screen != null)) {
            minecraft.options.keyUse.setDown(true);
        }
    }

    public static boolean isBusy() {
        return job != null;
    }

    public static void tick(Minecraft minecraft) {
        if (job == null || minecraft.player == null || minecraft.gameMode == null) return;
        LocalPlayer player = minecraft.player;
        AbstractContainerMenu menu = player.containerMenu;
        if (!(menu instanceof InventoryMenu) && !(menu instanceof CraftingMenu)) return;

        RecipePlan plan = job.plan;
        int[] grid = menu instanceof CraftingMenu ? plan.grid3x3 : plan.grid2x2;
        if (grid == null) {
            job = null;
            return;
        }

        if (job.step < grid.length) {
            int inventoryIndex = findInventoryItem(player, plan.ingredient);
            if (inventoryIndex >= 0) {
                int sourceSlot = inventoryToMenuSlot(inventoryIndex);
                int destinationSlot = grid[job.step];
                click(minecraft, menu, sourceSlot, 0, ContainerInput.PICKUP, player);
                click(minecraft, menu, destinationSlot, 0, ContainerInput.PICKUP, player);
                job = new Job(plan, job.step + 1);
                return;
            }
            job = null;
            return;
        }

        click(minecraft, menu, 0, 0, ContainerInput.QUICK_MOVE, player);
        job = null;
    }

    private static int findInventoryItem(LocalPlayer player, String wanted) {
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (!stack.isEmpty() && stack.getItem().toString().toLowerCase(Locale.ROOT).contains(wanted)) {
                return i;
            }
        }
        return -1;
    }

    private static int inventoryToMenuSlot(int inventoryIndex) {
        return inventoryIndex < 9 ? 37 + inventoryIndex : inventoryIndex + 1;
    }

    private static void click(Minecraft minecraft, AbstractContainerMenu menu, int slot, int button, ContainerInput input, LocalPlayer player) {
        minecraft.gameMode.handleContainerInput(menu.containerId, slot, button, input, player);
    }

    private record Job(RecipePlan plan, int step) {}

    private record RecipePlan(String ingredient, int[] grid2x2, int[] grid3x3) {
        static RecipePlan byName(String name) {
            return switch (name) {
                case "planks", "oak_planks", "birch_planks", "spruce_planks" -> new RecipePlan("_log", new int[]{1}, null);
                case "sticks" -> new RecipePlan("planks", new int[]{1, 2}, null);
                case "crafting_table", "crafting table" -> new RecipePlan("planks", new int[]{1, 2, 3, 4}, null);
                case "chest" -> new RecipePlan("planks", null, new int[]{1, 2, 3, 4, 6, 7, 8, 9});
                case "furnace" -> new RecipePlan("cobblestone", null, new int[]{1, 2, 3, 4, 6, 7, 8, 9});
                case "torch", "torches" -> new RecipePlan("coal", null, new int[]{1, 2});
                default -> null;
            };
        }
    }
}
