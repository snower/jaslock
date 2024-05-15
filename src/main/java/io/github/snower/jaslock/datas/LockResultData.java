package io.github.snower.jaslock.datas;

import io.github.snower.jaslock.commands.ICommand;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class LockResultData {
    protected byte[] data;

    public LockResultData(byte[] data) {
        this.data = data;
    }

    private int GetValueOffset() {
        if ((data[5] & ICommand.LOCK_DATA_FLAG_CONTAINS_PROPERTY) != 0) {
            return ((((int) data[6]) & 0xff) | (((int) data[7]) & 0xff) << 8) + 6;
        }
        return 6;
    }

    public byte[] getRawData() {
        return data;
    }

    public byte[] getDataAsBytes() {
        int offset = GetValueOffset();
        if (data.length <= offset) {
            return null;
        }
        return Arrays.copyOfRange(data, offset, data.length);
    }

    public String getDataAsString() {
        int offset = GetValueOffset();
        if (data.length <= offset) {
            return "";
        }
        return new String(data, offset, data.length - offset, StandardCharsets.UTF_8);
    }

    public long getDataAsLong() {
        int offset = GetValueOffset();
        long value = 0;
        for (int i = 0; i < 8; i++) {
            if (i + offset >= data.length) break;
            value = value | ((((long) data[i + offset]) & 0xff) << (i * 8));
        }
        return value;
    }
}
