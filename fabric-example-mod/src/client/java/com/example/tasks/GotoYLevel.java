package com.example.tasks;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalYLevel;
import com.example.Utility;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import static java.lang.Math.abs;

public class GotoYLevel implements ITask {
    int level;
    TaskState _failedState;
    long _startTime;
    double _timeout;

    public GotoYLevel(int level) {
        this.level = level;
        _timeout = 200000;
    }
    public GotoYLevel(int level, double timeout) {
        this.level = level;
        _timeout = timeout;
    }

    @Override
    public void execute() {
        Goal goal = new GoalYLevel(level);
        MinecraftClient.getInstance().execute(() -> {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(goal);
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

        if (abs(player.getBlockY() - this.level) < 2) return TaskState.terminatedSuccess();

        if (System.currentTimeMillis() - _startTime > 3000 && !BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().isActive()) {
            return TaskState.terminatedFailure("Could not get to y-level " + this.level);
        }
        if (System.currentTimeMillis() - _startTime > _timeout) {
            return TaskState.terminatedFailure("Took too long to get to y-level " + this.level);
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
