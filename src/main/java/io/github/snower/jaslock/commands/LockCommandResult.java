package io.github.snower.jaslock.commands;

import io.github.snower.jaslock.datas.LockResultData;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class LockCommandResult extends CommandResult {
    protected byte flag;
    protected byte dbId;
    protected byte[] lockId;
    protected byte[] lockKey;
    protected short lCount;
    protected short count;
    protected byte lrCount;
    protected byte rCount;
    protected LockResultData lockResultData;

    public LockCommandResult() {
        super();
    }

    public byte getFlag() {
        return flag;
    }

    public byte getDbId() {
        return dbId;
    }

    public byte[] getLockId() {
        return lockId;
    }

    public byte[] getLockKey() {
        return lockKey;
    }

    public short getlCount() {
        return lCount;
    }

    public short getCount() {
        return count;
    }

    public byte getLrCount() {
        return lrCount;
    }

    public byte getrCount() {
        return rCount;
    }

    public LockResultData getLockResultData() {
        return lockResultData;
    }

    @Override
    public ICommand parseCommand(byte[] buf) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buf);
        magic = (byte) byteArrayInputStream.read();
        version = (byte) byteArrayInputStream.read();
        commandType = (byte) byteArrayInputStream.read();
        requestId = new byte[16];
        byteArrayInputStream.read(requestId, 0, 16);
        result = (byte) byteArrayInputStream.read();
        flag = (byte) byteArrayInputStream.read();
        dbId = (byte) byteArrayInputStream.read();
        lockId = new byte[16];
        byteArrayInputStream.read(lockId, 0, 16);
        lockKey = new byte[16];
        byteArrayInputStream.read(lockKey, 0, 16);
        lCount = (short) (((short) byteArrayInputStream.read()) | (((short) byteArrayInputStream.read()) << 8));
        count = (short) (((short) byteArrayInputStream.read()) | (((short) byteArrayInputStream.read()) << 8));
        lrCount = (byte) byteArrayInputStream.read();
        rCount = (byte) byteArrayInputStream.read();
        return this;
    }

    @Override
    public CommandResult loadCommandData(byte[] buf) {
        this.lockResultData = new LockResultData(buf);
        return this;
    }

    @Override
    public byte[] dumpCommand() {
        ByteArrayOutputStream byteArrayOutputStream = new CapacityByteArrayOutputStream(64);
        byteArrayOutputStream.write(MAGIC);
        byteArrayOutputStream.write(VERSION);
        byteArrayOutputStream.write(commandType);
        byteArrayOutputStream.write(requestId, 0, 16);
        byteArrayOutputStream.write(flag);
        byteArrayOutputStream.write(dbId);
        byteArrayOutputStream.write(lockId, 0, 16);
        byteArrayOutputStream.write(lockKey, 0, 16);
        byteArrayOutputStream.write(lCount & 0xff);
        byteArrayOutputStream.write((lCount >> 8) & 0xff);
        byteArrayOutputStream.write(count & 0xff);
        byteArrayOutputStream.write((count >> 8) & 0xff);
        byteArrayOutputStream.write(lrCount);
        byteArrayOutputStream.write(rCount);
        byteArrayOutputStream.write(new byte[4], 0, 4);
        return byteArrayOutputStream.toByteArray();
    }

    @Override
    public boolean hasExtraData() {
        return (flag & LOCK_FLAG_CONTAINS_DATA) != 0;
    }
}
