package io.github.snower.jaslock;

import io.github.snower.jaslock.callback.CallbackFuture;
import io.github.snower.jaslock.commands.LockCommand;
import io.github.snower.jaslock.exceptions.SlockException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

public class ReadWriteLock {
    private final SlockDatabase database;
    private byte[] lockKey;
    private final int timeout;
    private final int expried;
    private final LinkedList<Lock> readLocks;
    private Lock writeLock;

    public ReadWriteLock(SlockDatabase database, byte[] lockKey, int timeout, int expried) {
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

    public ReadWriteLock(SlockDatabase database, String lockKey, int timeout, int expried) {
        this(database, lockKey.getBytes(StandardCharsets.UTF_8), timeout, expried);
    }

    public void acquireWrite() throws SlockException {
        synchronized (this) {
            if(writeLock == null) {
                writeLock = new Lock(database, lockKey, LockCommand.genLockId(), timeout, expried, (short) 0, (byte) 0);
            }
        }
        writeLock.acquire();
    }

    public CallbackFuture<Boolean> acquireWrite(Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        CallbackFuture<Boolean> callbackFuture = new CallbackFuture<>(callback);
        synchronized (this) {
            if(writeLock == null) {
                writeLock = new Lock(database, lockKey, LockCommand.genLockId(), timeout, expried, (short) 0, (byte) 0);
            }
        }
        writeLock.acquire((byte) 0, callbackCommandResult -> {
            try {
                callbackCommandResult.getResult();
                callbackFuture.setResult(true);
            } catch (SlockException e) {
                callbackFuture.setResult(false, e);
            }
        });
        return callbackFuture;
    }

    public void releaseWrite() throws SlockException {
        synchronized (this) {
            if(writeLock == null) {
                writeLock = new Lock(database, lockKey, LockCommand.genLockId(), timeout, expried, (short) 0, (byte) 0);
            }
        }
        writeLock.release();
    }

    public CallbackFuture<Boolean> releaseWrite(Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        CallbackFuture<Boolean> callbackFuture = new CallbackFuture<>(callback);
        synchronized (this) {
            if(writeLock == null) {
                writeLock = new Lock(database, lockKey, LockCommand.genLockId(), timeout, expried, (short) 0, (byte) 0);
            }
        }
        writeLock.release((byte) 0, callbackCommandResult -> {
            try {
                callbackCommandResult.getResult();
                callbackFuture.setResult(true);
            } catch (SlockException e) {
                callbackFuture.setResult(false, e);
            }
        });
        return callbackFuture;
    }

    public void acquireRead() throws SlockException {
        Lock readLock = new Lock(database, lockKey, LockCommand.genLockId(), timeout, expried, (short) 0xffff, (byte) 0);
        readLock.acquire();
        synchronized (this) {
            readLocks.add(readLock);
        }
    }

    public CallbackFuture<Boolean> acquireRead(Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        CallbackFuture<Boolean> callbackFuture = new CallbackFuture<>(callback);
        Lock readLock = new Lock(database, lockKey, LockCommand.genLockId(), timeout, expried, (short) 0xffff, (byte) 0);
        readLock.acquire((byte) 0, callbackCommandResult -> {
            try {
                callbackCommandResult.getResult();
                synchronized (this) {
                    readLocks.add(readLock);
                }
                callbackFuture.setResult(true);
            } catch (SlockException e) {
                callbackFuture.setResult(false, e);
            }
        });
        return callbackFuture;
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

    public CallbackFuture<Boolean> releaseRead(Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        CallbackFuture<Boolean> callbackFuture = new CallbackFuture<>(callback);
        Lock readLock;
        synchronized (this) {
            try {
                readLock = readLocks.removeFirst();
            } catch (NoSuchElementException e) {
                callbackFuture.setResult(true);
                return callbackFuture;
            }
        }
        readLock.release((byte) 0, callbackCommandResult -> {
            try {
                callbackCommandResult.getResult();
                callbackFuture.setResult(true);
            } catch (SlockException e) {
                callbackFuture.setResult(false, e);
            }
        });
        return callbackFuture;
    }

    public void acquire() throws SlockException {
        acquireWrite();
    }

    public CallbackFuture<Boolean> acquire(Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        return acquireWrite(callback);
    }

    public void release() throws SlockException {
        releaseWrite();
    }

    public CallbackFuture<Boolean> release(Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        return releaseWrite(callback);
    }
}
