package io.github.snower.jaslock;

import io.github.snower.jaslock.callback.CallbackFuture;
import io.github.snower.jaslock.commands.LockCommand;
import io.github.snower.jaslock.exceptions.SlockException;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class MaxConcurrentFlow extends AbstractExecution {
    private byte priority;
    private volatile Lock flowLock;

    public MaxConcurrentFlow(SlockDatabase database, byte[] flowKey, short count, int timeout, int expried, byte priority) {
        super(database, flowKey, timeout, expried, (short) (count > 0 ? count - 1 : 0), (byte) 0);

        this.priority = priority;
    }

    public MaxConcurrentFlow(SlockDatabase database, byte[] flowKey, short count, int timeout, int expried) {
        this(database, flowKey, count, timeout, expried, (byte) 0);
    }

    public MaxConcurrentFlow(SlockDatabase database, String flowKey, short count, int timeout, int expried, byte priority) {
        this(database, flowKey.getBytes(StandardCharsets.UTF_8), count, timeout, expried, priority);
    }

    public MaxConcurrentFlow(SlockDatabase database, String flowKey, short count, int timeout, int expried) {
        this(database, flowKey.getBytes(StandardCharsets.UTF_8), count, timeout, expried);
    }

    public byte getPriority() {
        return priority;
    }

    public void setPriority(byte priority) {
        this.priority = priority;
    }

    public void acquire() throws SlockException {
        if (flowLock == null) {
            synchronized (this) {
                if (flowLock == null) {
                    final int timeout = priority > 0 ? this.timeout | 0x00100000 : this.timeout;
                    flowLock = new Lock(database, lockKey, LockCommand.genLockId(), timeout, expried, count, priority);
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
                    final int timeout = priority > 0 ? this.timeout | 0x00100000 : this.timeout;
                    flowLock = new Lock(database, lockKey, LockCommand.genLockId(), timeout, expried, count, priority);
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
                    final int timeout = priority > 0 ? this.timeout | 0x00100000 : this.timeout;
                    flowLock = new Lock(database, lockKey, LockCommand.genLockId(), timeout, expried, count, priority);
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
                    final int timeout = priority > 0 ? this.timeout | 0x00100000 : this.timeout;
                    flowLock = new Lock(database, lockKey, LockCommand.genLockId(), timeout, expried, count, priority);
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
