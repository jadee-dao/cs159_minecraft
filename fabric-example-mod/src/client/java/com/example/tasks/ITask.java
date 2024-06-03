package com.example.tasks;

public interface ITask {
    void execute();
    TaskState getState();
    double getFinishCheckInterval();
    void cleanup();
}
