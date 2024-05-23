package io.github.snower.jaslock.datas;

import io.github.snower.jaslock.commands.ICommand;

import java.nio.charset.StandardCharsets;

public class LockAppendData extends LockData {
    public LockAppendData(byte[] value) {
        super(ICommand.LOCK_DATA_STAGE_LOCK, ICommand.LOCK_DATA_COMMAND_TYPE_APPEND, (byte) 0, value);
    }

    public LockAppendData(String value) {
        super(ICommand.LOCK_DATA_STAGE_LOCK, ICommand.LOCK_DATA_COMMAND_TYPE_APPEND, (byte) 0, value.getBytes(StandardCharsets.UTF_8));
    }

    public LockAppendData(byte[] value, byte commandFlag) {
        super(ICommand.LOCK_DATA_STAGE_LOCK, ICommand.LOCK_DATA_COMMAND_TYPE_APPEND, commandFlag, value);
    }

    public LockAppendData(String value, byte commandFlag) {
        super(ICommand.LOCK_DATA_STAGE_LOCK, ICommand.LOCK_DATA_COMMAND_TYPE_APPEND, commandFlag, value.getBytes(StandardCharsets.UTF_8));
    }
}
