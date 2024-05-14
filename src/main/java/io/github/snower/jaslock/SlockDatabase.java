package io.github.snower.jaslock;

import java.nio.charset.StandardCharsets;

public class SlockDatabase {
    private ISlockClient client;
    private final byte dbId;
    private short defaultTimeoutFlag;
    private short defaultExpriedFlag;

    public SlockDatabase(ISlockClient client, byte dbId, short defaultTimeoutFlag, short defaultExpriedFlag) {
        this.client = client;
        this.dbId = dbId;
        this.defaultTimeoutFlag = defaultTimeoutFlag;
        this.defaultExpriedFlag = defaultExpriedFlag;
    }

    public void setDefaultTimeoutFlag(short defaultTimeoutFlag) {
        this.defaultTimeoutFlag = defaultTimeoutFlag;
    }

    public void setDefaultExpriedFlag(short defaultExpriedFlag) {
        this.defaultExpriedFlag = defaultExpriedFlag;
    }

    public void close() {
        client = null;
    }

    public ISlockClient getClient() {
        return client;
    }

    public byte getDbId() {
        return dbId;
    }

    private int mergeTimeoutFlag(int timeout) {
        if (defaultTimeoutFlag != 0) {
            timeout = timeout | ((((int) defaultTimeoutFlag) & 0xffff) << 16);
        }
        return timeout;
    }

    private int mergeExpriedFlag(int expried) {
        if (defaultExpriedFlag != 0) {
            expried = expried | ((((int) defaultExpriedFlag) & 0xffff) << 16);
        }
        return expried;
    }

    public Lock newLock(byte[] lockKey, int timeout, int expried) {
        return new Lock(this, lockKey, mergeTimeoutFlag(timeout), mergeExpriedFlag(expried));
    }

    public Lock newLock(String lockKey, int timeout, int expried) {
        return new Lock(this, lockKey.getBytes(StandardCharsets.UTF_8), mergeTimeoutFlag(timeout), mergeExpriedFlag(expried));
    }

    public Event newEvent(byte[] eventKey, int timeout, int expried, boolean defaultSeted) {
        return new Event(this, eventKey, mergeTimeoutFlag(timeout), mergeExpriedFlag(expried), defaultSeted);
    }

    public Event newEvent(String eventKey, int timeout, int expried, boolean defaultSeted) {
        return new Event(this, eventKey.getBytes(StandardCharsets.UTF_8), mergeTimeoutFlag(timeout), mergeExpriedFlag(expried), defaultSeted);
    }

    public ReentrantLock newReentrantLock(byte[] lockKey, int timeout, int expried) {
        return new ReentrantLock(this, lockKey, mergeTimeoutFlag(timeout), mergeExpriedFlag(expried));
    }

    public ReentrantLock newReentrantLock(String lockKey, int timeout, int expried) {
        return new ReentrantLock(this, lockKey.getBytes(StandardCharsets.UTF_8), mergeTimeoutFlag(timeout), mergeExpriedFlag(expried));
    }

    public ReadWriteLock newReadWriteLock(byte[] lockKey, int timeout, int expried) {
        return new ReadWriteLock(this, lockKey, mergeTimeoutFlag(timeout), mergeExpriedFlag(expried));
    }

    public ReadWriteLock newReadWriteLock(String lockKey, int timeout, int expried) {
        return new ReadWriteLock(this, lockKey.getBytes(StandardCharsets.UTF_8), mergeTimeoutFlag(timeout), mergeExpriedFlag(expried));
    }

    public Semaphore newSemaphore(byte[] semaphoreKey, short count, int timeout, int expried) {
        return new Semaphore(this, semaphoreKey, count, mergeTimeoutFlag(timeout), mergeExpriedFlag(expried));
    }

    public Semaphore newSemaphore(String semaphoreKey, short count, int timeout, int expried) {
        return new Semaphore(this, semaphoreKey.getBytes(StandardCharsets.UTF_8), count, mergeTimeoutFlag(timeout), mergeExpriedFlag(expried));
    }

    public MaxConcurrentFlow newMaxConcurrentFlow(byte[] flowKey, short count, int timeout, int expried) {
        return new MaxConcurrentFlow(this, flowKey, count, mergeTimeoutFlag(timeout), mergeExpriedFlag(expried));
    }

    public MaxConcurrentFlow newMaxConcurrentFlow(String flowKey, short count, int timeout, int expried) {
        return new MaxConcurrentFlow(this, flowKey.getBytes(StandardCharsets.UTF_8), count, mergeTimeoutFlag(timeout), mergeExpriedFlag(expried));
    }

    public TokenBucketFlow newTokenBucketFlow(byte[] flowKey, short count, int timeout, double period) {
        return new TokenBucketFlow(this, flowKey, count, mergeTimeoutFlag(timeout), period);
    }

    public TokenBucketFlow newTokenBucketFlow(String flowKey, short count, int timeout, double period) {
        return new TokenBucketFlow(this, flowKey.getBytes(StandardCharsets.UTF_8), count, mergeTimeoutFlag(timeout), period);
    }

    public GroupEvent newGroupEvent(byte[] groupKey, long clientId, long versionId, int timeout, int expried) {
        return new GroupEvent(this, groupKey, clientId, versionId, mergeTimeoutFlag(timeout), mergeExpriedFlag(expried));
    }

    public GroupEvent newGroupEvent(String groupKey, long clientId, long versionId, int timeout, int expried) {
        return new GroupEvent(this, groupKey, clientId, versionId, mergeTimeoutFlag(timeout), mergeExpriedFlag(expried));
    }

    public TreeLock newTreeLock(byte[] parentKey, byte[] lockKey, int timeout, int expried) {
        return new TreeLock(this, parentKey, lockKey, mergeTimeoutFlag(timeout), mergeExpriedFlag(expried));
    }

    public TreeLock newTreeLock(String parentKey, String lockKey, int timeout, int expried) {
        return new TreeLock(this, parentKey, lockKey, mergeTimeoutFlag(timeout), mergeExpriedFlag(expried));
    }

    public TreeLock newTreeLock(byte[] lockKey, int timeout, int expried) {
        return new TreeLock(this, lockKey, mergeTimeoutFlag(timeout), mergeExpriedFlag(expried));
    }

    public TreeLock newTreeLock(String lockKey, int timeout, int expried) {
        return new TreeLock(this, lockKey, mergeTimeoutFlag(timeout), mergeExpriedFlag(expried));
    }
}
