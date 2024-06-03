package com.example.tasks;

public class TaskState {
    public TaskStatus status;
    private final String failureReason;

    private TaskState(TaskStatus status, String failureReason) {
        this.status = status;
        this.failureReason = failureReason;
    }

    public static TaskState running() {
        return new TaskState(TaskStatus.RUNNING, null);
    }

    public static TaskState terminatedFailure(String failureReason) {
        return new TaskState(TaskStatus.TERMINATED_FAILURE, failureReason);
    }

    public static TaskState terminatedSuccess() {
        return new TaskState(TaskStatus.TERMINATED_SUCCESS, null);
    }

    public boolean isRunning() {
        return status == TaskStatus.RUNNING;
    }

    public boolean isTerminated() {
        return status == TaskStatus.TERMINATED_FAILURE || status == TaskStatus.TERMINATED_SUCCESS;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }
}