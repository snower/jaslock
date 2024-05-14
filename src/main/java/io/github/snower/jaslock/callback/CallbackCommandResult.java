package io.github.snower.jaslock.callback;

import io.github.snower.jaslock.commands.Command;
import io.github.snower.jaslock.commands.CommandResult;
import io.github.snower.jaslock.commands.LockCommandResult;
import io.github.snower.jaslock.datas.LockResultData;
import io.github.snower.jaslock.exceptions.SlockException;

public class CallbackCommandResult {
    private final Command command;
    private final CommandResult commandResult;
    private final SlockException exception;

    public CallbackCommandResult(Command command, CommandResult commandResult, SlockException exception) {
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

    public SlockException getException() {
        return exception;
    }

    public CommandResult getResult() throws SlockException {
        if (exception != null) {
            throw exception;
        }
        return commandResult;
    }

    public LockResultData getLockData() {
        if (commandResult instanceof LockCommandResult) {
            return ((LockCommandResult) commandResult).getLockResultData();
        }
        return null;
    }

    public byte[] getLockDataAsBytes() {
        if (commandResult instanceof LockCommandResult) {
            return ((LockCommandResult) commandResult).getLockResultData().getDataAsBytes();
        }
        return null;
    }

    public String getLockDataAsString() {
        if (commandResult instanceof LockCommandResult) {
            return ((LockCommandResult) commandResult).getLockResultData().getDataAsString();
        }
        return null;
    }

    public Long getLockDataAsLong() {
        if (commandResult instanceof LockCommandResult) {
            return ((LockCommandResult) commandResult).getLockResultData().getDataAsLong();
        }
        return 0L;
    }
}
