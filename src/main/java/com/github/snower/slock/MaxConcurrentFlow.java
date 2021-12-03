package com.github.snower.slock;

import com.github.snower.slock.commands.LockCommand;
import com.github.snower.slock.exceptions.SlockException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class MaxConcurrentFlow {
    private Database database;
    private byte[] flowKey;
    private short count;
    private int timeout;
    private int expried;
    private Lock flowLock;

    public MaxConcurrentFlow(Database database, byte[] flowKey, short count, int timeout, int expried) {
        this.database = database;
        if(flowKey.length > 16) {
            try {
                MessageDigest digest = MessageDigest.getInstance("MD5");
                this.flowKey = digest.digest(flowKey);
            } catch (NoSuchAlgorithmException e) {
                this.flowKey = Arrays.copyOfRange(flowKey, 0, 16);
            }
        } else {
            this.flowKey = new byte[16];
            System.arraycopy(flowKey, 0, this.flowKey, 16 - flowKey.length, flowKey.length);
        }
        this.count = (short) (count > 0 ? count - 1 : 0);
        this.timeout = timeout;
        this.expried = expried;
    }

    public void acquire() throws SlockException {
        if (flowLock == null) {
            synchronized (this) {
                if (flowLock == null) {
                    flowLock = new Lock(database, flowKey, LockCommand.genLockId(), timeout, expried, count, (byte) 0);
                }
            }
        }
        flowLock.acquire();
    }

    public void release() throws SlockException {
        if (flowLock == null) {
            synchronized (this) {
                if (flowLock == null) {
                    flowLock = new Lock(database, flowKey, LockCommand.genLockId(), timeout, expried, count, (byte) 0);
                }
            }
        }
        flowLock.release();
    }
}
