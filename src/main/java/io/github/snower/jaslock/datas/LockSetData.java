package io.github.snower.jaslock.datas;

import io.github.snower.jaslock.commands.ICommand;

import java.nio.charset.StandardCharsets;

public class LockSetData extends LockData {
    public LockSetData(byte[] value) {
        super(ICommand.LOCK_DATA_STAGE_CURRENT, ICommand.LOCK_DATA_COMMAND_TYPE_SET, (byte) 0, value);
    }

    public LockSetData(String value) {
        super(ICommand.LOCK_DATA_STAGE_CURRENT, ICommand.LOCK_DATA_COMMAND_TYPE_SET, (byte) 0, value.getBytes(StandardCharsets.UTF_8));
    }

    public LockSetData(byte[] value, byte commandFlag) {
        super(ICommand.LOCK_DATA_STAGE_CURRENT, ICommand.LOCK_DATA_COMMAND_TYPE_SET, commandFlag, value);
    }

    public LockSetData(String value, byte commandFlag) {
        super(ICommand.LOCK_DATA_STAGE_CURRENT, ICommand.LOCK_DATA_COMMAND_TYPE_SET, commandFlag, value.getBytes(StandardCharsets.UTF_8));
    }
}
