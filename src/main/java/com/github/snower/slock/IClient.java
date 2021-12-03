package com.github.snower.slock;

import com.github.snower.slock.commands.Command;
import com.github.snower.slock.commands.CommandResult;
import com.github.snower.slock.exceptions.SlockException;

import java.io.IOException;

public interface IClient {
    void open() throws IOException;
    void close();
    CommandResult sendCommand(Command command) throws SlockException;
    Database selectDatabase(byte dbId);
    Lock newLock(byte[] lockKey, int timeout, int expried);
    Event newEvent(byte[] eventKey, int timeout, int expried, boolean defaultSeted);
    ReentrantLock newReentrantLock(byte[] lockKey, int timeout, int expried);
    ReadWriteLock newReadWriteLock(byte[] lockKey, int timeout, int expried);
    Semaphore newSemaphore(byte[] semaphoreKey, short count, int timeout, int expried);
    MaxConcurrentFlow newMaxConcurrentFlow(byte[] flowKey, short count, int timeout, int expried);
    TokenBucketFlow newTokenBucketFlow(byte[] flowKey, short count, int timeout, double period);
}
