package com.example.tasks;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import net.minecraft.registry.Registries;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/* Crafting, assuming the relevant screen is already open. */
public class Craft implements ITask {
    TaskState _failedState;
    Item _item;
    int _quantity;

    public Craft(String[] args) {
        _quantity = Integer.parseInt(args[0]);
        _item = Registries.ITEM.get(new Identifier(args[1]));
    }

    public static int getNumberOfRecipeForOutput(Recipe<?> recipe, int outputQuantity) {
        MinecraftClient client = MinecraftClient.getInstance();
        DynamicRegistryManager registryManager = client.player.world.getRegistryManager();
        return (int) Math.ceil((double) outputQuantity / recipe.getOutput(registryManager).getCount());
    }

    public static boolean playerCanMakeRecipe(Recipe<?> recipe, int n) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;

        // Create a copy of the player's inventory to track ingredient usage
        ItemStack[] inventoryCopy = new ItemStack[player.getInventory().size()];
        for (int i = 0; i < player.getInventory().size(); i++) {
            inventoryCopy[i] = player.getInventory().getStack(i).copy();
        }

        // Check player has the requisite items for n recipes
        for (Ingredient ingredient : recipe.getIngredients()) {
            int foundAmount = 0;
            if(ingredient.isEmpty()) {
                continue;
            }

            for (int i = 0; i < inventoryCopy.length; i++) {
                ItemStack stack = inventoryCopy[i];
                if (ingredient.test(stack)) {
                    int stackCount = stack.getCount();
                    if (stackCount >= n - foundAmount) {
                        stack.decrement(n - foundAmount);
                        foundAmount = n;
                        break;
                    } else {
                        foundAmount += stackCount;
                        stack.setCount(0);
                    }
                }
            }

            if (foundAmount < n) {
                System.out.println("Missing one of" + Arrays.toString(ingredient.getMatchingStacks()) + " for recipe " + recipe.getId());
                return false;
            }
        }

        return true;
    }

    public static List<Recipe<?>> getCraftingRecipes(Item item) {
        // First, get the crafting recipe
        MinecraftClient client = MinecraftClient.getInstance();
        RecipeManager recipeManager = client.player.world.getRecipeManager();
        DynamicRegistryManager registryManager = client.player.world.getRegistryManager();
        Iterable<Recipe<?>> recipes = recipeManager.values();

        List<Recipe<?>> validRecipes = new ArrayList<>();
        // Iterate through all recipes to find one that matches the given Item
        for (Recipe<?> recipe : recipes) {
            // Check if the recipe is a crafting recipe
            if (recipe.getType() == RecipeType.CRAFTING) {
                // Check if the output item matches the given item
                if (recipe.getOutput(registryManager).getItem() == item) {
                    validRecipes.add(recipe);
                }
            }
        }

        return validRecipes;
    }

    public static List<ITask> getCraftingTasks(String[] args) {
        int quantity = Integer.parseInt(args[0]);
        String itemName = args[1];

        List<ITask> tasks = new ArrayList<ITask>();
        // Get the crafting recipe for the given item
        Item item = Registries.ITEM.get(new Identifier(itemName));
        if(item.getName().getString().equals("Air")) {
            tasks.add(new AutoFailTask("Invalid item name " + itemName));
            return tasks;
        }
        List<Recipe<?>> recipes = getCraftingRecipes(item);
        if (recipes.isEmpty()) {
            tasks.add(new AutoFailTask("No crafting recipe exists for item " + itemName));
            return tasks;
        }

        if(recipes.getFirst().fits(2,2)) {
            tasks.add(new OpenInventory());
        } else {
            tasks.add(new FindOrPlaceBlock("crafting_table"));
            tasks.add(new InteractBlock("crafting_table"));
        }

        tasks.add(new Craft(args));

        return tasks;
    }

    public void craft(Item item, int quantity) {
        MinecraftClient client = MinecraftClient.getInstance();
        DynamicRegistryManager registryManager = client.player.world.getRegistryManager();

        List<Recipe<?>> recipes = getCraftingRecipes(item);

        Optional<Recipe<?>> recipeOptional = recipes.stream().filter(r -> playerCanMakeRecipe(r, getNumberOfRecipeForOutput(r, quantity))).findFirst();
        if(recipeOptional.isEmpty()) {
            _failedState = TaskState.terminatedFailure("Player does not have the required items");
            return;
        }
        Recipe<?> recipe = recipeOptional.get();

        if (recipe.getType() == RecipeType.CRAFTING) {
            CraftingRecipe craftingRecipe = (CraftingRecipe) recipe;
            // Check if the output item matches the given item
            if (recipe.getOutput(registryManager).getItem() == item) {
                System.out.println("Crafting " + item.getName().getString());
                int n = getNumberOfRecipeForOutput(craftingRecipe, quantity);
                craftUsingCraftingTable(client.player, craftingRecipe, n);
                return;
            }
        }

        _failedState = TaskState.terminatedFailure("Could not craft item");
    }


    public boolean craftUsingCraftingTable(PlayerEntity player, CraftingRecipe recipe, int n) {
        // Check if the block at the given position is a crafting table
        MinecraftClient client = MinecraftClient.getInstance();
        DynamicRegistryManager registryManager = client.player.world.getRegistryManager();

        // Check player has the requisite items
        if (!playerCanMakeRecipe(recipe, n)) {
            _failedState = TaskState.terminatedFailure("Player does not have the required items");
            return false;
        }

        if (!(player.currentScreenHandler instanceof CraftingScreenHandler || client.currentScreen instanceof InventoryScreen)) {
            _failedState = TaskState.terminatedFailure("Player is not at a crafting area");
            return false;
        }
        for (int i = 0; i < n; i++) {
            MinecraftClient.getInstance().execute(() -> {
                client.interactionManager.clickRecipe(player.currentScreenHandler.syncId, recipe, false);
                windowClick(player.currentScreenHandler, 0, SlotActionType.QUICK_MOVE, 1);
            });
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            //
        }
        return true; // Crafting succeeded
    }

    private static void windowClick(ScreenHandler container, int slot, SlotActionType action, int clickData) {
        MinecraftClient client = MinecraftClient.getInstance();

        assert client.interactionManager != null;
        client.interactionManager.clickSlot(container.syncId, slot, clickData, action, client.player);
    }


    @Override
    public void execute() {
        craft(_item, _quantity);
    }

    @Override
    public TaskState getState() {
        if(_failedState != null ) {
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
        System.out.println(MinecraftClient.getInstance().player.getInventory().count(Blocks.CRAFTING_TABLE.asItem()));
        MinecraftClient client = MinecraftClient.getInstance();
        MinecraftClient.getInstance().execute(() -> {
            client.player.closeHandledScreen();
        });
    }
}
