package io.github.snower.jaslock;

import io.github.snower.jaslock.commands.*;
import io.github.snower.jaslock.callback.CallbackCommandResult;
import io.github.snower.jaslock.callback.CallbackExecutorManager;
import io.github.snower.jaslock.callback.ExecutorOption;
import io.github.snower.jaslock.exceptions.ClientClosedException;
import io.github.snower.jaslock.exceptions.ClientAsyncCallbackDisabledException;
import io.github.snower.jaslock.exceptions.ClientUnconnectException;
import io.github.snower.jaslock.exceptions.SlockException;

import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;

public class SlockReplsetClient implements ISlockClient {
    private final String[] hosts;
    private short defaultTimeoutFlag;
    private short defaultExpriedFlag;
    private final LinkedList<SlockClient> clients;
    private final LinkedList<SlockClient> livedClients;
    private volatile SlockClient livedLeaderClient;
    private final ConcurrentHashMap<SlockClient.BytesKey, Command> requests;
    private final ConcurrentLinkedDeque<Command> pendingRequests;
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
        this.requests = new ConcurrentHashMap<>();
        this.pendingRequests = new ConcurrentLinkedDeque<>();
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

    protected ConcurrentHashMap<SlockClient.BytesKey, Command> getRequests() {
        return requests;
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
    public void setDefaultTimeoutFlag(short defaultTimeoutFlag) {
        this.defaultTimeoutFlag = defaultTimeoutFlag;
        for (SlockDatabase database : databases) {
            if (database != null) {
                database.setDefaultTimeoutFlag(defaultTimeoutFlag);
            }
        }
    }

    @Override
    public void setDefaultExpriedFlag(short defaultExpriedFlag) {
        this.defaultExpriedFlag = defaultExpriedFlag;
        for (SlockDatabase database : databases) {
            if (database != null) {
                database.setDefaultExpriedFlag(defaultExpriedFlag);
            }
        }
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
            throw new ClientUnconnectException("clients not connected");
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
                try {
                    client.close();
                } catch (Throwable ignored) {}
            }
            clients.clear();
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
        this.wakeupPendingRequestCommands(client);
    }

    protected void removeLivedClient(SlockClient client) {
        synchronized (this) {
            this.livedClients.remove(client);
            if (client.equals(this.livedLeaderClient)) {
                this.livedLeaderClient = null;
            }
        }
    }

    protected void addLivedLeaderClient(SlockClient client) {
        synchronized (this) {
            this.livedLeaderClient = client;
        }
        this.wakeupPendingRequestCommands(client);
    }

    protected void removeLivedLeaderClient(SlockClient client) {
        synchronized (this) {
            if (client.equals(this.livedLeaderClient)) {
                this.livedLeaderClient = null;
            }
        }
    }

    protected boolean doPendingRequestCommand(SlockClient client, Command command) {
        if (closed) return false;
        if (livedLeaderClient == null && livedClients.isEmpty()) return false;

        if (command.getRetryType() < 1) {
            try {
                SlockClient currentClient = livedLeaderClient;
                if (currentClient == null) {
                    currentClient = livedClients.getFirst();
                }
                if (currentClient != client) {
                    currentClient.writeCommand(command);
                    command.setRetryType(1);
                    return true;
                }
            } catch (Throwable ignored) {}
        }
        this.pendingRequests.add(command);
        command.setRetryType(2);
        return true;
    }

    protected void removePendingRequestCommand(Command command) {
        this.pendingRequests.remove(command);
    }

    protected void wakeupPendingRequestCommands(SlockClient client) {
        while (!this.pendingRequests.isEmpty()) {
            Command command = this.pendingRequests.poll();
            if (command == null) break;

            command.setRetryType(3);
            try {
                client.writeCommand(command);
            } catch (Throwable e) {
                command.exception = e;
                command.wakeupWaiter();
            }
        }
    }

    @Override
    public CommandResult sendCommand(Command command) throws SlockException {
        if(closed) {
            throw new ClientClosedException("client has been closed");
        }

        try {
            SlockClient client = livedLeaderClient;
            if (client == null) {
                client = livedClients.getFirst();
            }
            return client.sendCommand(command);
        } catch (NoSuchElementException e) {
            throw new ClientUnconnectException("clients not connected");
        }
    }

    @Override
    public void sendCommand(Command command, Consumer<CallbackCommandResult> callback) throws SlockException {
        if(closed) {
            throw new ClientClosedException("client has been closed");
        }
        if (callbackExecutorManager == null) {
            throw new ClientAsyncCallbackDisabledException("The asynchronous thread pool is not enabled. First enableAsyncCallback to enable the asynchronous thread pool.");
        }

        try {
            SlockClient client = livedLeaderClient;
            if (client == null) {
                client = livedClients.getFirst();
            }
            client.sendCommand(command, callback);
        } catch (NoSuchElementException e) {
            throw new ClientUnconnectException("clients not connected");
        }
    }

    @Override
    public void writeCommand(Command command) throws SlockException {
        if(closed) {
            throw new ClientClosedException("client has been closed");
        }

        try {
            SlockClient client = livedLeaderClient;
            if (client == null) {
                client = livedClients.getFirst();
            }
            client.writeCommand(command);
        } catch (NoSuchElementException e) {
            throw new ClientUnconnectException("clients not connected");
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
                    databases[dbId] = new SlockDatabase(this, dbId, defaultTimeoutFlag, defaultExpriedFlag);
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
    public MaxConcurrentFlow newMaxConcurrentFlow(byte[] flowKey, short count, int timeout, int expried, byte priority) {
        return selectDatabase((byte) 0).newMaxConcurrentFlow(flowKey, count, timeout, expried, priority);
    }

    @Override
    public MaxConcurrentFlow newMaxConcurrentFlow(String flowKey, short count, int timeout, int expried, byte priority) {
        return selectDatabase((byte) 0).newMaxConcurrentFlow(flowKey, count, timeout, expried, priority);
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
    public TokenBucketFlow newTokenBucketFlow(byte[] flowKey, short count, int timeout, double period, byte priority) {
        return selectDatabase((byte) 0).newTokenBucketFlow(flowKey, count, timeout, period, priority);
    }

    @Override
    public TokenBucketFlow newTokenBucketFlow(String flowKey, short count, int timeout, double period, byte priority) {
        return selectDatabase((byte) 0).newTokenBucketFlow(flowKey, count, timeout, period, priority);
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

    @Override
    public PriorityLock newPriorityLock(byte[] lockKey, byte priority, int timeout, int expried) {
        return selectDatabase((byte) 0).newPriorityLock(lockKey, priority, timeout, expried);
    }

    @Override
    public PriorityLock newPriorityLock(String lockKey, byte priority, int timeout, int expried) {
        return selectDatabase((byte) 0).newPriorityLock(lockKey, priority, timeout, expried);
    }
}
