package io.github.snower.jaslock.callback;

import io.github.snower.jaslock.commands.Command;
import io.github.snower.jaslock.commands.CommandResult;
import io.github.snower.jaslock.exceptions.SlockException;

public class DeferredCommandResult {
    private final Command command;
    private final CommandResult commandResult;
    private final SlockException exception;

    public DeferredCommandResult(Command command, CommandResult commandResult, SlockException exception) {
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

    public CommandResult getResult() throws SlockException {
        if (exception != null) {
            throw exception;
        }
        return commandResult;
    }
}
