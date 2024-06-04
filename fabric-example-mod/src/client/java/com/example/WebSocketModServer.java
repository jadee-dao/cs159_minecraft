package com.example;

import baritone.api.BaritoneAPI;
import com.example.tasks.TaskState;
import com.example.tasks.TaskStatus;
import com.google.gson.Gson;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import com.example.tasks.ITask;

import java.net.InetSocketAddress;
import java.util.List;

public class WebSocketModServer extends WebSocketServer {
    private static final Gson GSON = new Gson();

    public BaritoneHandler baritoneHandler;
    public Plan _globalPlan;

    public WebSocketModServer(InetSocketAddress address, Plan globalPlan) {
        super(address);
        baritoneHandler = new BaritoneHandler();
        _globalPlan = globalPlan;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("New connection: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Closed connection: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Received message: " + message);
        JsonMessage jsonMessage = GSON.fromJson(message, JsonMessage.class);
        handleMessage(jsonMessage, conn);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Server started successfully");
    }

    private void handleMessage(JsonMessage message, WebSocket conn) {
        switch (message.type) {
            case "initialPlan":
                handleInitialPlan(message.body, conn);
                break;
            case "planUpdate":
                handlePlanUpdate(message.body, conn);
                break;
            default:
                System.out.println("Unknown message type: " + message.type);
        }
    }

    private void handleInitialPlan(String[] commands, WebSocket conn) {
        Utility.chat("Executing plan...");
        _globalPlan.clear();
        for (String command : commands) {
            List<ITask> newTasks = BaritoneHandler.getTasksFromBaritoneCommand(command);
            for (ITask task : newTasks) {
                _globalPlan.enqueue(task, command);
            }
        }
        TaskState state = _globalPlan.execute();
        if (state.getStatus() == TaskStatus.TERMINATED_FAILURE) {
            System.out.println("Plan failed: " + state.getFailureReason());
            Utility.chat(Utility.getFailureMessage(state.getFailureReason()));
            JsonMessage failureMessage = new JsonMessage();
            failureMessage.type = "planFailure";
            failureMessage.failureReason = state.getFailureReason();
            failureMessage.inventory = Utility.getPlayerInventoryString();
            failureMessage.info = Utility.getPlayerInfoString();
            conn.send(GSON.toJson(failureMessage));
        } else {
            System.out.println("Plan succeeded");
            Utility.chat(Utility.getSuccessMessage());
            JsonMessage successMessage = new JsonMessage();
            successMessage.type = "planSuccess";
            conn.send(GSON.toJson(successMessage));
        }
        _globalPlan.clear();
    }

    private void handlePlanUpdate(String[] commands, WebSocket conn) {
        Utility.chat("Executing updated plan...");
        for (String command : commands) {
            List<ITask> newTasks = BaritoneHandler.getTasksFromBaritoneCommand(command);
            for (ITask task : newTasks) {
                _globalPlan.enqueue(task, command);
            }
        }
        TaskState state = _globalPlan.execute();
        if (state.getStatus() == TaskStatus.TERMINATED_FAILURE) {
            System.out.println("Plan failed: " + state.getFailureReason());
            Utility.chat(Utility.getFailureMessage(state.getFailureReason()));
            JsonMessage failureMessage = new JsonMessage();
            failureMessage.type = "planFailure";
            failureMessage.failureReason = state.getFailureReason();
            failureMessage.inventory = Utility.getPlayerInventoryString();
            failureMessage.info = Utility.getPlayerInfoString();
            conn.send(GSON.toJson(failureMessage));
        } else {
            System.out.println("Plan succeeded");
            Utility.chat(Utility.getSuccessMessage());
            JsonMessage successMessage = new JsonMessage();
            successMessage.type = "planSuccess";
            successMessage.inventory = Utility.getPlayerInventoryString();
            successMessage.info = Utility.getPlayerInfoString();
            conn.send(GSON.toJson(successMessage));
        }
        _globalPlan.clear();
    }

    @Override
    public void onWebsocketPing(WebSocket conn, org.java_websocket.framing.Framedata f) {
        // Override to disable automatic ping/pong response
    }

    @Override
    public void onWebsocketPong(WebSocket conn, org.java_websocket.framing.Framedata f) {
        // Override to disable automatic ping/pong response
    }

    public static class JsonMessage {
        String type;
        String[] body;
        String failureReason;
        String inventory;
        String info;
    }
}
