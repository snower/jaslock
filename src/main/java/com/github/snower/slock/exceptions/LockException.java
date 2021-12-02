package com.github.snower.slock.exceptions;

import com.github.snower.slock.Lock;
import com.github.snower.slock.commands.Command;
import com.github.snower.slock.commands.CommandResult;

public class LockException extends SlockException {
    static String[] ERROR_MSG = new String[]{
            "OK",
            "UNKNOWN_MAGIC",
            "UNKNOWN_VERSION",
            "UNKNOWN_DB",
            "UNKNOWN_COMMAND",
            "LOCKED_ERROR",
            "UNLOCK_ERROR",
            "UNOWN_ERROR",
            "TIMEOUT",
            "EXPRIED",
            "RESULT_STATE_ERROR",
            "UNKNOWN_ERROR"
    };

    private Command command;
    private CommandResult commandResult;

    public LockException(Command command, CommandResult commandResult) {
        super(getErrMessage(commandResult));

        this.command = command;
        this.commandResult = commandResult;
    }

    protected static String getErrMessage(CommandResult commandResult) {
        if(commandResult == null) {
            return "UNKNOWN_ERROR";
        }
        if(commandResult.getResult() > 0 && commandResult.getResult() < ERROR_MSG.length) {
            return "Code " + String.valueOf(commandResult.getResult()) + " " + ERROR_MSG[commandResult.getResult()];
        }
        return "Code " + String.valueOf(commandResult.getResult());
    }

    public Command getCommand() {
        return command;
    }

    public CommandResult getCommandResult() {
        return commandResult;
    }
}
