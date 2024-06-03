package com.example;

import baritone.api.BaritoneAPI;
import com.example.tasks.*;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BaritoneHandler {

    public static List<ITask> getTasksFromBaritoneCommand(String command) {
        if (command.startsWith("place ")) {
            String blockName = command.substring(6);
            return Arrays.asList(new PlaceBlockNearby(blockName));
        } else if (command.startsWith("craft ")) {
            String[] args = command.substring(6).split(" ");
            return Craft.getCraftingTasks(args);
        } else if (command.startsWith("smelt ")) {
            String[] args = command.substring(6).split(" ");
            return Smelt.getSmeltingTasks(args);
        } else if (command.startsWith("mine ")) {
            String[] args = command.substring(5).split(" ");
            return Arrays.asList(new Mine(args));
        } else if (command.startsWith("goto_block ")) {
            String blockName = command.substring(11);
            return Arrays.asList(new GotoBlock(blockName));
        } else if (command.startsWith("interact_block ")) {
            String blockName = command.substring(15);
            return Arrays.asList(new InteractBlock(blockName));
        } else if (command.startsWith("goto_y_level ")) {
            int level = Integer.parseInt(command.substring(13));
            return Arrays.asList(new GotoYLevel(level));
        }
        return Arrays.asList(new AutoFailTask("Invalid command: " + command));
    }

}
