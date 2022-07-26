package io.github.snower.jaslock;

import io.github.snower.jaslock.callback.CallbackFuture;
import io.github.snower.jaslock.commands.CommandResult;
import io.github.snower.jaslock.commands.ICommand;
import io.github.snower.jaslock.commands.LockCommand;
import io.github.snower.jaslock.commands.LockCommandResult;
import io.github.snower.jaslock.callback.CallbackCommandResult;
import io.github.snower.jaslock.exceptions.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.function.Consumer;

public class Lock {
    private final SlockDatabase database;
    private byte[] lockKey;
    private byte[] lockId;
    private final int timeout;
    private final int expried;
    private final short count;
    private final byte rCount;

    public byte[] getLockKey() {
        return lockKey;
    }

    public byte[] getLockId() {
        return lockId;
    }

    public int getTimeout() {
        return timeout;
    }

    public int getExpried() {
        return expried;
    }

    public short getCount() {
        return count;
    }

    public byte getRCount() {
        return rCount;
    }

    public Lock(SlockDatabase database, byte[] lockKey, byte[] lockId, int timeout, int expried, short count, byte rCount) {
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
        LockCommand command = new LockCommand(ICommand.COMMAND_TYPE_LOCK, flag, database.getDbId(), lockKey,
                lockId, timeout, expried, count, rCount);
        LockCommandResult commandResult = (LockCommandResult) database.getClient().sendCommand(command);
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
        LockCommand command = new LockCommand(ICommand.COMMAND_TYPE_LOCK, flag, database.getDbId(), lockKey,
                lockId, timeout, expried, count, rCount);
        database.getClient().sendCommand(command, callbackCommandResult -> {
            try {
                CommandResult commandResult = callbackCommandResult.getResult();
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
        acquire((byte) 0);
    }

    public CallbackFuture<Boolean> acquire(Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        CallbackFuture<Boolean> callbackFuture = new CallbackFuture<>(callback);
        acquire((byte) 0, callbackCommandResult -> {
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
        LockCommand command = new LockCommand(ICommand.COMMAND_TYPE_UNLOCK, flag, database.getDbId(), lockKey,
                lockId, timeout, expried, count, rCount);
        LockCommandResult commandResult = (LockCommandResult) database.getClient().sendCommand(command);
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
        LockCommand command = new LockCommand(ICommand.COMMAND_TYPE_UNLOCK, flag, database.getDbId(), lockKey,
                lockId, timeout, expried, count, rCount);
        database.getClient().sendCommand(command, callbackCommandResult -> {
            try {
                CommandResult commandResult = callbackCommandResult.getResult();
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
        release((byte) 0);
    }

    public CallbackFuture<Boolean> release(Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        CallbackFuture<Boolean> callbackFuture = new CallbackFuture<>(callback);
        release((byte) 0, callbackCommandResult -> {
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
        try {
            acquire(ICommand.LOCK_FLAG_SHOW_WHEN_LOCKED);
        } catch (LockNotOwnException e) {
            return e.getCommandResult();
        }
        return null;
    }

    public void show(Consumer<CallbackCommandResult> callback) throws SlockException {
        acquire(ICommand.LOCK_FLAG_SHOW_WHEN_LOCKED, callbackCommandResult -> {
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
        try {
            acquire(ICommand.LOCK_FLAG_UPDATE_WHEN_LOCKED);
        } catch (LockLockedException ignored) {}
    }

    public void update(Consumer<CallbackCommandResult> callback) throws SlockException {
        acquire(ICommand.LOCK_FLAG_UPDATE_WHEN_LOCKED, callbackCommandResult -> {
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
        Lock lock = new Lock(database, lockKey, new byte[16], timeout, expried, count, (byte) 0);
        lock.release(ICommand.UNLOCK_FLAG_UNLOCK_FIRST_LOCK_WHEN_UNLOCKED);
    }

    public void releaseHead(Consumer<CallbackCommandResult> callback) throws SlockException {
        Lock lock = new Lock(database, lockKey, new byte[16], timeout, expried, count, (byte) 0);
        lock.release(ICommand.UNLOCK_FLAG_UNLOCK_FIRST_LOCK_WHEN_UNLOCKED, callback);
    }

    public LockCommandResult releaseHeadRetoLockWait() throws SlockException {
        return acquire((byte) (ICommand.UNLOCK_FLAG_UNLOCK_FIRST_LOCK_WHEN_UNLOCKED | ICommand.UNLOCK_FLAG_SUCCED_TO_LOCK_WAIT));
    }

    public void releaseHeadRetoLockWait(Consumer<CallbackCommandResult> callback) throws SlockException {
        acquire((byte) (ICommand.UNLOCK_FLAG_UNLOCK_FIRST_LOCK_WHEN_UNLOCKED | ICommand.UNLOCK_FLAG_SUCCED_TO_LOCK_WAIT), callback);
    }
}
