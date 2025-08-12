package io.github.snower.jaslock;

import io.github.snower.jaslock.callback.CallbackFuture;
import io.github.snower.jaslock.commands.LockCommand;
import io.github.snower.jaslock.exceptions.SlockException;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class PriorityLock extends AbstractExecution {
    private byte priority;
    private Lock lock;

    public PriorityLock(SlockDatabase database, byte[] lockKey, byte priority, int timeout, int expried, short count) {
        super(database, lockKey, timeout, expried, (short) (count > 0 ? count - 1 : 0), (byte) 0);

        this.priority = priority;
    }

    public PriorityLock(SlockDatabase database, byte[] lockKey, byte priority, int timeout, int expried) {
        this(database, lockKey, priority, timeout, expried, (short) 0);
    }

    public PriorityLock(SlockDatabase database, String lockKey, byte priority, int timeout, int expried, short count) {
        this(database, lockKey.getBytes(StandardCharsets.UTF_8), priority, timeout, expried, count);
    }

    public PriorityLock(SlockDatabase database, String lockKey, byte priority, int timeout, int expried) {
        this(database, lockKey.getBytes(StandardCharsets.UTF_8), priority, timeout, expried, (short) 0);
    }

    public byte getPriority() {
        return priority;
    }

    public void setPriority(byte priority) {
        this.priority = priority;
    }

    public void acquire() throws SlockException {
        synchronized (this) {
            if(lock == null) {
                lock = new Lock(database, lockKey, LockCommand.genLockId(), timeout | 0x00100000, expried, count, priority);
            }
        }
        lock.acquire();
    }

    public CallbackFuture<Boolean> acquire(Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        CallbackFuture<Boolean> callbackFuture = new CallbackFuture<>(callback);
        synchronized (this) {
            if(lock == null) {
                lock = new Lock(database, lockKey, LockCommand.genLockId(), timeout | 0x00100000, expried, count, priority);
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
                lock = new Lock(database, lockKey, LockCommand.genLockId(), timeout | 0x00100000, expried, count, priority);
            }
        }
        lock.release();
    }

    public CallbackFuture<Boolean> release(Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        CallbackFuture<Boolean> callbackFuture = new CallbackFuture<>(callback);
        synchronized (this) {
            if(lock == null) {
                lock = new Lock(database, lockKey, LockCommand.genLockId(), timeout | 0x00100000, expried, count, priority);
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

    public AutoCloseable with() throws SlockException {
        acquire();
        return this::release;
    }
}
