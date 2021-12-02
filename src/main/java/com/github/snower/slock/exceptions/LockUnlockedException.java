package com.github.snower.slock.exceptions;

import com.github.snower.slock.commands.Command;
import com.github.snower.slock.commands.CommandResult;

public class LockUnlockedException extends LockException {
    public LockUnlockedException(Command command, CommandResult commandResult) {
        super(command, commandResult);
    }
}
