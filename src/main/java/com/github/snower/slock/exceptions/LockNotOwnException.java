package com.github.snower.slock.exceptions;

import com.github.snower.slock.commands.Command;
import com.github.snower.slock.commands.CommandResult;

public class LockNotOwnException extends LockException {
    public LockNotOwnException(Command command, CommandResult commandResult) {
        super(command, commandResult);
    }
}
