package com.example.tasks;

import baritone.api.BaritoneAPI;
import baritone.api.utils.BlockOptionalMeta;
import baritone.api.utils.BlockOptionalMetaLookup;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.registry.tag.BlockTags;

import java.util.ArrayList;
import java.util.List;

public class Mine implements ITask {

    private int num_items;
    private List<BlockOptionalMeta> blocks_to_mine;
    TaskState _failedState;
    long _startTime;

    public Mine(String[] args) {
        num_items = Integer.parseInt(args[0]);
        blocks_to_mine = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            try {
                blocks_to_mine.add(new BlockOptionalMeta(args[i]));
                // Get deepslate version, if it exists
                if (args[i].contains("ore")) {
                    blocks_to_mine.add(new BlockOptionalMeta("deepslate_" + args[i]));
                }
            } catch (Exception e) {
                _failedState = TaskState.terminatedFailure("Invalid block name: " + args[i]);
                return;
            }
        }
    }

    private static boolean canMineWithoutTool(BlockState blockState) {
        // Check if the block can be mined without a tool using the block's tags
        return !blockState.isToolRequired() && (blockState.isIn(BlockTags.SHOVEL_MINEABLE) ||
                blockState.isIn(BlockTags.PICKAXE_MINEABLE) ||
                blockState.isIn(BlockTags.AXE_MINEABLE) ||
                blockState.isIn(BlockTags.HOE_MINEABLE));
    }

    public static boolean canPlayerMineBlock(PlayerEntity player, BlockState blockState) {
        if (canMineWithoutTool(blockState)) {
            System.out.println("Can be mined with hand");
            return true;
        }

        // Iterate through the player's inventory
        for (ItemStack itemStack : player.getInventory().main) {
            // Check if the item is a tool
            if (itemStack.getItem() instanceof ToolItem) {
                ToolItem toolItem = (ToolItem) itemStack.getItem();

                // Check if the tool can harvest the block
                if (toolItem.isSuitableFor(blockState)) {
                    return true;
                }
            }
        }

        // If no suitable tool is found, return false
        return false;
    }

    @Override
    public void execute() {
        System.out.println("Mining " + num_items + " of " + blocks_to_mine.get(0).getBlock().toString());

        if(!canPlayerMineBlock(MinecraftClient.getInstance().player, blocks_to_mine.get(0).getBlock().getDefaultState())) {
            _failedState = TaskState.terminatedFailure("Player does not have a suitable tool to mine the block");
            return;
        }

        MinecraftClient.getInstance().execute(() -> {
            BaritoneAPI.getProvider().getPrimaryBaritone().getMineProcess().mine(num_items, blocks_to_mine.toArray(new BlockOptionalMeta[0]));
        });
        _startTime = System.currentTimeMillis();
    }

    @Override
    public TaskState getState() {
        if (_failedState != null) {
            return _failedState;
        }
        // If we have enough items, we've succeeded
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        BlockOptionalMetaLookup filter = new BlockOptionalMetaLookup(blocks_to_mine.toArray(new BlockOptionalMeta[0]));
        // Check if we have enough items
        int totalCount = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (filter.has(player.getInventory().getStack(i))) {
                totalCount += player.getInventory().getStack(i).getCount();
            }
        }
        if (totalCount >= num_items) {
            return TaskState.terminatedSuccess();
        }

        if (System.currentTimeMillis() - _startTime > 3000 && !BaritoneAPI.getProvider().getPrimaryBaritone().getMineProcess().isActive()) {
            return TaskState.terminatedFailure("Mining could not find enough of the specified blocks");
        }
        return TaskState.running();
    }

    @Override
    public double getFinishCheckInterval() {
        return 1;
    }

    @Override
    public void cleanup() {
        if (BaritoneAPI.getProvider().getPrimaryBaritone().getMineProcess().isActive()) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getMineProcess().cancel();
        }
    }
}
