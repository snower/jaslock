package io.github.snower.jaslock;

import io.github.snower.jaslock.commands.LockCommand;
import io.github.snower.jaslock.exceptions.SlockException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class ReentrantLock {
    private SlockDatabase database;
    private byte[] lockKey;
    private int timeout;
    private int expried;
    private Lock lock;

    public ReentrantLock(SlockDatabase database, byte[] lockKey, int timeout, int expried) {
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
    }

    public ReentrantLock(SlockDatabase database, String lockKey, int timeout, int expried) {
        this(database, lockKey.getBytes(StandardCharsets.UTF_8), timeout, expried);
    }

    public void acquire() throws SlockException {
        synchronized (this) {
            if(lock == null) {
                lock = new Lock(database, lockKey, LockCommand.genLockId(), timeout, expried, (short) 0, (byte) 0xff);
            }
        }
        lock.acquire();
    }

    public void release() throws SlockException {
        synchronized (this) {
            if(lock == null) {
                lock = new Lock(database, lockKey, LockCommand.genLockId(), timeout, expried, (short) 0, (byte) 0xff);
            }
        }
        lock.release();
    }
}
