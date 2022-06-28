package io.github.snower.jaslock;

import java.nio.charset.StandardCharsets;

public class SlockDatabase {
    private ISlockClient client;
    private final byte dbId;

    public SlockDatabase(ISlockClient client, byte dbId) {
        this.client = client;
        this.dbId = dbId;
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

    public Lock newLock(byte[] lockKey, int timeout, int expried) {
        return new Lock(this, lockKey, timeout, expried);
    }

    public Lock newLock(String lockKey, int timeout, int expried) {
        return new Lock(this, lockKey.getBytes(StandardCharsets.UTF_8), timeout, expried);
    }

    public Event newEvent(byte[] eventKey, int timeout, int expried, boolean defaultSeted) {
        return new Event(this, eventKey, timeout, expried, defaultSeted);
    }

    public Event newEvent(String eventKey, int timeout, int expried, boolean defaultSeted) {
        return new Event(this, eventKey.getBytes(StandardCharsets.UTF_8), timeout, expried, defaultSeted);
    }

    public ReentrantLock newReentrantLock(byte[] lockKey, int timeout, int expried) {
        return new ReentrantLock(this, lockKey, timeout, expried);
    }

    public ReentrantLock newReentrantLock(String lockKey, int timeout, int expried) {
        return new ReentrantLock(this, lockKey.getBytes(StandardCharsets.UTF_8), timeout, expried);
    }

    public ReadWriteLock newReadWriteLock(byte[] lockKey, int timeout, int expried) {
        return new ReadWriteLock(this, lockKey, timeout, expried);
    }

    public ReadWriteLock newReadWriteLock(String lockKey, int timeout, int expried) {
        return new ReadWriteLock(this, lockKey.getBytes(StandardCharsets.UTF_8), timeout, expried);
    }

    public Semaphore newSemaphore(byte[] semaphoreKey, short count, int timeout, int expried) {
        return new Semaphore(this, semaphoreKey, count, timeout, expried);
    }

    public Semaphore newSemaphore(String semaphoreKey, short count, int timeout, int expried) {
        return new Semaphore(this, semaphoreKey.getBytes(StandardCharsets.UTF_8), count, timeout, expried);
    }

    public MaxConcurrentFlow newMaxConcurrentFlow(byte[] flowKey, short count, int timeout, int expried) {
        return new MaxConcurrentFlow(this, flowKey, count, timeout, expried);
    }

    public MaxConcurrentFlow newMaxConcurrentFlow(String flowKey, short count, int timeout, int expried) {
        return new MaxConcurrentFlow(this, flowKey.getBytes(StandardCharsets.UTF_8), count, timeout, expried);
    }

    public TokenBucketFlow newTokenBucketFlow(byte[] flowKey, short count, int timeout, double period) {
        return new TokenBucketFlow(this, flowKey, count, timeout, period);
    }

    public TokenBucketFlow newTokenBucketFlow(String flowKey, short count, int timeout, double period) {
        return new TokenBucketFlow(this, flowKey.getBytes(StandardCharsets.UTF_8), count, timeout, period);
    }

    public GroupEvent newGroupEvent(byte[] groupKey, long clientId, long versionId, int timeout, int expried) {
        return new GroupEvent(this, groupKey, clientId, versionId, timeout, expried);
    }

    public GroupEvent newGroupEvent(String groupKey, long clientId, long versionId, int timeout, int expried) {
        return new GroupEvent(this, groupKey, clientId, versionId, timeout, expried);
    }

    public TreeLock newTreeLock(byte[] parentKey, byte[] lockKey, int timeout, int expried) {
        return new TreeLock(this, parentKey, lockKey, timeout, expried);
    }

    public TreeLock newTreeLock(String parentKey, String lockKey, int timeout, int expried) {
        return new TreeLock(this, parentKey, lockKey, timeout, expried);
    }

    public TreeLock newTreeLock(byte[] lockKey, int timeout, int expried) {
        return new TreeLock(this, lockKey, timeout, expried);
    }

    public TreeLock newTreeLock(String lockKey, int timeout, int expried) {
        return new TreeLock(this, lockKey, timeout, expried);
    }
}
