package io.github.snower.jaslock;

import io.github.snower.jaslock.commands.*;
import io.github.snower.jaslock.callback.DeferredCommandResult;
import io.github.snower.jaslock.callback.CallbackExecutorManager;
import io.github.snower.jaslock.callback.ExecutorOption;
import io.github.snower.jaslock.exceptions.ClientClosedException;
import io.github.snower.jaslock.exceptions.ClientAsyncCallbackDisabledException;
import io.github.snower.jaslock.exceptions.ClientUnconnectException;
import io.github.snower.jaslock.exceptions.SlockException;

import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

public class SlockReplsetClient implements ISlockClient {
    private final String[] hosts;
    private final LinkedList<SlockClient> clients;
    private final LinkedList<SlockClient> livedClients;
    private volatile SlockClient livedLeaderClient;
    private boolean closed;
    private final SlockDatabase[] databases;
    private CallbackExecutorManager callbackExecutorManager;

    public SlockReplsetClient(String hosts) {
        this(hosts.split("\\,"));
    }

    public SlockReplsetClient(String[] hosts) {
        this.hosts = hosts;
        this.clients = new LinkedList<>();
        this.livedClients = new LinkedList<>();
        this.livedLeaderClient = null;
        this.closed = false;
        this.databases = new SlockDatabase[256];
    }

    public SlockReplsetClient(String hosts, boolean enableAsyncCallback) {
        this(hosts.split("\\,"));
        if (enableAsyncCallback) {
            this.enableAsyncCallback();
        }
    }

    public SlockReplsetClient(String[] hosts, boolean enableAsyncCallback) {
        this(hosts);
        if (enableAsyncCallback) {
            this.enableAsyncCallback();
        }
    }

    @Override
    public boolean enableAsyncCallback() {
        if (callbackExecutorManager != null) return false;

        callbackExecutorManager = new CallbackExecutorManager(ExecutorOption.DefaultOption);
        if (!clients.isEmpty()) {
            callbackExecutorManager.start();
            for (SlockClient client : clients) {
                client.enableAsyncCallback(callbackExecutorManager);
            }
        }
        return true;
    }

    @Override
    public boolean enableAsyncCallback(ExecutorOption executorOption) {
        if (callbackExecutorManager != null) return false;

        callbackExecutorManager = new CallbackExecutorManager(executorOption);
        if (!clients.isEmpty()) {
            callbackExecutorManager.start();
            for (SlockClient client : clients) {
                client.enableAsyncCallback(callbackExecutorManager);
            }
        }
        return true;
    }

    @Override
    public boolean enableAsyncCallback(CallbackExecutorManager callbackExecutorManager) {
        if (this.callbackExecutorManager != null) {
            this.callbackExecutorManager.stop();
        }

        this.callbackExecutorManager = callbackExecutorManager;
        if (!clients.isEmpty()) {
            for (SlockClient client : clients) {
                client.enableAsyncCallback(callbackExecutorManager);
            }
        }
        return true;
    }

    @Override
    public void open() throws ClientUnconnectException {
        for(String host : hosts) {
            String[] hostInfo = host.startsWith("[") && host.contains("]:") ? host.substring(1).split("\\]\\:") : host.split("\\:");
            if(hostInfo.length != 2) {
                continue;
            }

            SlockClient client = new SlockClient(hostInfo[0], Integer.parseInt(hostInfo[1]), this, databases);
            if (callbackExecutorManager != null) {
                client.enableAsyncCallback(callbackExecutorManager);
            }
            clients.add(client);
            client.tryOpen();
        }

        if (clients.isEmpty()) {
            throw new ClientUnconnectException();
        }
        if (callbackExecutorManager != null) {
            callbackExecutorManager.start();
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
        try {
            for (SlockClient client : clients) {
                client.close();
            }
        } finally {
            if (callbackExecutorManager != null) {
                callbackExecutorManager.stop();
            }
            callbackExecutorManager = null;
        }
    }

    protected void addLivedClient(SlockClient client, boolean isLeader) {
        synchronized (this) {
            this.livedClients.add(client);
            if (isLeader) {
                this.livedLeaderClient = client;
            }
        }
    }

    protected void removeLivedClient(SlockClient client) {
        synchronized (this) {
            this.livedClients.remove(client);
            if (client.equals(this.livedLeaderClient)) {
                this.livedLeaderClient = null;
            }
        }
    }

    @Override
    public CommandResult sendCommand(Command command) throws SlockException {
        if(closed) {
            throw new ClientClosedException();
        }

        try {
            SlockClient client = livedLeaderClient;
            if (client == null) {
                client = livedClients.getFirst();
            }
            return client.sendCommand(command);
        } catch (NoSuchElementException e) {
            throw new ClientUnconnectException();
        }
    }

    @Override
    public void sendCommand(Command command, Consumer<DeferredCommandResult> callback) throws SlockException {
        if(closed) {
            throw new ClientClosedException();
        }
        if (callbackExecutorManager == null) {
            throw new ClientAsyncCallbackDisabledException();
        }

        try {
            SlockClient client = livedLeaderClient;
            if (client == null) {
                client = livedClients.getFirst();
            }
            client.sendCommand(command, callback);
        } catch (NoSuchElementException e) {
            throw new ClientUnconnectException();
        }
    }

    @Override
    public boolean ping() throws SlockException {
        PingCommand pingCommand = new PingCommand();
        PingCommandResult pingCommandResult = (PingCommandResult) sendCommand(pingCommand);
        return pingCommandResult != null && pingCommandResult.getResult() == ICommand.COMMAND_RESULT_SUCCED;
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

    @Override
    public GroupEvent newGroupEvent(byte[] groupKey, long clientId, long versionId, int timeout, int expried) {
        return selectDatabase((byte) 0).newGroupEvent(groupKey, clientId, versionId, timeout, expried);
    }

    @Override
    public GroupEvent newGroupEvent(String groupKey, long clientId, long versionId, int timeout, int expried) {
        return selectDatabase((byte) 0).newGroupEvent(groupKey, clientId, versionId, timeout, expried);
    }

    @Override
    public TreeLock newTreeLock(byte[] parentKey, byte[] lockKey, int timeout, int expried) {
        return selectDatabase((byte) 0).newTreeLock(parentKey, lockKey, timeout, expried);
    }

    @Override
    public TreeLock newTreeLock(String parentKey, String lockKey, int timeout, int expried) {
        return selectDatabase((byte) 0).newTreeLock(parentKey, lockKey, timeout, expried);
    }

    @Override
    public TreeLock newTreeLock(byte[] lockKey, int timeout, int expried) {
        return selectDatabase((byte) 0).newTreeLock(lockKey, timeout, expried);
    }

    @Override
    public TreeLock newTreeLock(String lockKey, int timeout, int expried) {
        return selectDatabase((byte) 0).newTreeLock(lockKey, timeout, expried);
    }
}
