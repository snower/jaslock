package com.github.snower.slock.commands;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class LockCommand extends Command {
    private static volatile int lockIdIndex;

    protected byte flag;
    protected byte dbId;
    protected byte[] lockId;
    protected byte[] lockKey;
    protected int timeout;
    protected int expried;
    protected short count;
    protected byte rCount;

    public LockCommand(byte commandType, byte flag, byte dbId, byte[] lockKey, byte[] lockId, int timeout, int expried, short count, byte rCount) {
        super(commandType);

        this.flag = flag;
        this.dbId = dbId;
        this.lockKey = lockKey;
        if (lockId == null) {
            this.lockId = genLockId();
        } else {
            this.lockId = lockId;
        }
        this.timeout = timeout;
        this.expried = expried;
        this.count = count;
        this.rCount = rCount;
    }

    public LockCommand(byte commandType, byte dbId, byte[] lockKey, byte[] lockId, int timeout, int expried) {
        this(commandType, (byte) 0, dbId, lockKey, lockId, timeout, expried, (short) 0, (byte) 0);
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

    public int getTimeout() {
        return timeout;
    }

    public int getExpried() {
        return expried;
    }

    public short getCount() {
        return count;
    }

    public byte getrCount() {
        return rCount;
    }

    @Override
    public ICommand parseCommand(byte[] buf) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buf);
        try {
            magic = (byte) byteArrayInputStream.read();
            version = (byte) byteArrayInputStream.read();
            commandType = (byte) byteArrayInputStream.read();
            requestId = byteArrayInputStream.readNBytes(16);
            flag = (byte) byteArrayInputStream.read();
            dbId = (byte) byteArrayInputStream.read();
            lockId = byteArrayInputStream.readNBytes(16);
            lockKey = byteArrayInputStream.readNBytes(16);
            timeout = byteArrayInputStream.read() | (byteArrayInputStream.read() << 8)
                    | (byteArrayInputStream.read() << 16) | (byteArrayInputStream.read() << 24);
            expried = byteArrayInputStream.read() | (byteArrayInputStream.read() << 8)
                    | (byteArrayInputStream.read() << 16) | (byteArrayInputStream.read() << 24);
            count = (short) (((short) byteArrayInputStream.read()) | (((short) byteArrayInputStream.read()) << 8));
            rCount = (byte) byteArrayInputStream.read();
        } catch (IOException e) {
            return null;
        }
        return this;
    }

    @Override
    public byte[] dumpCommand() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(MAGIC);
        byteArrayOutputStream.write(VERSION);
        byteArrayOutputStream.write(commandType);
        byteArrayOutputStream.write(requestId, 0, 16);
        byteArrayOutputStream.write(flag);
        byteArrayOutputStream.write(dbId);
        byteArrayOutputStream.write(lockId, 0, 16);
        byteArrayOutputStream.write(lockKey, 0, 16);
        byteArrayOutputStream.write(timeout & 0xff);
        byteArrayOutputStream.write((timeout >> 8) & 0xff);
        byteArrayOutputStream.write((timeout >> 16) & 0xff);
        byteArrayOutputStream.write((timeout >> 24) & 0xff);
        byteArrayOutputStream.write(expried & 0xff);
        byteArrayOutputStream.write((expried >> 8) & 0xff);
        byteArrayOutputStream.write((expried >> 16) & 0xff);
        byteArrayOutputStream.write((expried >> 24) & 0xff);
        byteArrayOutputStream.write(count & 0xff);
        byteArrayOutputStream.write((count >> 8) & 0xff);
        byteArrayOutputStream.write(rCount);
        return byteArrayOutputStream.toByteArray();
    }

    public static byte[] genLockId() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        long timestamp = (new Date()).getTime();
        long randNumber = (new Random()).nextLong();
        long ri = (++lockIdIndex) & 0x7fffffffL;
        byteArrayOutputStream.write((byte) (timestamp >> 40) & 0xff);
        byteArrayOutputStream.write((byte) (timestamp >> 32) & 0xff);
        byteArrayOutputStream.write((byte) (timestamp >> 24) & 0xff);
        byteArrayOutputStream.write((byte) (timestamp >> 16) & 0xff);
        byteArrayOutputStream.write((byte) (timestamp >> 8) & 0xff);
        byteArrayOutputStream.write((byte) timestamp & 0xff);
        byteArrayOutputStream.write((byte) (randNumber >> 40) & 0xff);
        byteArrayOutputStream.write((byte) (randNumber >> 32) & 0xff);
        byteArrayOutputStream.write((byte) (randNumber >> 24) & 0xff);
        byteArrayOutputStream.write((byte) (randNumber >> 16) & 0xff);
        byteArrayOutputStream.write((byte) (randNumber >> 8) & 0xff);
        byteArrayOutputStream.write((byte) randNumber & 0xff);
        byteArrayOutputStream.write((byte) (ri >> 24) & 0xff);
        byteArrayOutputStream.write((byte) (ri >> 16) & 0xff);
        byteArrayOutputStream.write((byte) (ri >> 8) & 0xff);
        byteArrayOutputStream.write((byte) ri & 0xff);
        return byteArrayOutputStream.toByteArray();
    }

    @Override
    public boolean waiteWaiter() {
        if (waiter == null) {
            return false;
        }
        try {
            return waiter.tryAcquire(1, (timeout & 0xffffL) + 1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }
}
