package com.github.snower.slock;

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
}
