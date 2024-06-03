package com.example.tasks;

public class AutoFailTask implements ITask {
        String _reason;
        public AutoFailTask(String reason) {
            _reason = reason;
        }

        @Override
        public void execute() {

        }

        @Override
        public TaskState getState() {
            return TaskState.terminatedFailure(_reason);
        }

        @Override
        public double getFinishCheckInterval() {
            return 0;
        }

        @Override
        public void cleanup() {

        }
}
