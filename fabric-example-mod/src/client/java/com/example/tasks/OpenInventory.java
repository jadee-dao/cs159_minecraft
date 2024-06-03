package com.example.tasks;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;

public class OpenInventory implements ITask {

    @Override
    public void execute() {
        // Open user's inventory
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> client.setScreen(new InventoryScreen(client.player)));
    }

    @Override
    public TaskState getState() {
        return TaskState.terminatedSuccess();
    }

    @Override
    public double getFinishCheckInterval() {
        return 0;
    }

    @Override
    public void cleanup() {

    }
}
