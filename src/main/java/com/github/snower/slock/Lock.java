package com.github.snower.slock;

import com.github.snower.slock.commands.ICommand;
import com.github.snower.slock.commands.LockCommand;
import com.github.snower.slock.commands.LockCommandResult;
import com.github.snower.slock.exceptions.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Lock {
    private Database database;
    private byte[] lockKey;
    private byte[] lockId;
    private int timeout;
    private int expried;
    private short count;
    private byte rCount;

    public Lock(Database database, byte[] lockKey, byte[] lockId, int timeout, int expried, short count, byte rCount) {
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

    public Lock(Database database, byte[] lockKey, int timeout, int expried) {
        this(database, lockKey, null, timeout, expried, (short) 0, (byte) 0);
    }

    public void acquire(byte flag) throws SlockException {
        LockCommand command = new LockCommand(ICommand.COMMAND_TYPE_LOCK, flag, database.getDbId(), lockKey,
                lockId, timeout, expried, count, rCount);
        LockCommandResult commandResult = (LockCommandResult) database.getClient().sendCommand(command);
        if(commandResult.getResult() == ICommand.COMMAND_RESULT_SUCCED)  {
            return;
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

    public void acquire() throws SlockException {
        acquire((byte) 0);
    }

    public void release(byte flag) throws SlockException {
        LockCommand command = new LockCommand(ICommand.COMMAND_TYPE_UNLOCK, flag, database.getDbId(), lockKey,
                lockId, timeout, expried, count, rCount);
        LockCommandResult commandResult = (LockCommandResult) database.getClient().sendCommand(command);
        if(commandResult.getResult() == ICommand.COMMAND_RESULT_SUCCED)  {
            return;
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

    public void release() throws SlockException {
        release((byte) 0);
    }
}
