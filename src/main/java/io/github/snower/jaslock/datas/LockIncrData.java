package io.github.snower.jaslock.datas;

import io.github.snower.jaslock.commands.ICommand;

public class LockIncrData extends LockData {
    public LockIncrData(long incrValue) {
        super(ICommand.LOCK_DATA_STAGE_LOCK, ICommand.LOCK_DATA_COMMAND_TYPE_INCR, (byte) 0, new byte[]{
                (byte)(incrValue & 0xff), (byte)((incrValue >> 8) & 0xff), (byte)((incrValue >> 16) & 0xff), (byte)((incrValue >> 24) & 0xff),
                (byte)((incrValue >> 32) & 0xff), (byte)((incrValue >> 40) & 0xff), (byte)((incrValue >> 48) & 0xff), (byte)((incrValue >> 56) & 0xff)});
    }

    public LockIncrData() {
        this(1);
    }
}
