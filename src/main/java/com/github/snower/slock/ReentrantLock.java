package com.github.snower.slock;

import com.github.snower.slock.commands.LockCommand;
import com.github.snower.slock.exceptions.SlockException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class ReentrantLock {
    private Database database;
    private byte[] lockKey;
    private int timeout;
    private int expried;
    private Lock lock;

    public ReentrantLock(Database database, byte[] lockKey, int timeout, int expried) {
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