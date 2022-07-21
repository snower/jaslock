package io.github.snower.jaslock.deferred;

import io.github.snower.jaslock.commands.Command;

import java.util.function.Consumer;

public class DeferredCommand {
    private final Command command;
    private final Consumer<DeferredCommandResult> callback;
    private long timeoutAt;
    private boolean finished;

    public DeferredCommand(Command command, Consumer<DeferredCommandResult> callback) {
        this.command = command;
        this.callback = callback;
        this.timeoutAt = 0;
        this.finished = false;
    }

    public Command getCommand() {
        return command;
    }

    public Consumer<DeferredCommandResult> getCallback() {
        return callback;
    }

    public long getTimeoutAt() {
        return timeoutAt;
    }

    public void setTimeoutAt(long timeoutAt) {
        this.timeoutAt = timeoutAt;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public boolean isFinished() {
        return finished;
    }
}
