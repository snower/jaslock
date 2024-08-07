package io.github.snower.jaslock;

import io.github.snower.jaslock.callback.CallbackExecutorManager;
import io.github.snower.jaslock.commands.Command;
import io.github.snower.jaslock.commands.CommandResult;
import io.github.snower.jaslock.callback.CallbackCommandResult;
import io.github.snower.jaslock.callback.ExecutorOption;
import io.github.snower.jaslock.exceptions.ClientUnconnectException;
import io.github.snower.jaslock.exceptions.SlockException;

import java.io.IOException;
import java.util.function.Consumer;

public interface ISlockClient {
    boolean enableAsyncCallback();
    boolean enableAsyncCallback(ExecutorOption executorOption);
    boolean enableAsyncCallback(CallbackExecutorManager callbackExecutorManager);
    void setDefaultTimeoutFlag(short defaultTimeoutFlag);
    void setDefaultExpriedFlag(short defaultExpriedFlag);
    void open() throws IOException, ClientUnconnectException;
    ISlockClient tryOpen();
    void close();
    CommandResult sendCommand(Command command) throws SlockException;
    void sendCommand(Command command, Consumer<CallbackCommandResult> callback) throws SlockException;
    void writeCommand(Command command) throws SlockException;
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
    MaxConcurrentFlow newMaxConcurrentFlow(byte[] flowKey, short count, int timeout, int expried, byte priority);
    MaxConcurrentFlow newMaxConcurrentFlow(String flowKey, short count, int timeout, int expried, byte priority);
    TokenBucketFlow newTokenBucketFlow(byte[] flowKey, short count, int timeout, double period);
    TokenBucketFlow newTokenBucketFlow(String flowKey, short count, int timeout, double period);
    TokenBucketFlow newTokenBucketFlow(byte[] flowKey, short count, int timeout, double period, byte priority);
    TokenBucketFlow newTokenBucketFlow(String flowKey, short count, int timeout, double period, byte priority);
    GroupEvent newGroupEvent(byte[] groupKey, long clientId, long versionId, int timeout, int expried);
    GroupEvent newGroupEvent(String groupKey, long clientId, long versionId, int timeout, int expried);
    TreeLock newTreeLock(byte[] parentKey, byte[] lockKey, int timeout, int expried);
    TreeLock newTreeLock(String parentKey, String lockKey, int timeout, int expried);
    TreeLock newTreeLock(byte[] lockKey, int timeout, int expried);
    TreeLock newTreeLock(String lockKey, int timeout, int expried);
    PriorityLock newPriorityLock(byte[] lockKey, byte priority, int timeout, int expried);
    PriorityLock newPriorityLock(String lockKey, byte priority, int timeout, int expried);
}
