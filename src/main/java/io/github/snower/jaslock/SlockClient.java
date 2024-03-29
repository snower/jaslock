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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class SlockClient implements Runnable, ISlockClient {
    private static final char[] DIGITS_LOWER = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private static String encodeHex(byte[] data) {
        int l = data.length;
        char[] out = new char[l << 1];
        for(int i = 0, j = 0; i < l; i++) {
            out[j++] = DIGITS_LOWER[(240 & data[i]) >>> 4];
            out[j++] = DIGITS_LOWER[15 & data[i]];
        }
        return new String(out);
    }

    private final String host;
    private final int port;
    private boolean closed;
    private byte[] clientId;
    private Thread thread;
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private SlockDatabase[] databases;
    private ConcurrentHashMap<String, Command> requests;
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
    public void open() throws IOException {
        if(thread != null) {
            return;
        }
        connect();
        thread = new Thread(this);
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
            thread = new Thread(this);
            thread.setDaemon(true);
            thread.start();

            if (callbackExecutorManager != null && replsetClient == null) {
                callbackExecutorManager.start();
            }
            return null;
        }
        thread = new Thread(this);
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
            for(Object requestId : requests.keySet().toArray()) {
                Command command = requests.remove((String) requestId);
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
                            int n = inputStream.read(buf, 0, 64);
                            while (n > 0 && n < 64) {
                                int nn = inputStream.read(buf, n, 64 - n);
                                if(nn <= 0) {
                                    break;
                                }
                                n += nn;
                            }
                            if(n < 64) {
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
                    } catch (InterruptedException ignored1) {
                    }
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
            for(Object requestId : requests.keySet().toArray()) {
                Command command = requests.remove((String) requestId);
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
            int n = inputStream.read(buf, 0, 64);
            while (n > 0 && n < 64) {
                int nn = inputStream.read(buf, n, 64 - n);
                if(nn <= 0) {
                    break;
                }
                n += nn;
            }
            if (n < 64) {
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
        Command command;
        String requestId = encodeHex(commandResult.getRequestId());
        if (!requests.containsKey(requestId)) {
            return;
        }
        command = requests.remove(requestId);
        if(command == null) {
            return;
        }
        command.commandResult = commandResult;
        command.wakeupWaiter();
    }

    @Override
    public CommandResult sendCommand(Command command) throws SlockException {
        if(closed) {
            throw new ClientClosedException();
        }

        byte[] buf = command.dumpCommand();
        if (!command.createWaiter()) {
            throw new ClientCommandException();
        }

        String requestId = encodeHex(command.getRequestId());
        synchronized (this) {
            if(outputStream == null) {
                throw new ClientUnconnectException();
            }

            requests.put(requestId, command);
            try {
                outputStream.write(buf);
            } catch (IOException e) {
                requests.remove(requestId);
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
                throw new ClientOutputStreamException();
            }
        }

        if(!command.waiteWaiter()) {
            requests.remove(requestId);
            throw new ClientCommandTimeoutException();
        }

        if(command.commandResult == null) {
            throw new ClientClosedException();
        }
        return command.commandResult;
    }

    @Override
    public void sendCommand(Command command, Consumer<CallbackCommandResult> callback) throws SlockException {
        if(closed) {
            throw new ClientClosedException();
        }
        if (callbackExecutorManager == null) {
            throw new ClientAsyncCallbackDisabledException();
        }

        byte[] buf = command.dumpCommand();
        String requestId = encodeHex(command.getRequestId());
        CallbackCommand callbackCommand = callbackExecutorManager.addCommand(command, callback, callbackCommandResult -> {
            requests.remove(requestId);
            callback.accept(callbackCommandResult);
        });

        synchronized (this) {
            if(outputStream == null) {
                throw new ClientUnconnectException();
            }

            requests.put(requestId, command);
            try {
                outputStream.write(buf);
            } catch (IOException e) {
                requests.remove(requestId);
                callbackCommand.close();
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
                throw new ClientOutputStreamException();
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
