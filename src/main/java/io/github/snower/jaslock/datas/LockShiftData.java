package io.github.snower.jaslock.datas;

import io.github.snower.jaslock.commands.ICommand;

public class LockShiftData extends LockData {
    public LockShiftData(int length) {
        super(ICommand.LOCK_DATA_STAGE_CURRENT, ICommand.LOCK_DATA_COMMAND_TYPE_SHIFT, ICommand.LOCK_DATA_FLAG_VALUE_TYPE_NUMBER, new byte[]{
                (byte)(length & 0xff), (byte)((length >> 8) & 0xff), (byte)((length >> 16) & 0xff), (byte)((length >> 24) & 0xff)});
    }

    public LockShiftData(int length, byte commandFlag) {
        super(ICommand.LOCK_DATA_STAGE_CURRENT, ICommand.LOCK_DATA_COMMAND_TYPE_SHIFT, (byte) (commandFlag | ICommand.LOCK_DATA_FLAG_VALUE_TYPE_NUMBER), new byte[]{
                (byte)(length & 0xff), (byte)((length >> 8) & 0xff), (byte)((length >> 16) & 0xff), (byte)((length >> 24) & 0xff)});
    }
}
