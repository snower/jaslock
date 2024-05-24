package io.github.snower.jaslock.datas;

import io.github.snower.jaslock.commands.ICommand;

public class LockPopData extends LockData {
    public LockPopData(int count) {
        super(ICommand.LOCK_DATA_STAGE_CURRENT, ICommand.LOCK_DATA_COMMAND_TYPE_POP, ICommand.LOCK_DATA_FLAG_VALUE_TYPE_NUMBER, new byte[]{
                (byte)(count & 0xff), (byte)((count >> 8) & 0xff), (byte)((count >> 16) & 0xff), (byte)((count >> 24) & 0xff)});
    }

    public LockPopData(int count, byte commandFlag) {
        super(ICommand.LOCK_DATA_STAGE_CURRENT, ICommand.LOCK_DATA_COMMAND_TYPE_POP, (byte) (commandFlag | ICommand.LOCK_DATA_FLAG_VALUE_TYPE_NUMBER), new byte[]{
                (byte)(count & 0xff), (byte)((count >> 8) & 0xff), (byte)((count >> 16) & 0xff), (byte)((count >> 24) & 0xff)});
    }
}
