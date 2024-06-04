package com.example;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.core.tools.picocli.CommandLine;

public class Utility {

    public static BlockPos findBlockNearby(int range, ClientPlayerEntity player, Block block) {
        int bestScore = -1;
        BlockPos bestPos = null;
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = player.getBlockPos().add(x, y, z);
                    if (player.world.getBlockState(pos).getBlock().equals(block)) {
                        int score = Math.abs(x) + Math.abs(y) + Math.abs(z);
                        if (bestPos == null || score < bestScore) {
                            bestPos = pos;
                            bestScore = score;
                        }
                    }
                }
            }
        }
        return bestPos;
    }

    public static String getPlayerInventoryString() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            sb.append(stack.getItem().toString());
            sb.append(": ");
            sb.append(stack.getCount());
            sb.append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    public static String getPlayerInfoString() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        BlockPos pos = player.getBlockPos();
        String biome = player.world.getBiome(pos).toString();
        String yLevel = Integer.toString(pos.getY());
        return "Player is in biome " + biome + " at y-level " + yLevel + ".";
    }

    public static void chat(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.inGameHud.getChatHud().addMessage(Text.of(message));
    }

    public static String getFailureMessage(String failureReason) {
        return "§4§lTask failed: §4" + failureReason;
    }

    public static String getSuccessMessage() {
        return "§2§lPlan succeeded!";
    }
}
