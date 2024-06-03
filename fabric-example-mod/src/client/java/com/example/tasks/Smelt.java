package com.example.tasks;

import baritone.api.BaritoneAPI;
import com.example.Utility;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.*;
import net.minecraft.registry.Registries;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/* Smelting task, assuming the relevant screen is already open. */
public class Smelt implements ITask {
    private TaskState _failedState;
    private final Item _item;
    private final int _quantity;

    public Smelt(String[] args) {
        _quantity = Integer.parseInt(args[0]);
        _item = Registries.ITEM.get(new Identifier(args[1]));
    }

    public static List<ITask> getSmeltingTasks(String[] args) {
        int quantity = Integer.parseInt(args[0]);
        String itemName = args[1];

        List<ITask> tasks = new ArrayList<ITask>();
        // Get the crafting recipe for the given item
        Item item = Registries.ITEM.get(new Identifier(itemName));
        if(item.getName().getString().equals("Air")) {
            tasks.add(new AutoFailTask("Invalid item name " + itemName));
            return tasks;
        }
        List<Recipe<?>> recipes = getSmeltingRecipes(item);
        if (recipes.isEmpty()) {
            tasks.add(new AutoFailTask("No smelting recipe exists for item " + itemName));
            return tasks;
        }

        tasks.add(new FindOrPlaceBlock("furnace"));
        tasks.add(new InteractBlock("furnace"));

        tasks.add(new Smelt(args));

        return tasks;
    }

    public static List<Recipe<?>> getSmeltingRecipes(Item item) {
        MinecraftClient client = MinecraftClient.getInstance();
        RecipeManager recipeManager = client.player.world.getRecipeManager();
        Iterable<Recipe<?>> recipes = recipeManager.values();

        List<Recipe<?>> validRecipes = new ArrayList<>();
        for (Recipe<?> recipe : recipes) {
            if (recipe.getType() == RecipeType.SMELTING && recipe.getOutput(client.player.world.getRegistryManager()).getItem() == item) {
                validRecipes.add(recipe);
            }
        }

        return validRecipes;
    }

    public static boolean playerCanMakeRecipe(SmeltingRecipe recipe, int n) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        ItemStack[] inventoryCopy = new ItemStack[player.getInventory().size()];
        for (int i = 0; i < player.getInventory().size(); i++) {
            inventoryCopy[i] = player.getInventory().getStack(i).copy();
        }

        int foundAmount = 0;
        for (ItemStack stack : inventoryCopy) {
            if (recipe.getIngredients().get(0).test(stack)) {
                foundAmount += stack.getCount();
                if (foundAmount >= n) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean playerHasEnoughFuel(int n) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        int requiredCoal = (int) Math.ceil(n / 8.0); // Each coal can smelt 8 items
        int foundCoal = 0;

        for (ItemStack stack : player.getInventory().main) {
            if (stack.getItem() == Items.COAL) {
                foundCoal += stack.getCount();
                if (foundCoal >= requiredCoal) {
                    return true;
                }
            }
        }

        return false;
    }

    public void smelt(Item item, int quantity) {
        MinecraftClient client = MinecraftClient.getInstance();

        List<Recipe<?>> recipes = getSmeltingRecipes(item);
        Optional<Recipe<?>> recipeOptional = recipes.stream().filter(r -> playerCanMakeRecipe((SmeltingRecipe) r, quantity)).findFirst();
        if (recipeOptional.isEmpty()) {
            _failedState = TaskState.terminatedFailure("Player does not have the required items");
            return;
        }
        SmeltingRecipe recipe = (SmeltingRecipe) recipeOptional.get();

        if (!playerHasEnoughFuel(quantity)) {
            _failedState = TaskState.terminatedFailure("Player does not have enough coal");
            return;
        }

        System.out.println("Smelting " + item.getName().getString());
        int n = getNumberOfRecipeForOutput(recipe, quantity);
        smeltUsingFurnace(client.player, recipe, n);
    }

    public static int getNumberOfRecipeForOutput(Recipe<?> recipe, int outputQuantity) {
        MinecraftClient client = MinecraftClient.getInstance();
        return (int) Math.ceil((double) outputQuantity / recipe.getOutput(client.player.world.getRegistryManager()).getCount());
    }

    public boolean smeltUsingFurnace(PlayerEntity player, SmeltingRecipe recipe, int n) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (!playerCanMakeRecipe(recipe, n)) {
            _failedState = TaskState.terminatedFailure("Player does not have the required items");
            return false;
        }

        if (!(player.currentScreenHandler instanceof FurnaceScreenHandler)) {
            _failedState = TaskState.terminatedFailure("Player is not at a furnace");
            return false;
        }

        addFuelToFurnace(player, n);

        System.out.println("Smelting " + n + " of " + recipe.getOutput(client.player.world.getRegistryManager()).getName().getString());
        for (int i = 0; i < n; i++) {
            MinecraftClient.getInstance().execute(() -> {
                client.interactionManager.clickRecipe(player.currentScreenHandler.syncId, recipe, true);
            });
        }

        waitForSmeltingToComplete(player);
        MinecraftClient.getInstance().execute(() -> {
            windowClick(player.currentScreenHandler, 2, SlotActionType.QUICK_MOVE, 0);
        });

        return true;
    }

    private void addFuelToFurnace(PlayerEntity player, int n) {
        MinecraftClient client = MinecraftClient.getInstance();
        int requiredCoal = (int) Math.ceil(n / 8.0); // Each coal can smelt 8 items
        int placedCoal = 0;

        // Iterate through the player's inventory to find and place coal
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == Items.COAL) {
                int toPlace = Math.min(stack.getCount(), requiredCoal - placedCoal);
                for (int j = 0; j < toPlace; j++) {
                    int furnaceSlot = inventoryToFurnaceSlot(i);
                    System.out.println("Placing coal in furnace slot " + furnaceSlot);
                    client.execute(() -> {
                        windowClick(player.currentScreenHandler, furnaceSlot, SlotActionType.QUICK_MOVE, 0);
                    });
                    placedCoal++;
                    if (placedCoal >= requiredCoal) {
                        return;
                    }
                }
            }
        }

        if (placedCoal < requiredCoal) {
            System.out.println("Not enough coal in inventory to place in furnace");
            _failedState = TaskState.terminatedFailure("Not enough coal in inventory to place in furnace");
        }
    }

    private int inventoryToFurnaceSlot(int i) {
        System.out.println(i);
        if (i < 9) {
            return (i + 3) + 27;
        } else if (i < 36) {
            return (i + 3) - 9;
        }
        return i;
    }

    private void waitForSmeltingToComplete(PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        ScreenHandler handler = player.currentScreenHandler;

        while (true) {

            ItemStack stack = handler.getSlot(2).getStack();
            if (stack.getItem() == _item && stack.getCount() == _quantity) {
                break;
            }

            try {
                Thread.sleep(100); // Check every 100ms
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void windowClick(ScreenHandler container, int slot, SlotActionType action, int clickData) {
        MinecraftClient client = MinecraftClient.getInstance();

        assert client.interactionManager != null;
        client.interactionManager.clickSlot(container.syncId, slot, clickData, action, client.player);
    }

    @Override
    public void execute() {
        smelt(_item, _quantity);
    }

    @Override
    public TaskState getState() {
        if (_failedState != null) {
            return _failedState;
        }
        return TaskState.terminatedSuccess();
    }

    @Override
    public double getFinishCheckInterval() {
        return 0;
    }

    @Override
    public void cleanup() {
        MinecraftClient client = MinecraftClient.getInstance();
        MinecraftClient.getInstance().execute(() -> {
            client.player.closeHandledScreen();
        });
    }
}
