package io.github.snower.jaslock;

import io.github.snower.jaslock.commands.*;
import io.github.snower.jaslock.exceptions.ClientClosedException;
import io.github.snower.jaslock.exceptions.ClientUnconnectException;
import io.github.snower.jaslock.exceptions.SlockException;

import java.util.LinkedList;
import java.util.NoSuchElementException;

public class ReplsetClient implements IClient {
    private String[] hosts;
    private LinkedList<Client> clients;
    private LinkedList<Client> livedClients;
    private boolean closed;
    private Database[] databases;

    public ReplsetClient(String hosts) {
        this(hosts.split(","));
    }

    public ReplsetClient(String[] hosts) {
        this.hosts = hosts;
        this.clients = new LinkedList<>();
        this.livedClients = new LinkedList<>();
        this.closed = false;
        this.databases = new Database[256];
    }

    @Override
    public void open() {
        for(String host : hosts) {
            String[] hostInfo = host.split(":");
            if(hostInfo.length != 2) {
                continue;
            }

            Client client = new Client(hostInfo[0], Integer.parseInt(hostInfo[1]), this, databases);
            this.clients.add(client);
            client.tryOpen();
        }
    }

    @Override
    public void close() {
        closed = true;
        for(Client client : clients) {
            client.close();
        }
    }

    public void addLivedClient(Client client) {
        synchronized (this) {
            this.livedClients.add(client);
        }
    }

    public void removeLivedClient(Client client) {
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
            Client client = livedClients.getFirst();
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
    public Database selectDatabase(byte dbId) {
        if(databases[dbId] == null) {
            synchronized (this) {
                if(databases[dbId] == null) {
                    databases[dbId] = new Database(this, dbId);
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
    public Event newEvent(byte[] eventKey, int timeout, int expried, boolean defaultSeted) {
        return selectDatabase((byte) 0).newEvent(eventKey, timeout, expried, defaultSeted);
    }


    @Override
    public ReentrantLock newReentrantLock(byte[] lockKey, int timeout, int expried) {
        return selectDatabase((byte) 0).newReentrantLock(lockKey, timeout, expried);
    }

    @Override
    public ReadWriteLock newReadWriteLock(byte[] lockKey, int timeout, int expried) {
        return selectDatabase((byte) 0).newReadWriteLock(lockKey, timeout, expried);
    }

    @Override
    public Semaphore newSemaphore(byte[] semaphoreKey, short count, int timeout, int expried) {
        return selectDatabase((byte) 0).newSemaphore(semaphoreKey, count, timeout, expried);
    }

    @Override
    public MaxConcurrentFlow newMaxConcurrentFlow(byte[] flowKey, short count, int timeout, int expried) {
        return selectDatabase((byte) 0).newMaxConcurrentFlow(flowKey, count, timeout, expried);
    }

    @Override
    public TokenBucketFlow newTokenBucketFlow(byte[] flowKey, short count, int timeout, double period) {
        return selectDatabase((byte) 0).newTokenBucketFlow(flowKey, count, timeout, period);
    }
}
