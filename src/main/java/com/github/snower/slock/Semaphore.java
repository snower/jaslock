package com.github.snower.slock;

import com.github.snower.slock.commands.ICommand;
import com.github.snower.slock.commands.LockCommand;
import com.github.snower.slock.exceptions.SlockException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Semaphore {
    private Database database;
    private byte[] semaphoreKey;
    private int timeout;
    private int expried;
    private short count;

    public Semaphore(Database database, byte[] semaphoreKey, short count, int timeout, int expried) {
        this.database = database;
        if(semaphoreKey.length > 16) {
            try {
                MessageDigest digest = MessageDigest.getInstance("MD5");
                this.semaphoreKey = digest.digest(semaphoreKey);
            } catch (NoSuchAlgorithmException e) {
                this.semaphoreKey = Arrays.copyOfRange(semaphoreKey, 0, 16);
            }
        } else {
            this.semaphoreKey = new byte[16];
            System.arraycopy(semaphoreKey, 0, this.semaphoreKey, 16 - semaphoreKey.length, semaphoreKey.length);
        }
        this.count = (short) (count > 0 ? count - 1 : 0);
        this.timeout = timeout;
        this.expried = expried;
    }

    public void acquire() throws SlockException {
        Lock flowLock = new Lock(database, semaphoreKey, LockCommand.genLockId(), timeout, expried, count, (byte) 0);
        flowLock.acquire();
    }

    public void release() throws SlockException {
        Lock flowLock = new Lock(database, semaphoreKey, new byte[16], timeout, expried, count, (byte) 0);
        flowLock.release(ICommand.UNLOCK_FLAG_UNLOCK_FIRST_LOCK_WHEN_UNLOCKED);
    }
}