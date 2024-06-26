package io.github.snower.jaslock.datas;

import io.github.snower.jaslock.commands.ICommand;

public class LockUnsetData extends LockData {
    public LockUnsetData() {
        super(ICommand.LOCK_DATA_STAGE_CURRENT, ICommand.LOCK_DATA_COMMAND_TYPE_UNSET, (byte) 0, new byte[0]);
    }

    public LockUnsetData(byte commandFlag) {
        super(ICommand.LOCK_DATA_STAGE_CURRENT, ICommand.LOCK_DATA_COMMAND_TYPE_UNSET, commandFlag, new byte[0]);
    }
}
