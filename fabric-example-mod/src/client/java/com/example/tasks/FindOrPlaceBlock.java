package com.example.tasks;

import baritone.api.BaritoneAPI;
import baritone.api.utils.BlockOptionalMetaLookup;
import com.example.Utility;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.recipe.Recipe;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.example.tasks.Craft.*;

public class FindOrPlaceBlock implements ITask {

    String block;
    ITask _internalTask;

    public FindOrPlaceBlock(String block) {
        this.block = block;
    }

    @Override
    public void execute() {
        MinecraftClient client = MinecraftClient.getInstance();
        Item item = Registries.ITEM.get(new Identifier(block));
        Block blockType = Block.getBlockFromItem(item);
        if (Utility.findBlockNearby(3, client.player, blockType) == null) {

            // If inventory contains or can make, place. Otherwise, try to find.
            if (client.player.getInventory().count(item) != 0) {
                _internalTask = new PlaceBlockNearby(block);
                _internalTask.execute();
                return;
            } else if (!BaritoneAPI.getProvider().getWorldScanner().scanChunkRadius(BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext(), new BlockOptionalMetaLookup(blockType), 1, 10, 32).isEmpty()){
                _internalTask = new GotoBlock(block, 20000);
                _internalTask.execute();
                return;
            } else {
                _internalTask = new AutoFailTask("Could not find or place block " + block);
                return;
            }
        }
        _internalTask = new AutoSucceedTask();
    }

    @Override
    public TaskState getState() {
        return _internalTask.getState();
    }

    @Override
    public double getFinishCheckInterval() {
        return _internalTask.getFinishCheckInterval();
    }

    @Override
    public void cleanup() {
        _internalTask.cleanup();
    }
}