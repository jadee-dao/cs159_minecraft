package com.example.tasks;

/* Represents a task along with its associated command */
public class TaskCommand {
    public final ITask task;
    public final String command;
    public TaskCommand(ITask task, String command) {
        this.task = task;
        this.command = command;
    }
}
