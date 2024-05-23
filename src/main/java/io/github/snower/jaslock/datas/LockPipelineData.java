package io.github.snower.jaslock.datas;

import io.github.snower.jaslock.commands.ICommand;
import io.github.snower.jaslock.exceptions.LockDataException;

public class LockPipelineData extends LockData {
    private final LockData[] lockDatas;

    public LockPipelineData(LockData[] lockDatas) {
        super(ICommand.LOCK_DATA_STAGE_LOCK, ICommand.LOCK_DATA_COMMAND_TYPE_PIPELINE, (byte) 0, null);
        this.lockDatas = lockDatas;
    }

    public LockPipelineData(LockData[] lockDatas, byte commandFlag) {
        super(ICommand.LOCK_DATA_STAGE_LOCK, ICommand.LOCK_DATA_COMMAND_TYPE_PIPELINE, commandFlag, null);
        this.lockDatas = lockDatas;
    }

    @Override
    public byte[] dumpData() throws LockDataException {
        if (lockDatas == null || lockDatas.length == 0) {
            throw new LockDataException("Data value is null");
        }
        byte[][] values = new byte[lockDatas.length][];
        int valueLength = 0;
        for (int i = 0; i < lockDatas.length; i++) {
            values[i] = lockDatas[i].dumpData();
            valueLength += values[i].length;
        }
        byte[] data = new byte[valueLength + 6];
        data[0] = (byte) ((valueLength + 2) & 0xff);
        data[1] = (byte) (((valueLength + 2) >> 8 ) & 0xff);
        data[2] = (byte) (((valueLength + 2) >> 16 ) & 0xff);
        data[3] = (byte) (((valueLength + 2) >> 24 ) & 0xff);
        data[4] = (byte) (((commandStage << 6) & 0xc0) | (commandType & 0x3f));
        data[5] = commandFlag;
        for (int i = 0, j = 6; i < values.length; i++) {
            System.arraycopy(values[i], 0, data, j, values[i].length);
            j += values[i].length;
        }
        return data;
    }
}
