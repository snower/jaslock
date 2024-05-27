package io.github.snower.jaslock.datas;

import io.github.snower.jaslock.commands.ICommand;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class LockResultData {
    protected byte[] data;

    public LockResultData(byte[] data) {
        this.data = data;
    }

    private int getValueOffset() {
        if ((data[5] & ICommand.LOCK_DATA_FLAG_CONTAINS_PROPERTY) != 0) {
            return ((((int) data[6]) & 0xff) | (((int) data[7]) & 0xff) << 8) + 8;
        }
        return 6;
    }

    public byte[] getRawData() {
        return data;
    }

    public byte[] getDataAsBytes() {
        int offset = getValueOffset();
        if (data.length <= offset) {
            return null;
        }
        return Arrays.copyOfRange(data, offset, data.length);
    }

    public String getDataAsString() {
        int offset = getValueOffset();
        if (data.length <= offset) {
            return "";
        }
        return new String(data, offset, data.length - offset, StandardCharsets.UTF_8);
    }

    public long getDataAsLong() {
        int offset = getValueOffset();
        long value = 0;
        for (int i = 0; i < 8; i++) {
            if (i + offset >= data.length) break;
            value = value | ((((long) data[i + offset]) & 0xff) << (i * 8));
        }
        return value;
    }

    public List<byte[]> getRawDataAsList() {
        if ( data == null || data[4] == ICommand.LOCK_DATA_COMMAND_TYPE_UNSET || (data[5]&ICommand.LOCK_DATA_FLAG_VALUE_TYPE_ARRAY) == 0) {
            return null;
        }
        List<byte[]> values = new ArrayList<>();
        int index = getValueOffset();
        while (index+4 < data.length) {
            int valueLen = ((int) data[index] & 0xff) | ((int) data[index+1] & 0xff) <<8 | ((int) data[index+2] & 0xff) << 16 | ((int) data[index+3] & 0xff) <<24;
            if (valueLen == 0) {
                index += 4;
                continue;
            }
            values.add(Arrays.copyOfRange(data, index+4, index+4+valueLen));
            index += valueLen + 4;
        }
        return values;
    }

    public List<String> getRawDataAsStringList() {
        if ( data == null || data[4] == ICommand.LOCK_DATA_COMMAND_TYPE_UNSET || (data[5]&ICommand.LOCK_DATA_FLAG_VALUE_TYPE_ARRAY) == 0) {
            return null;
        }
        List<String> values = new ArrayList<>();
        int index = getValueOffset();
        while (index+4 < data.length) {
            int valueLen = ((int) data[index] & 0xff) | ((int) data[index+1] & 0xff) <<8 | ((int) data[index+2] & 0xff) << 16 | ((int) data[index+3] & 0xff) <<24;
            if (valueLen == 0) {
                index += 4;
                continue;
            }
            values.add(new String(data, index+4, valueLen, StandardCharsets.UTF_8));
            index += valueLen + 4;
        }
        return values;
    }

    public Map<String, byte[]> getRawDataAsMap() {
        if ( data == null || data[4] == ICommand.LOCK_DATA_COMMAND_TYPE_UNSET || (data[5]&ICommand.LOCK_DATA_FLAG_VALUE_TYPE_KV) == 0) {
            return null;
        }
        Map<String, byte[]> values = new HashMap<>();
        int index = getValueOffset();
        while (index+4 < data.length) {
            int keyLen = ((int) data[index] & 0xff) | ((int) data[index+1] & 0xff) <<8 | ((int) data[index+2] & 0xff) << 16 | ((int) data[index+3] & 0xff) <<24;
            if (keyLen == 0) {
                index += 4;
                continue;
            }
            String key = new String(data, index+4, keyLen, StandardCharsets.UTF_8);
            index += keyLen + 4;
            int valueLen = ((int) data[index] & 0xff) | ((int) data[index+1] & 0xff) <<8 | ((int) data[index+2] & 0xff) << 16 | ((int) data[index+3] & 0xff) <<24;
            if (valueLen == 0) {
                index += 4;
                continue;
            }
            values.put(key, Arrays.copyOfRange(data, index+4, index+4+valueLen));
            index += valueLen + 4;
        }
        return values;
    }

    public Map<String, String> getRawDataAsStringMap() {
        if ( data == null || data[4] == ICommand.LOCK_DATA_COMMAND_TYPE_UNSET || (data[5]&ICommand.LOCK_DATA_FLAG_VALUE_TYPE_KV) == 0) {
            return null;
        }
        Map<String, String> values = new HashMap<>();
        int index = getValueOffset();
        while (index+4 < data.length) {
            int keyLen = ((int) data[index] & 0xff) | ((int) data[index+1] & 0xff) <<8 | ((int) data[index+2] & 0xff) << 16 | ((int) data[index+3] & 0xff) <<24;
            if (keyLen == 0) {
                index += 4;
                continue;
            }
            String key = new String(data, index+4, keyLen, StandardCharsets.UTF_8);
            index += keyLen + 4;
            int valueLen = ((int) data[index] & 0xff) | ((int) data[index+1] & 0xff) <<8 | ((int) data[index+2] & 0xff) << 16 | ((int) data[index+3] & 0xff) <<24;
            if (valueLen == 0) {
                index += 4;
                continue;
            }
            values.put(key, new String(data, index+4, valueLen, StandardCharsets.UTF_8));
            index += valueLen + 4;
        }
        return values;
    }
}
