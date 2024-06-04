package com.example;

import com.google.gson.Gson;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import baritone.api.BaritoneAPI;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.*;

import java.net.InetSocketAddress;

public class ExampleModClient implements ClientModInitializer {
	private WebSocketModServer server;
	private static final Gson GSON = new Gson();

	@Override
	public void onInitializeClient() {
		Plan globalPlan = new Plan();

		server = new WebSocketModServer(new InetSocketAddress("localhost", 8080), globalPlan);
		server.setConnectionLostTimeout(0);
		server.start();
		System.out.println("WebSocket server started on ws://localhost:8080");
		BaritoneAPI.getSettings();
		CommandRegistrationCallback.EVENT.register(this::registerCommands);
	}

	private void registerCommands(CommandDispatcher<ServerCommandSource> serverCommandSourceCommandDispatcher, CommandRegistryAccess commandRegistryAccess, RegistrationEnvironment registrationEnvironment) {
		serverCommandSourceCommandDispatcher.register(literal("setgoal")
				.then(argument("goal", StringArgumentType.string())
						.executes(this::handleSetGoalCommand)));
	}

	private int handleSetGoalCommand(CommandContext<ServerCommandSource> context) {
		String goal = StringArgumentType.getString(context, "goal");
		context.getSource().sendFeedback(Text.literal("Goal set: " + goal), false);
		sendGoalToPython(goal);
		return 1;
	}

	private void sendGoalToPython(String goal) {
		WebSocketModServer.JsonMessage goalMessage = new WebSocketModServer.JsonMessage();
		goalMessage.type = "setGoal";
		goalMessage.body = new String[]{goal};
		goalMessage.inventory = Utility.getPlayerInventoryString();
		goalMessage.info = Utility.getPlayerInfoString();
		server.broadcast(GSON.toJson(goalMessage));
	}

}