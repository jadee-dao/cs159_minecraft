package com.example.tasks;

public class AutoSucceedTask implements ITask {

    @Override
    public void execute() {

    }

    @Override
    public TaskState getState() {
        return TaskState.terminatedSuccess();
    }

    @Override
    public double getFinishCheckInterval() {
        return 0;
    }

    @Override
    public void cleanup() {

    }
}
