package io.github.snower.jaslock;

import io.github.snower.jaslock.callback.CallbackFuture;
import io.github.snower.jaslock.commands.ICommand;
import io.github.snower.jaslock.commands.LockCommand;
import io.github.snower.jaslock.commands.LockCommandResult;
import io.github.snower.jaslock.exceptions.*;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Consumer;

public class GroupEvent {
    private final SlockDatabase database;
    private final byte[] groupKey;
    private final long clientId;
    private long versionId;
    private final int timeout;
    private final int expried;

    public GroupEvent(SlockDatabase database, byte[] groupKey, long clientId, long versionId, int timeout, int expried) {
        this.database = database;
        this.groupKey = groupKey;
        this.clientId = clientId;
        this.versionId = versionId;
        this.timeout = timeout;
        this.expried = expried;
    }

    public GroupEvent(SlockDatabase database, String groupKey, long clientId, long versionId, int timeout, int expried) {
        this(database, groupKey.getBytes(StandardCharsets.UTF_8), clientId, versionId, timeout, expried);
    }

    public void clear() throws SlockException {
        byte[] lockId = encodeLockId(0, versionId);
        int timeout = this.timeout | (ICommand.TIMEOUT_FLAG_LESS_LOCK_VERSION_IS_LOCK_SUCCED << 16);
        Lock eventLock = new Lock(database, groupKey, lockId, timeout, expried, (short) 0, (byte) 0);
        eventLock.update();
    }

    public void set() throws SlockException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        for (int i = 0; i < 16; i++) {
            byteArrayOutputStream.write((byte) 0);
        }
        Lock eventLock = new Lock(database, groupKey, byteArrayOutputStream.toByteArray(), timeout, expried, (short) 0, (byte) 0);
        try {
            eventLock.releaseHead();
        } catch (LockUnlockedException ignored) {
        }
    }

    public boolean isSet() throws SlockException {
        Lock checkLock = new Lock(database, groupKey, LockCommand.genLockId(), 0, 0, (short) 0, (byte) 0);
        try {
            checkLock.acquire();
        } catch (LockTimeoutException e) {
            return false;
        }
        return true;
    }

    public void wakeup() throws SlockException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        for (int i = 0; i < 16; i++) {
            byteArrayOutputStream.write((byte) 0);
        }
        byte[] lockId = byteArrayOutputStream.toByteArray();
        int timeout = this.timeout | (ICommand.TIMEOUT_FLAG_LESS_LOCK_VERSION_IS_LOCK_SUCCED << 16);
        Lock eventLock = new Lock(database, groupKey, lockId, timeout, expried, (short) 0, (byte) 0);
        LockCommandResult lockCommandResult = eventLock.releaseHeadRetoLockWait();
        byte[] rlockId = lockCommandResult.getLockId();
        if (!Arrays.equals(lockId, rlockId)) {
            versionId = ((long) rlockId[0]) | (((long) rlockId[1])<<8) | (((long) rlockId[2])<<16) | (((long) rlockId[3])<<24)
                    | (((long) rlockId[4])<<32) | (((long) rlockId[5])<<40) | (((long) rlockId[6])<<48) | (((long) rlockId[7])<<56);
        }
    }

    public void wait(int timeout) throws SlockException {
        byte[] lockId = encodeLockId(clientId, versionId);
        Lock waitLock = new Lock(database, groupKey, lockId, timeout | (ICommand.TIMEOUT_FLAG_LESS_LOCK_VERSION_IS_LOCK_SUCCED << 16),
                0, (short) 0, (byte) 0);
        try {
            LockCommandResult lockCommandResult = waitLock.acquire((byte) 0);
            byte[] rlockId = lockCommandResult.getLockId();
            if (!Arrays.equals(lockId, rlockId)) {
                versionId = ((long) rlockId[0]) | (((long) rlockId[1])<<8) | (((long) rlockId[2])<<16) | (((long) rlockId[3])<<24)
                        | (((long) rlockId[4])<<32) | (((long) rlockId[5])<<40) | (((long) rlockId[6])<<48) | (((long) rlockId[7])<<56);
            }
        } catch (LockTimeoutException | ClientCommandTimeoutException ignored) {
            throw new EventWaitTimeoutException();
        }
    }

    public void waitAndTimeoutRetryClear(int timeout) throws SlockException {
        byte[] lockId = encodeLockId(clientId, versionId);
        Lock waitLock = new Lock(database, groupKey, lockId, timeout | (ICommand.TIMEOUT_FLAG_LESS_LOCK_VERSION_IS_LOCK_SUCCED << 16),
                0, (short) 0, (byte) 0);
        try {
            LockCommandResult lockCommandResult = waitLock.acquire((byte) 0);
            byte[] rlockId = lockCommandResult.getLockId();
            if (!Arrays.equals(lockId, rlockId)) {
                versionId = ((long) rlockId[0]) | (((long) rlockId[1])<<8) | (((long) rlockId[2])<<16) | (((long) rlockId[3])<<24)
                        | (((long) rlockId[4])<<32) | (((long) rlockId[5])<<40) | (((long) rlockId[6])<<48) | (((long) rlockId[7])<<56);
            }
        } catch (LockTimeoutException | ClientCommandTimeoutException ignored) {
            Lock eventLock = new Lock(database, encodeLockId(0, versionId), groupKey,
                    this.timeout | (ICommand.TIMEOUT_FLAG_LESS_LOCK_VERSION_IS_LOCK_SUCCED << 16), expried, (short) 0, (byte) 0);
            try {
                eventLock.acquire(ICommand.LOCK_FLAG_UPDATE_WHEN_LOCKED);
                try {
                    eventLock.release();
                } catch (SlockException ignored2) {
                }
                return;
            } catch (SlockException ignored2) {
            }
            throw new EventWaitTimeoutException();
        }
    }

    public CallbackFuture<Boolean> wait(int timeout, Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        byte[] lockId = encodeLockId(clientId, versionId);
        Lock waitLock = new Lock(database, groupKey, lockId, timeout | (ICommand.TIMEOUT_FLAG_LESS_LOCK_VERSION_IS_LOCK_SUCCED << 16),
                0, (short) 0, (byte) 0);
        CallbackFuture<Boolean> callbackFuture = new CallbackFuture<>(callback);
        waitLock.acquire((byte) 0, callbackCommandResult -> {
            try {
                LockCommandResult lockCommandResult = (LockCommandResult) callbackCommandResult.getResult();
                byte[] rlockId = lockCommandResult.getLockId();
                if (!Arrays.equals(lockId, rlockId)) {
                    versionId = ((long) rlockId[0]) | (((long) rlockId[1])<<8) | (((long) rlockId[2])<<16) | (((long) rlockId[3])<<24)
                            | (((long) rlockId[4])<<32) | (((long) rlockId[5])<<40) | (((long) rlockId[6])<<48) | (((long) rlockId[7])<<56);
                }
                callbackFuture.setResult(true);
            } catch (LockTimeoutException | ClientCommandTimeoutException ignored) {
                callbackFuture.setResult(false, new EventWaitTimeoutException());
            } catch (SlockException e) {
                callbackFuture.setResult(false, e);
            }
        });
        return callbackFuture;
    }

    public CallbackFuture<Boolean> waitAndTimeoutRetryClear(int timeout, Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        byte[] lockId = encodeLockId(clientId, versionId);
        Lock waitLock = new Lock(database, groupKey, lockId, timeout | (ICommand.TIMEOUT_FLAG_LESS_LOCK_VERSION_IS_LOCK_SUCCED << 16),
                0, (short) 0, (byte) 0);
        CallbackFuture<Boolean> callbackFuture = new CallbackFuture<>(callback);
        waitLock.acquire((byte) 0, callbackCommandResult -> {
            try {
                LockCommandResult lockCommandResult = (LockCommandResult) callbackCommandResult.getResult();
                byte[] rlockId = lockCommandResult.getLockId();
                if (!Arrays.equals(lockId, rlockId)) {
                    versionId = ((long) rlockId[0]) | (((long) rlockId[1]) << 8) | (((long) rlockId[2]) << 16) | (((long) rlockId[3]) << 24)
                            | (((long) rlockId[4]) << 32) | (((long) rlockId[5]) << 40) | (((long) rlockId[6]) << 48) | (((long) rlockId[7]) << 56);
                }
                callbackFuture.setResult(true);
            } catch (LockTimeoutException | ClientCommandTimeoutException ignored) {
                Lock eventLock = new Lock(database, encodeLockId(0, versionId), groupKey,
                        this.timeout | (ICommand.TIMEOUT_FLAG_LESS_LOCK_VERSION_IS_LOCK_SUCCED << 16), expried, (short) 0, (byte) 0);
                try {
                    eventLock.acquire(ICommand.LOCK_FLAG_UPDATE_WHEN_LOCKED);
                    try {
                        eventLock.release();
                    } catch (SlockException ignored2) {
                    }
                    callbackFuture.setResult(true);
                    return;
                } catch (SlockException ignored2) {}
                callbackFuture.setResult(false, new EventWaitTimeoutException());
            } catch (SlockException e) {
                callbackFuture.setResult(false, e);
            }
        });
        return callbackFuture;
    }

    private byte[] encodeLockId(long clientId, long versionId) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write((byte) (versionId & 0xff));
        byteArrayOutputStream.write((byte) ((versionId >> 8) & 0xff));
        byteArrayOutputStream.write((byte) ((versionId >> 16) & 0xff));
        byteArrayOutputStream.write((byte) ((versionId >> 24) & 0xff));
        byteArrayOutputStream.write((byte) ((versionId >> 32) & 0xff));
        byteArrayOutputStream.write((byte) ((versionId >> 40) & 0xff));
        byteArrayOutputStream.write((byte) ((versionId >> 48) & 0xff));
        byteArrayOutputStream.write((byte) ((versionId >> 56) & 0xff));
        byteArrayOutputStream.write((byte) (clientId & 0xff));
        byteArrayOutputStream.write((byte) ((clientId >> 8) & 0xff));
        byteArrayOutputStream.write((byte) ((clientId >> 16) & 0xff));
        byteArrayOutputStream.write((byte) ((clientId >> 24) & 0xff));
        byteArrayOutputStream.write((byte) ((clientId >> 32) & 0xff));
        byteArrayOutputStream.write((byte) ((clientId >> 40) & 0xff));
        byteArrayOutputStream.write((byte) ((clientId >> 48) & 0xff));
        byteArrayOutputStream.write((byte) ((clientId >> 56) & 0xff));
        return byteArrayOutputStream.toByteArray();
    }
}
