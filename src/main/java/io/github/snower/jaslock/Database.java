package io.github.snower.jaslock;

public class Database {
    private IClient client;
    private byte dbId;

    public Database(IClient client, byte dbId) {
        this.client = client;
        this.dbId = dbId;
    }

    public void close() {
        client = null;
    }

    public IClient getClient() {
        return client;
    }

    public byte getDbId() {
        return dbId;
    }

    public Lock newLock(byte[] lockKey, int timeout, int expried) {
        return new Lock(this, lockKey, timeout, expried);
    }

    public Event newEvent(byte[] eventKey, int timeout, int expried, boolean defaultSeted) {
        return new Event(this, eventKey, timeout, expried, defaultSeted);
    }

    public ReentrantLock newReentrantLock(byte[] lockKey, int timeout, int expried) {
        return new ReentrantLock(this, lockKey, timeout, expried);
    }

    public ReadWriteLock newReadWriteLock(byte[] lockKey, int timeout, int expried) {
        return new ReadWriteLock(this, lockKey, timeout, expried);
    }

    public Semaphore newSemaphore(byte[] semaphoreKey, short count, int timeout, int expried) {
        return new Semaphore(this, semaphoreKey, count, timeout, expried);
    }

    public MaxConcurrentFlow newMaxConcurrentFlow(byte[] flowKey, short count, int timeout, int expried) {
        return new MaxConcurrentFlow(this, flowKey, count, timeout, expried);
    }

    public TokenBucketFlow newTokenBucketFlow(byte[] flowKey, short count, int timeout, double period) {
        return new TokenBucketFlow(this, flowKey, count, timeout, period);
    }
}
