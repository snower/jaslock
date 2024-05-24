package io.github.snower.jaslock.datas;

import io.github.snower.jaslock.commands.ICommand;

import java.nio.charset.StandardCharsets;

public class LockPushData extends LockData {
    public LockPushData(byte[] value) {
        super(ICommand.LOCK_DATA_STAGE_CURRENT, ICommand.LOCK_DATA_COMMAND_TYPE_PUSH, (byte) 0, value);
    }

    public LockPushData(String value) {
        super(ICommand.LOCK_DATA_STAGE_CURRENT, ICommand.LOCK_DATA_COMMAND_TYPE_PUSH, (byte) 0, value.getBytes(StandardCharsets.UTF_8));
    }

    public LockPushData(byte[] value, byte commandFlag) {
        super(ICommand.LOCK_DATA_STAGE_CURRENT, ICommand.LOCK_DATA_COMMAND_TYPE_PUSH, commandFlag, value);
    }

    public LockPushData(String value, byte commandFlag) {
        super(ICommand.LOCK_DATA_STAGE_CURRENT, ICommand.LOCK_DATA_COMMAND_TYPE_PUSH, commandFlag, value.getBytes(StandardCharsets.UTF_8));
    }
}
