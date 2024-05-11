package io.github.snower.jaslock.datas;

import io.github.snower.jaslock.commands.ICommand;

public class LockShiftData extends LockData {
    public LockShiftData(int length) {
        super(ICommand.LOCK_DATA_STAGE_LOCK, ICommand.LOCK_DATA_COMMAND_TYPE_SHIFT, (byte) 0, new byte[]{
                (byte)(length & 0xff), (byte)((length >> 8) & 0xff), (byte)((length >> 16) & 0xff), (byte)((length >> 24) & 0xff)});
    }
}
