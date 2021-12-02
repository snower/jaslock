package com.github.snower.slock.exceptions;

import com.github.snower.slock.commands.Command;
import com.github.snower.slock.commands.CommandResult;

public class LockTimeoutException extends LockException {
    public LockTimeoutException(Command command, CommandResult commandResult) {
        super(command, commandResult);
    }
}
