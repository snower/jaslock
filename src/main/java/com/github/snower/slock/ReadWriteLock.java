package com.github.snower.slock;

import com.github.snower.slock.commands.ICommand;
import com.github.snower.slock.commands.LockCommand;
import com.github.snower.slock.exceptions.LockLockedException;
import com.github.snower.slock.exceptions.SlockException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.NoSuchElementException;

public class ReadWriteLock {
    private Database database;
    private byte[] lockKey;
    private int timeout;
    private int expried;
    private LinkedList<Lock> readLocks;
    private Lock writeLock;

    public ReadWriteLock(Database database, byte[] lockKey, int timeout, int expried) {
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
        this.readLocks = new LinkedList<>();
    }

    public void acquireWrite() throws SlockException {
        synchronized (this) {
            if(writeLock == null) {
                writeLock = new Lock(database, lockKey, LockCommand.genLockId(), timeout, expried, (short) 0, (byte) 0);
            }
        }
        writeLock.acquire();
    }

    public void releaseWrite() throws SlockException {
        synchronized (this) {
            if(writeLock == null) {
                writeLock = new Lock(database, lockKey, LockCommand.genLockId(), timeout, expried, (short) 0, (byte) 0);
            }
        }
        writeLock.release();
    }

    public void acquireRead() throws SlockException {
        Lock readLock = new Lock(database, lockKey, LockCommand.genLockId(), timeout, expried, (short) 0xffff, (byte) 0);
        readLock.acquire();
        synchronized (this) {
            readLocks.add(readLock);
        }
    }

    public void releaseRead() throws SlockException {
        Lock readLock;
        synchronized (this) {
            try {
                readLock = readLocks.removeFirst();
            } catch (NoSuchElementException e) {
                return;
            }
        }
        readLock.release();
    }

    public void acquire() throws SlockException {
        acquireWrite();
    }

    public void release() throws SlockException {
        releaseWrite();
    }
}
