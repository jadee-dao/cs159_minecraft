package com.example.tasks;

import baritone.api.BaritoneAPI;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import com.example.Utility;

/* Get within reach of a block type */
public class GotoBlock implements ITask {

    Block _block;
    TaskState _failedState;
    long _startTime;
    double _timeout;

    public GotoBlock(String blockName) {
        Item item = Registries.ITEM.get(new Identifier(blockName));
        Block block = Block.getBlockFromItem(item);
        if (block.equals(Blocks.AIR)) {
            _failedState = TaskState.terminatedFailure("Invalid block name");
            return;
        }
        _block = block;
        _timeout = 200000;
    }
    public GotoBlock(String blockName, double timeout) {
        Item item = Registries.ITEM.get(new Identifier(blockName));
        Block block = Block.getBlockFromItem(item);
        if (block.equals(Blocks.AIR)) {
            _failedState = TaskState.terminatedFailure("Invalid block name");
            return;
        }
        _block = block;
        _timeout = timeout;
    }

    @Override
    public void execute() {
        MinecraftClient.getInstance().execute(() -> {
            BaritoneAPI.getProvider().getPrimaryBaritone().getGetToBlockProcess().getToBlock(_block);
        });
        _startTime = System.currentTimeMillis();
    }

    @Override
    public TaskState getState() {
        if( _failedState != null ) {
            return _failedState;
        }

        int range = 3;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (Utility.findBlockNearby(range, player, _block) != null) return TaskState.terminatedSuccess();

        if (System.currentTimeMillis() - _startTime > 3000 && !BaritoneAPI.getProvider().getPrimaryBaritone().getGetToBlockProcess().isActive()) {
            return TaskState.terminatedFailure("Could not find block of type " + _block.toString() + " nearby.");
        }
        if (System.currentTimeMillis() - _startTime > _timeout) {
            return TaskState.terminatedFailure("Took too long to find block of type " + _block.toString() + " nearby.");
        }

        return TaskState.running();
    }

    @Override
    public double getFinishCheckInterval() {
        return 1;
    }

    @Override
    public void cleanup() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        if (BaritoneAPI.getProvider().getPrimaryBaritone().getGetToBlockProcess().isActive()) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getGetToBlockProcess().onLostControl();
        }
    }
}
