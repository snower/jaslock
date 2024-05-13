package io.github.snower.jaslock;

import io.github.snower.jaslock.callback.CallbackFuture;
import io.github.snower.jaslock.commands.ICommand;
import io.github.snower.jaslock.commands.LockCommand;
import io.github.snower.jaslock.commands.LockCommandResult;
import io.github.snower.jaslock.datas.LockData;
import io.github.snower.jaslock.datas.LockSetData;
import io.github.snower.jaslock.exceptions.*;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Consumer;

public class GroupEvent extends AbstractExecution {
    private final long clientId;
    private long versionId;

    public GroupEvent(SlockDatabase database, byte[] groupKey, long clientId, long versionId, int timeout, int expried) {
        super(database, groupKey, timeout, expried);
        this.clientId = clientId;
        this.versionId = versionId;
    }

    public GroupEvent(SlockDatabase database, String groupKey, long clientId, long versionId, int timeout, int expried) {
        this(database, groupKey.getBytes(StandardCharsets.UTF_8), clientId, versionId, timeout, expried);
    }

    public long getClientId() {
        return clientId;
    }

    public long getVersionId() {
        return versionId;
    }

    public void clear() throws SlockException {
        clear((LockData) null);
    }

    public void clear(byte[] data) throws SlockException {
        clear(data != null ? new LockSetData(data) : null);
    }

    public void clear(String data) throws SlockException {
        clear(data != null ? new LockSetData(data) : null);
    }

    public void clear(LockData lockData) throws SlockException {
        byte[] lockId = encodeLockId(0, versionId);
        int timeout = this.timeout | (ICommand.TIMEOUT_FLAG_LESS_LOCK_VERSION_IS_LOCK_SUCCED << 16);
        Lock eventLock = new Lock(database, lockKey, lockId, timeout, expried, (short) 0, (byte) 0);
        eventLock.update(lockData);
    }

    public CallbackFuture<Boolean> clear(Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        return clear((LockData) null, callback);
    }

    public CallbackFuture<Boolean> clear(byte[] data, Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        return clear(data != null ? new LockSetData(data) : null, callback);
    }

    public CallbackFuture<Boolean> clear(String data, Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        return clear(data != null ? new LockSetData(data) : null, callback);
    }

    public CallbackFuture<Boolean> clear(LockData lockData, Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        CallbackFuture<Boolean> callbackFuture = new CallbackFuture<>(callback);
        byte[] lockId = encodeLockId(0, versionId);
        int timeout = this.timeout | (ICommand.TIMEOUT_FLAG_LESS_LOCK_VERSION_IS_LOCK_SUCCED << 16);
        Lock eventLock = new Lock(database, lockKey, lockId, timeout, expried, (short) 0, (byte) 0);
        eventLock.update(lockData, callbackCommandResult -> {
            try {
                callbackCommandResult.getResult();
                callbackFuture.setResult(true);
            } catch (SlockException e) {
                callbackFuture.setResult(false, e);
            }
        });
        return callbackFuture;
    }

    public void set() throws SlockException {
        set((LockData) null);
    }

    public void set(byte[] data) throws SlockException {
        set(data != null ? new LockSetData(data) : null);
    }

    public void set(String data) throws SlockException {
        set(data != null ? new LockSetData(data) : null);
    }

    public void set(LockData lockData) throws SlockException {
        byte[] lockId = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        Lock eventLock = new Lock(database, lockKey, lockId, timeout, expried, (short) 0, (byte) 0);
        try {
            eventLock.releaseHead(lockData);
        } catch (LockUnlockedException ignored) {}
    }

    public CallbackFuture<Boolean> set(Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        return set((LockData) null, callback);
    }

    public CallbackFuture<Boolean> set(byte[] data, Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        return set(data != null ?  new LockSetData(data) : null, callback);
    }

    public CallbackFuture<Boolean> set(String data, Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        return set(data != null ?  new LockSetData(data) : null, callback);
    }

    public CallbackFuture<Boolean> set(LockData lockData, Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        CallbackFuture<Boolean> callbackFuture = new CallbackFuture<>(callback);
        byte[] lockId = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        Lock eventLock = new Lock(database, lockKey, lockId, timeout, expried, (short) 0, (byte) 0);
        eventLock.releaseHead(lockData, callbackCommandResult -> {
            try {
                callbackCommandResult.getResult();
                callbackFuture.setResult(true);
            } catch (LockUnlockedException ignored) {
                callbackFuture.setResult(true);
            } catch (SlockException e) {
                callbackFuture.setResult(false, e);
            }
        });
        return callbackFuture;
    }

    public boolean isSet() throws SlockException {
        Lock checkLock = new Lock(database, lockKey, LockCommand.genLockId(), 0, 0, (short) 0, (byte) 0);
        try {
            checkLock.acquire();
        } catch (LockTimeoutException e) {
            return false;
        }
        return true;
    }

    public CallbackFuture<Boolean> isSet(Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        CallbackFuture<Boolean> callbackFuture = new CallbackFuture<>(callback);
        Lock checkLock = new Lock(database, lockKey, LockCommand.genLockId(), 0, 0, (short) 0, (byte) 0);
        checkLock.acquire((byte) 0, callbackCommandResult -> {
            try {
                callbackCommandResult.getResult();
                callbackFuture.setResult(true);
            } catch (LockTimeoutException e) {
                callbackFuture.setResult(false);
            } catch (SlockException e) {
                callbackFuture.setResult(false, e);
            }
        });
        return callbackFuture;
    }

    public void wakeup() throws SlockException {
        wakeup((LockData) null);
    }

    public void wakeup(byte[] data) throws SlockException {
        wakeup(data != null ? new LockSetData(data) : null);
    }

    public void wakeup(String data) throws SlockException {
        wakeup(data != null ? new LockSetData(data) : null);
    }

    public void wakeup(LockData lockData) throws SlockException {
        byte[] lockId = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        int timeout = this.timeout | (ICommand.TIMEOUT_FLAG_LESS_LOCK_VERSION_IS_LOCK_SUCCED << 16);
        Lock eventLock = new Lock(database, lockKey, lockId, timeout, expried, (short) 0, (byte) 0);
        LockCommandResult lockCommandResult = eventLock.releaseHeadRetoLockWait(lockData);
        byte[] rlockId = lockCommandResult.getLockId();
        if (!Arrays.equals(lockId, rlockId)) {
            versionId = ((long) rlockId[0]) | (((long) rlockId[1])<<8) | (((long) rlockId[2])<<16) | (((long) rlockId[3])<<24)
                    | (((long) rlockId[4])<<32) | (((long) rlockId[5])<<40) | (((long) rlockId[6])<<48) | (((long) rlockId[7])<<56);
        }
    }

    public CallbackFuture<Boolean> wakeup(Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        return wakeup((LockData) null, callback);
    }

    public CallbackFuture<Boolean> wakeup(byte[] data, Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        return wakeup(data != null ? new LockSetData(data) : null, callback);
    }

    public CallbackFuture<Boolean> wakeup(String data, Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        return wakeup(data != null ? new LockSetData(data) : null, callback);
    }

    public CallbackFuture<Boolean> wakeup(LockData lockData, Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        CallbackFuture<Boolean> callbackFuture = new CallbackFuture<>(callback);
        byte[] lockId = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        int timeout = this.timeout | (ICommand.TIMEOUT_FLAG_LESS_LOCK_VERSION_IS_LOCK_SUCCED << 16);
        Lock eventLock = new Lock(database, lockKey, lockId, timeout, expried, (short) 0, (byte) 0);
        eventLock.releaseHeadRetoLockWait(lockData, callbackCommandResult -> {
            try {
                LockCommandResult lockCommandResult = (LockCommandResult) callbackCommandResult.getResult();
                byte[] rlockId = lockCommandResult.getLockId();
                if (!Arrays.equals(lockId, rlockId)) {
                    versionId = ((long) rlockId[0]) | (((long) rlockId[1])<<8) | (((long) rlockId[2])<<16) | (((long) rlockId[3])<<24)
                            | (((long) rlockId[4])<<32) | (((long) rlockId[5])<<40) | (((long) rlockId[6])<<48) | (((long) rlockId[7])<<56);
                }
                callbackFuture.setResult(true);
            } catch (SlockException e) {
                callbackFuture.setResult(false, e);
            }
        });
        return callbackFuture;
    }

    public void wait(int timeout) throws SlockException {
        byte[] lockId = encodeLockId(clientId, versionId);
        Lock waitLock = new Lock(database, lockKey, lockId, timeout | (ICommand.TIMEOUT_FLAG_LESS_LOCK_VERSION_IS_LOCK_SUCCED << 16),
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
        currentLockData = waitLock.getCurrentLockData();
    }

    public void waitAndTimeoutRetryClear(int timeout) throws SlockException {
        byte[] lockId = encodeLockId(clientId, versionId);
        Lock waitLock = new Lock(database, lockKey, lockId, timeout | (ICommand.TIMEOUT_FLAG_LESS_LOCK_VERSION_IS_LOCK_SUCCED << 16),
                0, (short) 0, (byte) 0);
        try {
            LockCommandResult lockCommandResult = waitLock.acquire((byte) 0);
            byte[] rlockId = lockCommandResult.getLockId();
            if (!Arrays.equals(lockId, rlockId)) {
                versionId = ((long) rlockId[0]) | (((long) rlockId[1])<<8) | (((long) rlockId[2])<<16) | (((long) rlockId[3])<<24)
                        | (((long) rlockId[4])<<32) | (((long) rlockId[5])<<40) | (((long) rlockId[6])<<48) | (((long) rlockId[7])<<56);
            }
        } catch (LockTimeoutException | ClientCommandTimeoutException ignored) {
            Lock eventLock = new Lock(database, encodeLockId(0, versionId), lockKey,
                    this.timeout | (ICommand.TIMEOUT_FLAG_LESS_LOCK_VERSION_IS_LOCK_SUCCED << 16), expried, (short) 0, (byte) 0);
            try {
                eventLock.acquire(ICommand.LOCK_FLAG_UPDATE_WHEN_LOCKED);
                try {
                    eventLock.release();
                } catch (SlockException ignored2) {
                }
                currentLockData = eventLock.getCurrentLockData();
                return;
            } catch (SlockException ignored2) {
            }
            throw new EventWaitTimeoutException();
        }
        currentLockData = waitLock.getCurrentLockData();
    }

    public CallbackFuture<Boolean> wait(int timeout, Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        byte[] lockId = encodeLockId(clientId, versionId);
        Lock waitLock = new Lock(database, lockKey, lockId, timeout | (ICommand.TIMEOUT_FLAG_LESS_LOCK_VERSION_IS_LOCK_SUCCED << 16),
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
                currentLockData = ((LockCommandResult) callbackCommandResult.getCommandResult()).getLockResultData();
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
        Lock waitLock = new Lock(database, lockKey, lockId, timeout | (ICommand.TIMEOUT_FLAG_LESS_LOCK_VERSION_IS_LOCK_SUCCED << 16),
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
                currentLockData = ((LockCommandResult) callbackCommandResult.getCommandResult()).getLockResultData();
                callbackFuture.setResult(true);
            } catch (LockTimeoutException | ClientCommandTimeoutException ignored) {
                Lock eventLock = new Lock(database, encodeLockId(0, versionId), lockKey,
                        this.timeout | (ICommand.TIMEOUT_FLAG_LESS_LOCK_VERSION_IS_LOCK_SUCCED << 16), expried, (short) 0, (byte) 0);
                try {
                    eventLock.acquire(ICommand.LOCK_FLAG_UPDATE_WHEN_LOCKED);
                    try {
                        eventLock.release();
                    } catch (SlockException ignored2) {
                    }
                    currentLockData = eventLock.getCurrentLockData();
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
