package io.github.snower.jaslock.callback;

import io.github.snower.jaslock.commands.Command;

import java.util.List;
import java.util.function.Consumer;

public class CallbackCommand {
    private Command command;
    private Consumer<CallbackCommandResult> callback;
    private Consumer<CallbackCommandResult> timeoutCallback;
    private List<CallbackCommand> timeoutQueues;
    private long timeoutAt;
    private boolean finished;

    public CallbackCommand(Command command, Consumer<CallbackCommandResult> callback, Consumer<CallbackCommandResult> timeoutCallback) {
        this.command = command;
        this.callback = callback;
        this.timeoutCallback = timeoutCallback;
        this.timeoutAt = 0;
        this.finished = false;
    }

    public Command getCommand() {
        return command;
    }

    public Consumer<CallbackCommandResult> getCallback() {
        return callback;
    }

    public Consumer<CallbackCommandResult> getTimeoutCallback() {
        return timeoutCallback;
    }

    public long getTimeoutAt() {
        return timeoutAt;
    }

    public void setTimeoutAt(long timeoutAt) {
        this.timeoutAt = timeoutAt;
    }

    public void addTimeoutQueues(List<CallbackCommand> timeoutQueues) {
        this.timeoutQueues = timeoutQueues;
        this.timeoutQueues.add(this);
    }

    public boolean isFinished() {
        return finished;
    }

    public void close() {
        if (this.timeoutQueues != null) {
            this.timeoutQueues.remove(this);
        }
        this.finished = true;
        this.command = null;
        this.callback = null;
        this.timeoutCallback = null;
        this.timeoutQueues = null;
    }
}
