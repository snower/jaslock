package io.github.snower.jaslock.datas;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class LockResultData {
    protected byte[] data;

    public LockResultData(byte[] data) {
        this.data = data;
    }

    public byte[] getRawData() {
        return data;
    }

    public byte[] getDataAsBytes() {
        return Arrays.copyOfRange(data, 6, data.length);
    }

    public String getDataAsString() {
        return new String(data, 6, data.length - 6, StandardCharsets.UTF_8);
    }

    public long getDataAsLong() {
        long value = 0;
        for (int i = 0; i < 8; i++) {
            if (i + 6 >= data.length) break;
            value = value | ((((long) data[i + 6]) & 0xff) << (i * 8));
        }
        return value;
    }
}
