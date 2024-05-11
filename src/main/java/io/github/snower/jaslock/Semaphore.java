package io.github.snower.jaslock;

import io.github.snower.jaslock.callback.CallbackFuture;
import io.github.snower.jaslock.commands.ICommand;
import io.github.snower.jaslock.commands.LockCommand;
import io.github.snower.jaslock.exceptions.SlockException;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class Semaphore extends AbstractExecution {
    public Semaphore(SlockDatabase database, byte[] semaphoreKey, short count, int timeout, int expried) {
        super(database, semaphoreKey, timeout, expried, (short) (count > 0 ? count - 1 : 0), (byte) 0);
    }

    public Semaphore(SlockDatabase database, String semaphoreKey, short count, int timeout, int expried) {
        this(database, semaphoreKey.getBytes(StandardCharsets.UTF_8), count, timeout, expried);
    }

    public void acquire() throws SlockException {
        Lock flowLock = new Lock(database, lockKey, LockCommand.genLockId(), timeout, expried, count, (byte) 0);
        flowLock.acquire();
    }

    public CallbackFuture<Boolean> acquire(Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        CallbackFuture<Boolean> callbackFuture = new CallbackFuture<>(callback);
        Lock flowLock = new Lock(database, lockKey, LockCommand.genLockId(), timeout, expried, count, (byte) 0);
        flowLock.acquire((byte) 0, callbackCommandResult -> {
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
        Lock flowLock = new Lock(database, lockKey, new byte[16], timeout, expried, count, (byte) 0);
        flowLock.release(ICommand.UNLOCK_FLAG_UNLOCK_FIRST_LOCK_WHEN_UNLOCKED);
    }

    public CallbackFuture<Boolean> release(Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        CallbackFuture<Boolean> callbackFuture = new CallbackFuture<>(callback);
        Lock flowLock = new Lock(database, lockKey, new byte[16], timeout, expried, count, (byte) 0);
        flowLock.release(ICommand.UNLOCK_FLAG_UNLOCK_FIRST_LOCK_WHEN_UNLOCKED, callbackCommandResult -> {
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
