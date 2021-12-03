package io.github.snower.jaslock.exceptions;

import io.github.snower.jaslock.commands.Command;
import io.github.snower.jaslock.commands.CommandResult;

public class LockTimeoutException extends LockException {
    public LockTimeoutException(Command command, CommandResult commandResult) {
        super(command, commandResult);
    }
}
