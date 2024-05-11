package io.github.snower.jaslock.datas;

import io.github.snower.jaslock.commands.ICommand;
import io.github.snower.jaslock.commands.LockCommand;

public class LockExecuteData extends LockData {
    public LockExecuteData(byte commandStage, LockCommand lockCommand) {
        super(commandStage, ICommand.LOCK_DATA_COMMAND_TYPE_EXECUTE, (byte) 0, lockCommand.dumpCommand());
    }

    public LockExecuteData(LockCommand lockCommand) {
        this(ICommand.LOCK_DATA_STAGE_LOCK, lockCommand);
    }
}
