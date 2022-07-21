package io.github.snower.jaslock.deferred;

import io.github.snower.jaslock.commands.Command;
import io.github.snower.jaslock.commands.CommandResult;

public class DeferredCommandResult {
    private final Command command;
    private final CommandResult commandResult;
    private final Exception exception;

    public DeferredCommandResult(Command command, CommandResult commandResult, Exception exception) {
        this.command = command;
        this.commandResult = commandResult;
        this.exception = exception;
    }

    public Command getCommand() {
        return command;
    }

    public CommandResult getCommandResult() {
        return commandResult;
    }

    public Exception getException() {
        return exception;
    }

    public CommandResult getResult() throws Exception {
        if (exception != null) {
            throw exception;
        }
        return commandResult;
    }
}
