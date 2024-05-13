package io.github.snower.jaslock;

import io.github.snower.jaslock.datas.LockResultData;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public abstract class AbstractExecution {
    protected final SlockDatabase database;
    protected byte[] lockKey;
    protected int timeout;
    protected int expried;
    protected short count;
    protected byte rCount;
    protected LockResultData currentLockData;

    public AbstractExecution(SlockDatabase database, byte[] lockKey, int timeout, int expried, short count, byte rCount) {
        this.database = database;
        if(lockKey.length > 16) {
            try {
                MessageDigest digest = MessageDigest.getInstance("MD5");
                this.lockKey = digest.digest(lockKey);
            } catch (NoSuchAlgorithmException e) {
                this.lockKey = Arrays.copyOfRange(lockKey, 0, 16);
            }
        } else {
            this.lockKey = new byte[16];
            System.arraycopy(lockKey, 0, this.lockKey, 16 - lockKey.length, lockKey.length);
        }
        this.timeout = timeout;
        this.expried = expried;
        this.count = count;
        this.rCount = rCount;
    }

    public AbstractExecution(SlockDatabase database, byte[] lockKey, int timeout, int expried) {
        this(database, lockKey, timeout, expried, (short)0, (byte)0);
    }

    public void setTimeout(short timeout) {
        this.timeout = (((int) timeout) & 0xffff) | (this.timeout & 0xffff0000);
    }

    public short getTimeout() {
        return (short) timeout;
    }

    public void setTimeoutFlag(short timeoutFlag) {
        this.timeout = ((((int) timeoutFlag) & 0xffff) << 16) | (this.timeout & 0xffff);
    }

    public void updateTimeoutFlag(short timeoutFlag) {
        this.timeout = ((((int) timeoutFlag) & 0xffff) << 16) | this.timeout;
    }

    public short getTimeoutFlag() {
        return (short) (timeout >> 16);
    }

    public void setExpried(short expried) {
        this.expried = (((int) expried) & 0xffff) | (this.expried & 0xffff0000);
    }

    public short getExpried() {
        return (short) expried;
    }

    public void setExpriedFlag(short expriedFlag) {
        this.expried = ((((int) expriedFlag) & 0xffff) << 16) | (this.expried & 0xffff);
    }

    public void updateExpriedFlag(short expriedFlag) {
        this.expried = ((((int) expriedFlag) & 0xffff) << 16) | this.expried;
    }

    public short getExpriedFlag() {
        return (short) (expried >> 16);
    }

    public void setCount(short count) {
        this.count = count;
    }

    public short getCount() {
        return count;
    }

    public void setRCount(byte rCount) {
        this.rCount = rCount;
    }

    public byte getRCount() {
        return rCount;
    }

    public LockResultData getCurrentLockData() {
        return currentLockData;
    }

    public byte[] getCurrentLockDataAsBytes() {
        if (currentLockData == null) {
            return null;
        }
        return currentLockData.getDataAsBytes();
    }

    public String getCurrentLockDataAsString() {
        if (currentLockData == null) {
            return null;
        }
        return currentLockData.getDataAsString();
    }

    public Long getCurrentLockDataAsLong() {
        if (currentLockData == null) {
            return 0L;
        }
        return currentLockData.getDataAsLong();
    }
}
