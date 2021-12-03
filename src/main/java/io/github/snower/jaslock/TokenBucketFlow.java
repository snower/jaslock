package io.github.snower.jaslock;

import io.github.snower.jaslock.commands.LockCommand;
import io.github.snower.jaslock.exceptions.LockTimeoutException;
import io.github.snower.jaslock.exceptions.SlockException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;

public class TokenBucketFlow {
    private Database database;
    private byte[] flowKey;
    private short count;
    private int timeout;
    private double period;
    private int expriedFlag;
    private Lock flowLock;

    public TokenBucketFlow(Database database, byte[] flowKey, short count, int timeout, double period) {
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

    public void setExpriedFlag(int expriedFlag) {
        this.expriedFlag = expriedFlag;
    }

    public void acquire() throws SlockException {
        synchronized (this) {
            int expried = 0;
            long now = new Date().getTime();
            if(period <= 1) {
                expried = (int) (((1000L - now%1000L) | 0x04000000L) & 0xffffffffL);
            } else {
                now = now / 1000L;
                if(((long) period)%60 == 0) {
                    expried = (int) (((now/60L+1L)*60L)%120L + (60L - (now % 60L)));
                } else {
                    expried = (int) (((long)period) - (now % ((long) (period))));
                }
            }
            expried = expried | (expriedFlag << 16);
            flowLock = new Lock(database, flowKey, LockCommand.genLockId(), timeout, expried, count, (byte) 0);
        }

        try {
            flowLock.acquire();
        } catch (LockTimeoutException e) {
            flowLock = new Lock(database, flowKey, LockCommand.genLockId(), timeout, (int) period, count, (byte) 0);
            flowLock.acquire();
        }
    }
}
