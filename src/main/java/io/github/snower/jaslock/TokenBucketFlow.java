package io.github.snower.jaslock;

import io.github.snower.jaslock.commands.LockCommand;
import io.github.snower.jaslock.exceptions.LockTimeoutException;
import io.github.snower.jaslock.exceptions.SlockException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;

public class TokenBucketFlow {
    private final SlockDatabase database;
    private byte[] flowKey;
    private final short count;
    private final int timeout;
    private final double period;
    private int expriedFlag;

    public TokenBucketFlow(SlockDatabase database, byte[] flowKey, short count, int timeout, double period) {
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
        this.period = period;
    }

    public TokenBucketFlow(SlockDatabase database, String flowKey, short count, int timeout, double period) {
        this(database, flowKey.getBytes(StandardCharsets.UTF_8), count, timeout, period);
    }

    public void setExpriedFlag(int expriedFlag) {
        this.expriedFlag = expriedFlag;
    }

    public void acquire() throws SlockException {
        Lock flowLock;
        if(period < 3) {
            synchronized (this) {
                int expried = (int)Math.ceil(period * 1000) | 0x04000000;
                expried = expried | (expriedFlag << 16);
                flowLock = new Lock(database, flowKey, LockCommand.genLockId(), timeout, expried, count, (byte) 0);
            }
            flowLock.acquire();
            return;
        }

        synchronized (this) {
            long now = new Date().getTime() / 1000L;
            int expried = (int) (((long)Math.ceil(period)) - (now % ((long) Math.ceil((period)))));
            expried = expried | (expriedFlag << 16);
            flowLock = new Lock(database, flowKey, LockCommand.genLockId(), 0, expried, count, (byte) 0);
        }

        try {
            flowLock.acquire();
        } catch (LockTimeoutException e) {
            int expried = (int) Math.ceil(period);
            expried = expried | (expriedFlag << 16);
            flowLock = new Lock(database, flowKey, LockCommand.genLockId(), timeout, expried, count, (byte) 0);
            flowLock.acquire();
        }
    }
}
