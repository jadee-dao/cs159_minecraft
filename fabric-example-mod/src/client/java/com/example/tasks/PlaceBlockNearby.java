package com.example.tasks;

import baritone.api.BaritoneAPI;
import baritone.api.schematic.AbstractSchematic;
import baritone.api.schematic.ISchematic;
import com.example.Utility;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class PlaceBlockNearby implements ITask {
    String _block;
    long _startTime;
    TaskState _failedState;

    public PlaceBlockNearby(String block) {
        _block = block;
    }

    private static BlockPos getNearbyBlockToPlace() {
        int range = 7;
        BlockPos best = null;
        double smallestScore = Double.POSITIVE_INFINITY;

        // Get the closest reachable block to player which can be placed
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= 0; y++) {
                for (int z = -range; z <= range; z++) {
                    if (x == 0 && z == 0) continue;
                    BlockPos pos = player.getBlockPos().add(x, y, z);
                    // Check if the block is air, and the block underneath is solid
                    if (player.world.getBlockState(pos).isAir() && player.world.getBlockState(pos.down()).isSolidBlock(player.world, pos.down())) {
                        double score = player.getBlockPos().getSquaredDistance(pos);
                        if (score != 0 && score < smallestScore) {
                            best = pos;
                            smallestScore = score;
                        }
                    }
                }
            }
        }
        return best;
    }

    public boolean placeNearby(Block block) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        // Check player has block in inventory
        if(player.getInventory().count(block.asItem()) == 0) {
            _failedState = TaskState.terminatedFailure("Player does not have block in inventory");
            return false;
        }

        // Get placement location
        BlockPos placePos = getNearbyBlockToPlace();
        if (placePos == null) {
            _failedState = TaskState.terminatedFailure("No suitable place found to place the block");
            return false;
        }

        ISchematic schematic = new PlaceStructureSchematic(block);
        BaritoneAPI.getProvider().getPrimaryBaritone().getBuilderProcess().build("structure", schematic, placePos);
        return true;
    }

    @Override
    public void execute() {
        _startTime = System.currentTimeMillis();
        Item item = Registries.ITEM.get(new Identifier(_block));
        Block block = Block.getBlockFromItem(item);
        if (block.equals(Blocks.AIR)) {
            _failedState = TaskState.terminatedFailure("Invalid block name");
            return;
        }
        placeNearby(block);
    }

    @Override
    public TaskState getState() {
        // Check if failed from outset
        if( _failedState != null ) {
            return _failedState;
        }

        // Check for block in question within 10 blocks
        int range = 10;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        Item item = Registries.ITEM.get(new Identifier(_block));
        Block block = Block.getBlockFromItem(item);

        if (Utility.findBlockNearby(range, player, block) != null) return TaskState.terminatedSuccess();

        if(System.currentTimeMillis() - _startTime > 5000) {
            return TaskState.terminatedFailure("Timed out");
        }

        return TaskState.running();
    }

    @Override
    public double getFinishCheckInterval() {
        return 0.1;
    }

    @Override
    public void cleanup() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        if (BaritoneAPI.getProvider().getPrimaryBaritone().getBuilderProcess().isActive()) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getBuilderProcess().onLostControl();
        }
    }

    private static class PlaceStructureSchematic extends AbstractSchematic {

        Block _toPlace;
        public PlaceStructureSchematic(Block toPlace) {
            super(1, 1, 1);
            _toPlace = toPlace;
        }

        @Override
        public BlockState desiredState(int x, int y, int z, BlockState blockState, List<BlockState> available) {
            if (x == 0 && y == 0 && z == 0) {
                // Place!!
                for (BlockState possible : available) {
                    if (possible == null) continue;
                    if (_toPlace.equals(possible.getBlock())) {
                        return possible;
                    }
                }
                // Not possible
                return blockState;
            }
            // Don't care.
            return blockState;
        }
    }
}
