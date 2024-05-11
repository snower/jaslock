package io.github.snower.jaslock.datas;

import io.github.snower.jaslock.exceptions.LockDataException;

public class LockData {
    protected byte commandStage;
    protected byte commandType;
    protected byte commandFlag;
    protected byte[] value;

    public LockData(byte commandStage, byte commandType, byte commandFlag, byte[] value) {
        this.commandStage = commandStage;
        this.commandType = commandType;
        this.commandFlag = commandFlag;
        this.value = value;
    }

    public byte[] dumpData() throws LockDataException {
        if (value == null) {
            throw new LockDataException();
        }
        byte[] data = new byte[value.length + 6];
        data[0] = (byte) ((value.length + 2) & 0xff);
        data[1] = (byte) (((value.length + 2) >> 8 ) & 0xff);
        data[2] = (byte) (((value.length + 2) >> 16 ) & 0xff);
        data[3] = (byte) (((value.length + 2) >> 24 ) & 0xff);
        data[4] = (byte) (((commandStage << 6) & 0xc0) | (commandType & 0x3f));
        data[5] = commandFlag;
        System.arraycopy(value, 0, data, 6, value.length);
        return data;
    }
}
