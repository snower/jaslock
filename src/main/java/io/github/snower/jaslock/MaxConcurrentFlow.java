package io.github.snower.jaslock;

import io.github.snower.jaslock.commands.LockCommand;
import io.github.snower.jaslock.exceptions.SlockException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class MaxConcurrentFlow {
    private final SlockDatabase database;
    private byte[] flowKey;
    private final short count;
    private final int timeout;
    private final int expried;
    private volatile Lock flowLock;

    public MaxConcurrentFlow(SlockDatabase database, byte[] flowKey, short count, int timeout, int expried) {
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

    public MaxConcurrentFlow(SlockDatabase database, String flowKey, short count, int timeout, int expried) {
        this(database, flowKey.getBytes(StandardCharsets.UTF_8), count, timeout, expried);
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
