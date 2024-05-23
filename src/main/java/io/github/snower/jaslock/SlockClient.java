package io.github.snower.jaslock;

import io.github.snower.jaslock.callback.CallbackCommand;
import io.github.snower.jaslock.commands.*;
import io.github.snower.jaslock.callback.CallbackCommandResult;
import io.github.snower.jaslock.callback.CallbackExecutorManager;
import io.github.snower.jaslock.callback.ExecutorOption;
import io.github.snower.jaslock.exceptions.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class SlockClient implements Runnable, ISlockClient {
    private static final class BytesKey {
        private final byte[] bytes;
        private int bytesHash = 0;

        public BytesKey(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return Arrays.equals(bytes, ((BytesKey) o).bytes);
        }

        @Override
        public int hashCode() {
            if (bytesHash == 0) {
                bytesHash = Arrays.hashCode(bytes);
            }
            return bytesHash;
        }
    }

    private final String host;
    private final int port;
    private short defaultTimeoutFlag;
    private short defaultExpriedFlag;
    private boolean closed;
    private byte[] clientId;
    private Thread thread;
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private SlockDatabase[] databases;
    private ConcurrentHashMap<BytesKey, Command> requests;
    private SlockReplsetClient replsetClient;
    private CallbackExecutorManager callbackExecutorManager;

    public SlockClient() {
        this("127.0.0.1", 5658);
    }

    public SlockClient(String host) {
        this(host, 5658);
    }

    public SlockClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.databases = new SlockDatabase[256];
        this.requests = new ConcurrentHashMap<>();
        this.closed = false;
    }

    public SlockClient(String host, int port, boolean enableAsyncCallback) {
        this(host, port);
        if (enableAsyncCallback) {
            this.enableAsyncCallback();
        }
    }

    protected SlockClient(String host, int port, SlockReplsetClient replsetClient, SlockDatabase[] databases) {
        this(host, port);
        this.replsetClient = replsetClient;
        this.databases = databases;
        this.requests = new ConcurrentHashMap<>();
    }

    @Override
    public boolean enableAsyncCallback() {
        if (replsetClient != null) return false;
        if (callbackExecutorManager != null) return false;

        callbackExecutorManager = new CallbackExecutorManager(ExecutorOption.DefaultOption);
        if (thread != null && replsetClient == null) {
            callbackExecutorManager.start();
        }
        return true;
    }

    @Override
    public boolean enableAsyncCallback(ExecutorOption executorOption) {
        if (replsetClient != null) return false;
        if (callbackExecutorManager != null) return false;

        callbackExecutorManager = new CallbackExecutorManager(executorOption);
        if (thread != null && replsetClient == null) {
            callbackExecutorManager.start();
        }
        return true;
    }

    @Override
    public boolean enableAsyncCallback(CallbackExecutorManager callbackExecutorManager) {
        if (this.callbackExecutorManager != null) {
            this.callbackExecutorManager.stop();
        }
        this.callbackExecutorManager = callbackExecutorManager;
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
    public void open() throws IOException {
        if(thread != null) {
            return;
        }
        connect();
        thread = new Thread(this, "jaslock-io-" + host + ":" + port);
        thread.setDaemon(true);
        thread.start();

        if (callbackExecutorManager != null && replsetClient == null) {
            callbackExecutorManager.start();
        }
    }

    @Override
    public ISlockClient tryOpen() {
        try {
            if(thread != null) {
                return this;
            }
            connect();
        } catch (IOException e) {
            thread = new Thread(this, "jaslock-io-" + host + ":" + port);
            thread.setDaemon(true);
            thread.start();

            if (callbackExecutorManager != null && replsetClient == null) {
                callbackExecutorManager.start();
            }
            return null;
        }
        thread = new Thread(this, "jaslock-io-" + host + ":" + port);
        thread.setDaemon(true);
        thread.start();

        if (callbackExecutorManager != null && replsetClient == null) {
            callbackExecutorManager.start();
        }
        return this;
    }

    @Override
    public void close() {
        closed = true;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }

        synchronized (this) {
            for(BytesKey requestId : requests.keySet().toArray(new BytesKey[0])) {
                Command command = requests.remove(requestId);
                if(command == null) {
                    continue;
                }
                command.commandResult = null;
                command.wakeupWaiter();
            }

            for(int i = 0; i < databases.length; i++) {
                if(databases[i] != null ) {
                    databases[i].close();
                }
                databases[i] = null;
            }
        }

        if (callbackExecutorManager != null && replsetClient == null) {
            callbackExecutorManager.stop();
        }
        callbackExecutorManager = null;
    }

    private int readBytes(byte[] buf, int len) throws IOException {
        int n = inputStream.read(buf, 0, len);
        while (n > 0 && n < len) {
            int nn = inputStream.read(buf, n, len - n);
            if(nn <= 0) {
                break;
            }
            n += nn;
        }
        return n;
    }

    private int readBytes(byte[] buf, int offset, int len) throws IOException {
        int n = inputStream.read(buf, offset, len);
        while (n > 0 && n < len) {
            int nn = inputStream.read(buf, offset + n, len - n);
            if(nn <= 0) {
                break;
            }
            n += nn;
        }
        return n;
    }

    @Override
    public void run() {
        try {
            while (!closed) {
                try {
                    if(inputStream == null) {
                        reconnect();
                        continue;
                    }

                    byte[] buf = new byte[64];
                    while (!closed && socket != null) {
                        try {
                            if(readBytes(buf, 64) < 64) {
                                break;
                            }
                        } catch (IOException ignored) {
                            break;
                        }

                        switch (buf[2]) {
                            case ICommand.COMMAND_TYPE_LOCK:
                            case ICommand.COMMAND_TYPE_UNLOCK:
                                LockCommandResult lockCommandResult = new LockCommandResult();
                                if (lockCommandResult.parseCommand(buf) != null) {
                                    if (lockCommandResult.hasExtraData()) {
                                        if(readBytes(buf, 4) < 4) {
                                            break;
                                        }
                                        int dataLen = (((int) buf[0]) & 0xff) | ((((int) buf[1]) & 0xff) << 8) | ((((int) buf[2]) & 0xff) << 16) | ((((int) buf[3]) & 0xff) << 24);
                                        byte[] dataBuf = new byte[dataLen + 4];
                                        if (readBytes(dataBuf, 4, dataLen) < dataLen) {
                                            break;
                                        }
                                        lockCommandResult.loadCommandData(dataBuf);
                                    }
                                    handleCommand(lockCommandResult);
                                }
                                break;
                            case ICommand.COMMAND_TYPE_PING:
                                PingCommandResult pingResultCommand = new PingCommandResult();
                                if (pingResultCommand.parseCommand(buf) != null) {
                                    handleCommand(pingResultCommand);
                                }
                                break;
                        }
                    }
                } catch (Exception ignored) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored1) {}
                }
                closeSocket();
            }
        } finally {
            closeSocket();
            thread = null;
            replsetClient = null;
        }
    }

    protected void connect() throws IOException {
        socket = new Socket();
        socket.setKeepAlive(true);
        socket.setTcpNoDelay(true);
        socket.connect(new InetSocketAddress(host, port), 5000);
        try {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
        } catch (IOException e) {
            closeSocket();
            throw e;
        }

        InitCommandResult initCommandResult = initClient();
        if(replsetClient != null) {
            replsetClient.addLivedClient(this, initCommandResult.getInitType() == 1);
        }
    }

    protected void reconnect() {
        synchronized (this) {
            for(BytesKey requestId : requests.keySet().toArray(new BytesKey[0])) {
                Command command = requests.remove(requestId);
                if(command == null) {
                    continue;
                }
                command.commandResult = null;
                command.wakeupWaiter();
            }
        }

        while (!closed) {
            try {
                connect();
                return;
            } catch (IOException e) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    protected void closeSocket() {
        synchronized (this) {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
                socket = null;
                inputStream = null;
                outputStream = null;
                if (replsetClient != null) {
                    replsetClient.removeLivedClient(this);
                }
            }
        }
    }

    protected InitCommandResult initClient() throws IOException {
        if(clientId == null) {
            clientId = InitCommand.genClientId();
        }
        InitCommand initCommand = new InitCommand(clientId);
        byte[] buf = initCommand.dumpCommand();
        try {
            outputStream.write(buf);
        } catch (IOException e) {
            closeSocket();
            throw e;
        }

        try {
            if (readBytes(buf, 64) < 64) {
                throw new IOException("read result error");
            }
            InitCommandResult initCommandResult = new InitCommandResult();
            if (initCommandResult.parseCommand(buf) != null) {
                if(initCommandResult.getResult() != ICommand.COMMAND_RESULT_SUCCED) {
                    throw new IOException("init commnad error");
                }
            }
            return initCommandResult;
        } catch (IOException e) {
            closeSocket();
            throw e;
        }
    }

    protected void handleCommand(CommandResult commandResult) {
        BytesKey requestId = new BytesKey(commandResult.getRequestId());
        if (!requests.containsKey(requestId)) {
            return;
        }
        Command command = requests.remove(requestId);
        if(command == null) {
            return;
        }
        command.commandResult = commandResult;
        command.wakeupWaiter();
    }

    @Override
    public CommandResult sendCommand(Command command) throws SlockException {
        if(closed) {
            throw new ClientClosedException("client has been closed");
        }

        byte[] buf = command.dumpCommand();
        if (!command.createWaiter()) {
            throw new ClientCommandException("Adding a wait command returns waiter failure");
        }

        BytesKey requestId = new BytesKey(command.getRequestId());
        synchronized (this) {
            if(outputStream == null) {
                throw new ClientUnconnectException("client not connected " + host + ":" + port);
            }

            requests.put(requestId, command);
            try {
                outputStream.write(buf);
                byte[] extraData = command.getExtraData();
                if (extraData != null) {
                    outputStream.write(extraData);
                }
            } catch (IOException e) {
                requests.remove(requestId);
                try {
                    socket.close();
                } catch (IOException ignored) {}
                throw new ClientOutputStreamException("Client writes data abnormally: " + e);
            }
        }

        if(!command.waiteWaiter()) {
            requests.remove(requestId);
            throw new ClientCommandTimeoutException("The client waits for command execution to return a timeout");
        }

        if(command.commandResult == null) {
            throw new ClientClosedException("client has been closed");
        }
        return command.commandResult;
    }

    @Override
    public void sendCommand(Command command, Consumer<CallbackCommandResult> callback) throws SlockException {
        if(closed) {
            throw new ClientClosedException("client has been closed");
        }
        if (callbackExecutorManager == null) {
            throw new ClientAsyncCallbackDisabledException("The asynchronous thread pool is not enabled. First enableAsyncCallback to enable the asynchronous thread pool.");
        }

        byte[] buf = command.dumpCommand();
        BytesKey requestId = new BytesKey(command.getRequestId());
        CallbackCommand callbackCommand = callbackExecutorManager.addCommand(command, callback, callbackCommandResult -> {
            requests.remove(requestId);
            callback.accept(callbackCommandResult);
        });

        synchronized (this) {
            if(outputStream == null) {
                throw new ClientUnconnectException("client not connected " + host + ":" + port);
            }

            requests.put(requestId, command);
            try {
                outputStream.write(buf);
                byte[] extraData = command.getExtraData();
                if (extraData != null) {
                    outputStream.write(extraData);
                }
            } catch (IOException e) {
                requests.remove(requestId);
                callbackCommand.close();
                try {
                    socket.close();
                } catch (IOException ignored) {}
                throw new ClientOutputStreamException("Client writes data abnormally: " + e);
            }
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
