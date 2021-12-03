package io.github.snower.jaslock.exceptions;

import io.github.snower.jaslock.commands.Command;
import io.github.snower.jaslock.commands.CommandResult;

public class LockNotOwnException extends LockException {
    public LockNotOwnException(Command command, CommandResult commandResult) {
        super(command, commandResult);
    }
}
