package com.example.tasks;

import com.example.Utility;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/* Interact with a block type, assuming it's within reach */
public class InteractBlock implements ITask {

    private Block _block;
    private TaskState _failedState;

    public InteractBlock(String blockName) {
        _block = Block.getBlockFromItem(Registries.ITEM.get(new Identifier(blockName)));
        if (_block.equals(Blocks.AIR)) {
            _failedState = TaskState.terminatedFailure("Invalid block name");
        }
    }

    @Override
    public void execute() {
        MinecraftClient client = MinecraftClient.getInstance();
        BlockPos blockPos = Utility.findBlockNearby(5, client.player, _block);
        if (blockPos == null) {
            _failedState = TaskState.terminatedFailure("Could not find block of type " + _block.toString() + " nearby.");
            return;
        }
        Vec3d blockVec = new Vec3d(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        BlockHitResult table = new BlockHitResult(blockVec, Direction.UP, blockPos, false);
        MinecraftClient.getInstance().execute(() -> {
            client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, table);
        });
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
        return;
    }
}
