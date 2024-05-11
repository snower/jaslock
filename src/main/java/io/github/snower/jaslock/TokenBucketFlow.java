package io.github.snower.jaslock;

import io.github.snower.jaslock.callback.CallbackFuture;
import io.github.snower.jaslock.commands.LockCommand;
import io.github.snower.jaslock.exceptions.LockTimeoutException;
import io.github.snower.jaslock.exceptions.SlockException;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Consumer;

public class TokenBucketFlow extends AbstractExecution {
    private final double period;

    public TokenBucketFlow(SlockDatabase database, byte[] flowKey, short count, int timeout, double period) {
        super(database, flowKey, timeout, 0, (short) (count > 0 ? count - 1 : 0), (byte) 0);
        this.period = period;
    }

    public TokenBucketFlow(SlockDatabase database, String flowKey, short count, int timeout, double period) {
        this(database, flowKey.getBytes(StandardCharsets.UTF_8), count, timeout, period);
    }

    public void acquire() throws SlockException {
        Lock flowLock;
        if(period < 3) {
            synchronized (this) {
                int expried = (int)Math.ceil(period * 1000) | 0x04000000;
                expried = expried | (this.expried & 0xffff0000);
                flowLock = new Lock(database, lockKey, LockCommand.genLockId(), timeout, expried, count, (byte) 0);
            }
            flowLock.acquire();
            return;
        }

        synchronized (this) {
            long now = new Date().getTime() / 1000L;
            int expried = (int) (((long)Math.ceil(period)) - (now % ((long) Math.ceil((period)))));
            expried = expried | (this.expried & 0xffff0000);
            flowLock = new Lock(database, lockKey, LockCommand.genLockId(), 0, expried, count, (byte) 0);
        }

        try {
            flowLock.acquire();
        } catch (LockTimeoutException e) {
            int expried = (int) Math.ceil(period);
            expried = expried | (this.expried & 0xffff0000);
            flowLock = new Lock(database, lockKey, LockCommand.genLockId(), timeout, expried, count, (byte) 0);
            flowLock.acquire();
        }
    }

    public CallbackFuture<Boolean> acquire(Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        CallbackFuture<Boolean> callbackFuture = new CallbackFuture<>(callback);
        Lock flowLock;
        if(period < 3) {
            synchronized (this) {
                int expried = (int)Math.ceil(period * 1000) | 0x04000000;
                expried = expried | (this.expried & 0xffff0000);
                flowLock = new Lock(database, lockKey, LockCommand.genLockId(), timeout, expried, count, (byte) 0);
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

        synchronized (this) {
            long now = new Date().getTime() / 1000L;
            int expried = (int) (((long)Math.ceil(period)) - (now % ((long) Math.ceil((period)))));
            expried = expried | (this.expried & 0xffff0000);
            flowLock = new Lock(database, lockKey, LockCommand.genLockId(), 0, expried, count, (byte) 0);
        }

        flowLock.acquire((byte) 0, callbackCommandResult -> {
            try {
                callbackCommandResult.getResult();
                callbackFuture.setResult(true);
            } catch (LockTimeoutException e) {
                int expried = (int) Math.ceil(period);
                expried = expried | (this.expried & 0xffff0000);
                Lock reflowLock = new Lock(database, lockKey, LockCommand.genLockId(), timeout, expried, count, (byte) 0);
                try {
                    reflowLock.acquire(recallbackCommandResult -> {
                        try {
                            recallbackCommandResult.getResult();
                            callbackFuture.setResult(true);
                        } catch (SlockException ex) {
                            callbackFuture.setResult(false, e);
                        }
                    });
                } catch (SlockException ex) {
                    callbackFuture.setResult(false, e);
                }
            } catch (SlockException e) {
                callbackFuture.setResult(false, e);
            }
        });
        return callbackFuture;
    }
}
