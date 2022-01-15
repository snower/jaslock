package io.github.snower.jaslock;

import io.github.snower.jaslock.commands.*;
import io.github.snower.jaslock.exceptions.ClientClosedException;
import io.github.snower.jaslock.exceptions.ClientUnconnectException;
import io.github.snower.jaslock.exceptions.SlockException;

import java.util.LinkedList;
import java.util.NoSuchElementException;

public class SlockReplsetClient implements ISlockClient {
    private String[] hosts;
    private LinkedList<SlockClient> clients;
    private LinkedList<SlockClient> livedClients;
    private boolean closed;
    private SlockDatabase[] databases;

    public SlockReplsetClient(String hosts) {
        this(hosts.split(","));
    }

    public SlockReplsetClient(String[] hosts) {
        this.hosts = hosts;
        this.clients = new LinkedList<>();
        this.livedClients = new LinkedList<>();
        this.closed = false;
        this.databases = new SlockDatabase[256];
    }

    @Override
    public void open() {
        for(String host : hosts) {
            String[] hostInfo = host.split(":");
            if(hostInfo.length != 2) {
                continue;
            }

            SlockClient client = new SlockClient(hostInfo[0], Integer.parseInt(hostInfo[1]), this, databases);
            this.clients.add(client);
            client.tryOpen();
        }
    }

    @Override
    public ISlockClient tryOpen() {
        try {
            open();
        } catch (Exception ignore) {
            return null;
        }
        return this;
    }

    @Override
    public void close() {
        closed = true;
        for(SlockClient client : clients) {
            client.close();
        }
    }

    public void addLivedClient(SlockClient client) {
        synchronized (this) {
            this.livedClients.add(client);
        }
    }

    public void removeLivedClient(SlockClient client) {
        synchronized (this) {
            this.livedClients.remove(client);
        }
    }

    @Override
    public CommandResult sendCommand(Command command) throws SlockException {
        if(closed) {
            throw new ClientClosedException();
        }

        try {
            SlockClient client = livedClients.getFirst();
            return client.sendCommand(command);
        } catch (NoSuchElementException e) {
            throw new ClientUnconnectException();
        }
    }

    @Override
    public boolean ping() throws SlockException {
        PingCommand pingCommand = new PingCommand();
        PingCommandResult pingCommandResult = (PingCommandResult) sendCommand(pingCommand);
        if (pingCommandResult != null && pingCommandResult.getResult() == ICommand.COMMAND_RESULT_SUCCED) {
            return true;
        }
        return false;
    }

    @Override
    public SlockDatabase selectDatabase(byte dbId) {
        if(databases[dbId] == null) {
            synchronized (this) {
                if(databases[dbId] == null) {
                    databases[dbId] = new SlockDatabase(this, dbId);
                }
            }
        }
        return databases[dbId];
    }

    @Override
    public Lock newLock(byte[] lockKey, int timeout, int expried) {
        return selectDatabase((byte) 0).newLock(lockKey, timeout, expried);
    }

    @Override
    public Lock newLock(String lockKey, int timeout, int expried) {
        return selectDatabase((byte) 0).newLock(lockKey, timeout, expried);
    }

    @Override
    public Event newEvent(byte[] eventKey, int timeout, int expried, boolean defaultSeted) {
        return selectDatabase((byte) 0).newEvent(eventKey, timeout, expried, defaultSeted);
    }

    @Override
    public Event newEvent(String eventKey, int timeout, int expried, boolean defaultSeted) {
        return selectDatabase((byte) 0).newEvent(eventKey, timeout, expried, defaultSeted);
    }

    @Override
    public ReentrantLock newReentrantLock(byte[] lockKey, int timeout, int expried) {
        return selectDatabase((byte) 0).newReentrantLock(lockKey, timeout, expried);
    }

    @Override
    public ReentrantLock newReentrantLock(String lockKey, int timeout, int expried) {
        return selectDatabase((byte) 0).newReentrantLock(lockKey, timeout, expried);
    }

    @Override
    public ReadWriteLock newReadWriteLock(byte[] lockKey, int timeout, int expried) {
        return selectDatabase((byte) 0).newReadWriteLock(lockKey, timeout, expried);
    }

    @Override
    public ReadWriteLock newReadWriteLock(String lockKey, int timeout, int expried) {
        return selectDatabase((byte) 0).newReadWriteLock(lockKey, timeout, expried);
    }

    @Override
    public Semaphore newSemaphore(byte[] semaphoreKey, short count, int timeout, int expried) {
        return selectDatabase((byte) 0).newSemaphore(semaphoreKey, count, timeout, expried);
    }

    @Override
    public Semaphore newSemaphore(String semaphoreKey, short count, int timeout, int expried) {
        return selectDatabase((byte) 0).newSemaphore(semaphoreKey, count, timeout, expried);
    }

    @Override
    public MaxConcurrentFlow newMaxConcurrentFlow(byte[] flowKey, short count, int timeout, int expried) {
        return selectDatabase((byte) 0).newMaxConcurrentFlow(flowKey, count, timeout, expried);
    }

    @Override
    public MaxConcurrentFlow newMaxConcurrentFlow(String flowKey, short count, int timeout, int expried) {
        return selectDatabase((byte) 0).newMaxConcurrentFlow(flowKey, count, timeout, expried);
    }

    @Override
    public TokenBucketFlow newTokenBucketFlow(byte[] flowKey, short count, int timeout, double period) {
        return selectDatabase((byte) 0).newTokenBucketFlow(flowKey, count, timeout, period);
    }

    @Override
    public TokenBucketFlow newTokenBucketFlow(String flowKey, short count, int timeout, double period) {
        return selectDatabase((byte) 0).newTokenBucketFlow(flowKey, count, timeout, period);
    }
}
