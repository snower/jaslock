package io.github.snower.jaslock.callback;

import io.github.snower.jaslock.commands.Command;

import java.util.List;
import java.util.function.Consumer;

public class DeferredCommand {
    private Command command;
    private Consumer<DeferredCommandResult> callback;
    private Consumer<DeferredCommandResult> timeoutCallback;
    private List<DeferredCommand> timeoutQueues;
    private long timeoutAt;
    private boolean finished;

    public DeferredCommand(Command command, Consumer<DeferredCommandResult> callback, Consumer<DeferredCommandResult> timeoutCallback) {
        this.command = command;
        this.callback = callback;
        this.timeoutCallback = timeoutCallback;
        this.timeoutAt = 0;
        this.finished = false;
    }

    public Command getCommand() {
        return command;
    }

    public Consumer<DeferredCommandResult> getCallback() {
        return callback;
    }

    public Consumer<DeferredCommandResult> getTimeoutCallback() {
        return timeoutCallback;
    }

    public long getTimeoutAt() {
        return timeoutAt;
    }

    public void setTimeoutAt(long timeoutAt) {
        this.timeoutAt = timeoutAt;
    }

    public void addTimeoutQueues(List<DeferredCommand> timeoutQueues) {
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
