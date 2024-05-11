package io.github.snower.jaslock;

import io.github.snower.jaslock.callback.CallbackFuture;
import io.github.snower.jaslock.commands.CommandResult;
import io.github.snower.jaslock.commands.ICommand;
import io.github.snower.jaslock.commands.LockCommand;
import io.github.snower.jaslock.commands.LockCommandResult;
import io.github.snower.jaslock.callback.CallbackCommandResult;
import io.github.snower.jaslock.datas.LockData;
import io.github.snower.jaslock.exceptions.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.function.Consumer;

public class Lock extends AbstractExecution {
    private byte[] lockId;

    public byte[] getLockKey() {
        return lockKey;
    }

    public byte[] getLockId() {
        return lockId;
    }

    public Lock(SlockDatabase database, byte[] lockKey, byte[] lockId, int timeout, int expried, short count, byte rCount) {
        super(database, lockKey, timeout, expried, count, rCount);

        if(lockId == null) {
            this.lockId = LockCommand.genLockId();
        } else {
            if(lockId.length > 16) {
                try {
                    MessageDigest digest = MessageDigest.getInstance("MD5");
                    this.lockId = digest.digest(lockId);
                } catch (NoSuchAlgorithmException e) {
                    this.lockId = Arrays.copyOfRange(lockId, 0, 16);
                }
            } else {
                this.lockId = new byte[16];
                System.arraycopy(lockId, 0, this.lockId, 16 - lockId.length, lockId.length);
            }
        }
        this.timeout = timeout;
        this.expried = expried;
        this.count = count;
        this.rCount = rCount;
    }

    public Lock(SlockDatabase database, byte[] lockKey, int timeout, int expried) {
        this(database, lockKey, null, timeout, expried, (short) 0, (byte) 0);
    }

    public Lock(SlockDatabase database, String lockKey, int timeout, int expried) {
        this(database, lockKey.getBytes(StandardCharsets.UTF_8), null, timeout, expried, (short) 0, (byte) 0);
    }

    public LockCommandResult acquire(byte flag) throws SlockException {
        return acquire(flag, (LockData) null);
    }

    public LockCommandResult acquire(byte flag, LockData lockData) throws SlockException {
        LockCommand command = new LockCommand(ICommand.COMMAND_TYPE_LOCK, lockData != null ? (byte) (flag | ICommand.LOCK_FLAG_CONTAINS_DATA) :  flag,
                database.getDbId(), lockKey, lockId, timeout, expried, count, rCount, lockData);
        LockCommandResult commandResult = (LockCommandResult) database.getClient().sendCommand(command);
        currentLockData = commandResult.getLockResultData();
        if(commandResult.getResult() == ICommand.COMMAND_RESULT_SUCCED)  {
            return commandResult;
        }

        switch (commandResult.getResult()) {
            case ICommand.COMMAND_RESULT_LOCKED_ERROR:
                throw new LockLockedException(command, commandResult);
            case ICommand.COMMAND_RESULT_UNLOCK_ERROR:
                throw new LockUnlockedException(command, commandResult);
            case ICommand.COMMAND_RESULT_UNOWN_ERROR:
                throw new LockNotOwnException(command, commandResult);
            case ICommand.COMMAND_RESULT_TIMEOUT:
                throw new LockTimeoutException(command, commandResult);
            default:
                throw new LockException(command, commandResult);
        }
    }

    public void acquire(byte flag, Consumer<CallbackCommandResult> callback) throws SlockException {
        acquire(flag, null, callback);
    }

    public void acquire(byte flag, LockData lockData, Consumer<CallbackCommandResult> callback) throws SlockException {
        LockCommand command = new LockCommand(ICommand.COMMAND_TYPE_LOCK, lockData != null ? (byte) (flag | ICommand.LOCK_FLAG_CONTAINS_DATA) :  flag,
                database.getDbId(), lockKey, lockId, timeout, expried, count, rCount, lockData);
        database.getClient().sendCommand(command, callbackCommandResult -> {
            try {
                CommandResult commandResult = callbackCommandResult.getResult();
                currentLockData = ((LockCommandResult) commandResult).getLockResultData();
                if(commandResult.getResult() == ICommand.COMMAND_RESULT_SUCCED)  {
                    callback.accept(new CallbackCommandResult(command, commandResult, null));
                    return;
                }

                switch (commandResult.getResult()) {
                    case ICommand.COMMAND_RESULT_LOCKED_ERROR:
                        callback.accept(new CallbackCommandResult(command, commandResult, new LockLockedException(command, commandResult)));
                        return;
                    case ICommand.COMMAND_RESULT_UNLOCK_ERROR:
                        callback.accept(new CallbackCommandResult(command, commandResult, new LockUnlockedException(command, commandResult)));
                        return;
                    case ICommand.COMMAND_RESULT_UNOWN_ERROR:
                        callback.accept(new CallbackCommandResult(command, commandResult, new LockNotOwnException(command, commandResult)));
                        return;
                    case ICommand.COMMAND_RESULT_TIMEOUT:
                        callback.accept(new CallbackCommandResult(command, commandResult, new LockTimeoutException(command, commandResult)));
                        return;
                    default:
                        callback.accept(new CallbackCommandResult(command, commandResult, new LockException(command, commandResult)));
                }
            } catch (SlockException e) {
                callback.accept(new CallbackCommandResult(command, null, e));
            }
        });
    }

    public void acquire() throws SlockException {
        acquire((byte) 0, (LockData) null);
    }

    public void acquire(LockData lockData) throws SlockException {
        acquire((byte) 0, lockData);
    }

    public CallbackFuture<Boolean> acquire(Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        return acquire(null, callback);
    }

    public CallbackFuture<Boolean> acquire(LockData lockData, Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        CallbackFuture<Boolean> callbackFuture = new CallbackFuture<>(callback);
        acquire((byte) 0, lockData, callbackCommandResult -> {
            try {
                callbackCommandResult.getResult();
                callbackFuture.setResult(true);
            } catch (SlockException e) {
                callbackFuture.setResult(false, e);
            }
        });
        return callbackFuture;
    }

    public LockCommandResult release(byte flag) throws SlockException {
        return release(flag, (LockData) null);
    }

    public LockCommandResult release(byte flag, LockData lockData) throws SlockException {
        LockCommand command = new LockCommand(ICommand.COMMAND_TYPE_UNLOCK, lockData != null ? (byte) (flag | ICommand.UNLOCK_FLAG_CONTAINS_DATA) :  flag,
                database.getDbId(), lockKey, lockId, timeout, expried, count, rCount, lockData);
        LockCommandResult commandResult = (LockCommandResult) database.getClient().sendCommand(command);
        currentLockData = commandResult.getLockResultData();
        if(commandResult.getResult() == ICommand.COMMAND_RESULT_SUCCED)  {
            return commandResult;
        }

        switch (commandResult.getResult()) {
            case ICommand.COMMAND_RESULT_LOCKED_ERROR:
                throw new LockLockedException(command, commandResult);
            case ICommand.COMMAND_RESULT_UNLOCK_ERROR:
                throw new LockUnlockedException(command, commandResult);
            case ICommand.COMMAND_RESULT_UNOWN_ERROR:
                throw new LockNotOwnException(command, commandResult);
            case ICommand.COMMAND_RESULT_TIMEOUT:
                throw new LockTimeoutException(command, commandResult);
            default:
                throw new LockException(command, commandResult);
        }
    }

    public void release(byte flag, Consumer<CallbackCommandResult> callback) throws SlockException {
        release(flag, null, callback);
    }

    public void release(byte flag, LockData lockData, Consumer<CallbackCommandResult> callback) throws SlockException {
        LockCommand command = new LockCommand(ICommand.COMMAND_TYPE_UNLOCK, lockData != null ? (byte) (flag | ICommand.UNLOCK_FLAG_CONTAINS_DATA) :  flag,
                database.getDbId(), lockKey, lockId, timeout, expried, count, rCount, lockData);
        database.getClient().sendCommand(command, callbackCommandResult -> {
            try {
                CommandResult commandResult = callbackCommandResult.getResult();
                currentLockData = ((LockCommandResult) commandResult).getLockResultData();
                if(commandResult.getResult() == ICommand.COMMAND_RESULT_SUCCED)  {
                    callback.accept(new CallbackCommandResult(command, commandResult, null));
                    return;
                }

                switch (commandResult.getResult()) {
                    case ICommand.COMMAND_RESULT_LOCKED_ERROR:
                        callback.accept(new CallbackCommandResult(command, commandResult, new LockLockedException(command, commandResult)));
                        return;
                    case ICommand.COMMAND_RESULT_UNLOCK_ERROR:
                        callback.accept(new CallbackCommandResult(command, commandResult, new LockUnlockedException(command, commandResult)));
                        return;
                    case ICommand.COMMAND_RESULT_UNOWN_ERROR:
                        callback.accept(new CallbackCommandResult(command, commandResult, new LockNotOwnException(command, commandResult)));
                        return;
                    case ICommand.COMMAND_RESULT_TIMEOUT:
                        callback.accept(new CallbackCommandResult(command, commandResult, new LockTimeoutException(command, commandResult)));
                        return;
                    default:
                        callback.accept(new CallbackCommandResult(command, commandResult, new LockException(command, commandResult)));
                }
            } catch (SlockException e) {
                callback.accept(new CallbackCommandResult(command, null, e));
            }
        });
    }


    public void release() throws SlockException {
        release((byte) 0, (LockData) null);
    }

    public void release(LockData lockData) throws SlockException {
        release((byte) 0, lockData);
    }

    public CallbackFuture<Boolean> release(Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        return release(null, callback);
    }

    public CallbackFuture<Boolean> release(LockData lockData, Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        CallbackFuture<Boolean> callbackFuture = new CallbackFuture<>(callback);
        release((byte) 0, lockData, callbackCommandResult -> {
            try {
                callbackCommandResult.getResult();
                callbackFuture.setResult(true);
            } catch (SlockException e) {
                callbackFuture.setResult(false, e);
            }
        });
        return callbackFuture;
    }

    public CommandResult show() throws SlockException {
        return show((LockData) null);
    }

    public CommandResult show(LockData lockData) throws SlockException {
        try {
            acquire(ICommand.LOCK_FLAG_SHOW_WHEN_LOCKED, lockData);
        } catch (LockNotOwnException e) {
            return e.getCommandResult();
        }
        return null;
    }

    public void show(Consumer<CallbackCommandResult> callback) throws SlockException {
        show(null, callback);
    }

    public void show(LockData lockData, Consumer<CallbackCommandResult> callback) throws SlockException {
        acquire(ICommand.LOCK_FLAG_SHOW_WHEN_LOCKED, lockData, callbackCommandResult -> {
            try {
                callbackCommandResult.getResult();
                callback.accept(new CallbackCommandResult(callbackCommandResult.getCommand(), null, null));
            } catch (LockNotOwnException e) {
                callback.accept(new CallbackCommandResult(callbackCommandResult.getCommand(), callbackCommandResult.getCommandResult(), null));
            } catch (SlockException e) {
                callback.accept(new CallbackCommandResult(callbackCommandResult.getCommand(), callbackCommandResult.getCommandResult(), e));
            }
        });
    }

    public void update() throws SlockException {
        update((LockData) null);
    }

    public void update(LockData lockData) throws SlockException {
        try {
            acquire(ICommand.LOCK_FLAG_UPDATE_WHEN_LOCKED, lockData);
        } catch (LockLockedException ignored) {}
    }

    public void update(Consumer<CallbackCommandResult> callback) throws SlockException {
        update(null, callback);
    }

    public void update(LockData lockData, Consumer<CallbackCommandResult> callback) throws SlockException {
        acquire(ICommand.LOCK_FLAG_UPDATE_WHEN_LOCKED, lockData, callbackCommandResult -> {
            try {
                CommandResult commandResult = callbackCommandResult.getResult();
                callback.accept(new CallbackCommandResult(callbackCommandResult.getCommand(), commandResult, null));
            } catch (LockLockedException e) {
                callback.accept(new CallbackCommandResult(callbackCommandResult.getCommand(), callbackCommandResult.getCommandResult(), null));
            } catch (SlockException e) {
                callback.accept(new CallbackCommandResult(callbackCommandResult.getCommand(), callbackCommandResult.getCommandResult(), e));
            }
        });
    }

    public void releaseHead() throws SlockException {
        releaseHead((LockData) null);
    }

    public void releaseHead(LockData lockData) throws SlockException {
        Lock lock = new Lock(database, lockKey, new byte[16], timeout, expried, count, (byte) 0);
        lock.release(ICommand.UNLOCK_FLAG_UNLOCK_FIRST_LOCK_WHEN_UNLOCKED, lockData);
    }

    public void releaseHead(Consumer<CallbackCommandResult> callback) throws SlockException {
        releaseHead(null, callback);
    }

    public void releaseHead(LockData lockData, Consumer<CallbackCommandResult> callback) throws SlockException {
        Lock lock = new Lock(database, lockKey, new byte[16], timeout, expried, count, (byte) 0);
        lock.release(ICommand.UNLOCK_FLAG_UNLOCK_FIRST_LOCK_WHEN_UNLOCKED, lockData, callback);
    }

    public LockCommandResult releaseHeadRetoLockWait() throws SlockException {
        return releaseHeadRetoLockWait((LockData) null);
    }

    public LockCommandResult releaseHeadRetoLockWait(LockData lockData) throws SlockException {
        return acquire((byte) (ICommand.UNLOCK_FLAG_UNLOCK_FIRST_LOCK_WHEN_UNLOCKED | ICommand.UNLOCK_FLAG_SUCCED_TO_LOCK_WAIT), lockData);
    }

    public void releaseHeadRetoLockWait(Consumer<CallbackCommandResult> callback) throws SlockException {
        releaseHeadRetoLockWait(null, callback);
    }

    public void releaseHeadRetoLockWait(LockData lockData, Consumer<CallbackCommandResult> callback) throws SlockException {
        acquire((byte) (ICommand.UNLOCK_FLAG_UNLOCK_FIRST_LOCK_WHEN_UNLOCKED | ICommand.UNLOCK_FLAG_SUCCED_TO_LOCK_WAIT), lockData, callback);
    }
}
