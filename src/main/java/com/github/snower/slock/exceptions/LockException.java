package com.github.snower.slock.exceptions;

import com.github.snower.slock.Lock;
import com.github.snower.slock.commands.Command;
import com.github.snower.slock.commands.CommandResult;

public class LockException extends SlockException {
    private Command command;
    private CommandResult commandResult;

    public LockException(Command command, CommandResult commandResult) {
        super();

        this.command = command;
        this.commandResult = commandResult;
    }

    public Command getCommand() {
        return command;
    }

    public CommandResult getCommandResult() {
        return commandResult;
    }
}
