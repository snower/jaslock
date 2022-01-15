package io.github.snower.jaslock;

import io.github.snower.jaslock.commands.Command;
import io.github.snower.jaslock.commands.CommandResult;
import io.github.snower.jaslock.exceptions.SlockException;

import java.io.IOException;

public interface ISlockClient {
    void open() throws IOException;
    ISlockClient tryOpen();
    void close();
    CommandResult sendCommand(Command command) throws SlockException;
    boolean ping() throws SlockException;
    SlockDatabase selectDatabase(byte dbId);
    Lock newLock(byte[] lockKey, int timeout, int expried);
    Lock newLock(String lockKey, int timeout, int expried);
    Event newEvent(byte[] eventKey, int timeout, int expried, boolean defaultSeted);
    Event newEvent(String eventKey, int timeout, int expried, boolean defaultSeted);
    ReentrantLock newReentrantLock(byte[] lockKey, int timeout, int expried);
    ReentrantLock newReentrantLock(String lockKey, int timeout, int expried);
    ReadWriteLock newReadWriteLock(byte[] lockKey, int timeout, int expried);
    ReadWriteLock newReadWriteLock(String lockKey, int timeout, int expried);
    Semaphore newSemaphore(byte[] semaphoreKey, short count, int timeout, int expried);
    Semaphore newSemaphore(String semaphoreKey, short count, int timeout, int expried);
    MaxConcurrentFlow newMaxConcurrentFlow(byte[] flowKey, short count, int timeout, int expried);
    MaxConcurrentFlow newMaxConcurrentFlow(String flowKey, short count, int timeout, int expried);
    TokenBucketFlow newTokenBucketFlow(byte[] flowKey, short count, int timeout, double period);
    TokenBucketFlow newTokenBucketFlow(String flowKey, short count, int timeout, double period);
}
