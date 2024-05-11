package io.github.snower.jaslock;

import io.github.snower.jaslock.callback.CallbackFuture;
import io.github.snower.jaslock.commands.LockCommand;
import io.github.snower.jaslock.exceptions.SlockException;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class MaxConcurrentFlow extends AbstractExecution {
    private volatile Lock flowLock;

    public MaxConcurrentFlow(SlockDatabase database, byte[] flowKey, short count, int timeout, int expried) {
        super(database, flowKey, timeout, expried, (short) (count > 0 ? count - 1 : 0), (byte) 0);
    }

    public MaxConcurrentFlow(SlockDatabase database, String flowKey, short count, int timeout, int expried) {
        this(database, flowKey.getBytes(StandardCharsets.UTF_8), count, timeout, expried);
    }

    public void acquire() throws SlockException {
        if (flowLock == null) {
            synchronized (this) {
                if (flowLock == null) {
                    flowLock = new Lock(database, lockKey, LockCommand.genLockId(), timeout, expried, count, (byte) 0);
                }
            }
        }
        flowLock.acquire();
    }

    public CallbackFuture<Boolean> acquire(Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        CallbackFuture<Boolean> callbackFuture = new CallbackFuture<>(callback);
        if (flowLock == null) {
            synchronized (this) {
                if (flowLock == null) {
                    flowLock = new Lock(database, lockKey, LockCommand.genLockId(), timeout, expried, count, (byte) 0);
                }
            }
        }
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
        if (flowLock == null) {
            synchronized (this) {
                if (flowLock == null) {
                    flowLock = new Lock(database, lockKey, LockCommand.genLockId(), timeout, expried, count, (byte) 0);
                }
            }
        }
        flowLock.release();
    }

    public CallbackFuture<Boolean> release(Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        CallbackFuture<Boolean> callbackFuture = new CallbackFuture<>(callback);
        if (flowLock == null) {
            synchronized (this) {
                if (flowLock == null) {
                    flowLock = new Lock(database, lockKey, LockCommand.genLockId(), timeout, expried, count, (byte) 0);
                }
            }
        }
        flowLock.release((byte) 0, callbackCommandResult -> {
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
