package io.github.snower.jaslock.exceptions;

import io.github.snower.jaslock.commands.Command;
import io.github.snower.jaslock.commands.CommandResult;

public class LockUnlockedException extends LockException {
    public LockUnlockedException(Command command, CommandResult commandResult) {
        super(command, commandResult);
    }
}
