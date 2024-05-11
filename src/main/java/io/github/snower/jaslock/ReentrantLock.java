package io.github.snower.jaslock;

import io.github.snower.jaslock.callback.CallbackFuture;
import io.github.snower.jaslock.commands.LockCommand;
import io.github.snower.jaslock.exceptions.SlockException;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class ReentrantLock extends AbstractExecution {
    private Lock lock;

    public ReentrantLock(SlockDatabase database, byte[] lockKey, int timeout, int expried) {
        super(database, lockKey, timeout, expried);
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

    public CallbackFuture<Boolean> acquire(Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        CallbackFuture<Boolean> callbackFuture = new CallbackFuture<>(callback);
        synchronized (this) {
            if(lock == null) {
                lock = new Lock(database, lockKey, LockCommand.genLockId(), timeout, expried, (short) 0, (byte) 0xff);
            }
        }
        lock.acquire((byte) 0, callbackCommandResult -> {
            try {
                callbackCommandResult.getResult();
                callbackFuture.setResult(true);
            } catch (SlockException e) {
                callbackFuture.setResult(false, e);
            }
        });
        return callbackFuture;
    }

    public void release() throws SlockException {
        synchronized (this) {
            if(lock == null) {
                lock = new Lock(database, lockKey, LockCommand.genLockId(), timeout, expried, (short) 0, (byte) 0xff);
            }
        }
        lock.release();
    }

    public CallbackFuture<Boolean> release(Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        CallbackFuture<Boolean> callbackFuture = new CallbackFuture<>(callback);
        synchronized (this) {
            if(lock == null) {
                lock = new Lock(database, lockKey, LockCommand.genLockId(), timeout, expried, (short) 0, (byte) 0xff);
            }
        }
        lock.release((byte) 0, callbackCommandResult -> {
            try {
                callbackCommandResult.getResult();
                callbackFuture.setResult(true);
            } catch (SlockException e) {
                callbackFuture.setResult(false, e);
            }
        });
        return callbackFuture;
    }
}
