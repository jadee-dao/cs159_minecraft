package com.example;
import com.example.tasks.ITask;
import com.example.tasks.TaskCommand;
import com.example.tasks.TaskState;
import com.example.tasks.TaskStatus;

import java.util.ArrayList;
import java.util.List;


public class Plan {
    public List<TaskCommand> tasks;
    public boolean ready = false;

    public Plan() {
        tasks = new ArrayList<>();
    }

    public void enqueue(ITask task, String command) {
        tasks.add(new TaskCommand(task, command));
    }

    public void clear() {
        tasks.clear();
    }

    public TaskState execute() {
        while(!tasks.isEmpty()) {
            TaskCommand taskCommand = tasks.removeFirst();
            ITask task = taskCommand.task;
            String command = taskCommand.command;
            task.execute();
            double checkInterval = task.getFinishCheckInterval();
            while(true) {
                if (task.getState().isTerminated()) {
                    break;
                }
                try {
                    Thread.sleep((long) checkInterval);
                } catch (InterruptedException e) {
                    return TaskState.terminatedFailure("Sleep interrupted");
                }
            }

            if (task.getState().getStatus() == TaskStatus.TERMINATED_FAILURE) {
                return TaskState.terminatedFailure(
                        "Command " + command + " failed: " +
                        task.getState().getFailureReason());
            }
            task.cleanup();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return TaskState.terminatedFailure("Sleep interrupted");
            }
        }
        return TaskState.terminatedSuccess();
    }
}
