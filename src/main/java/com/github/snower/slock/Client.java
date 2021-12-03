package com.github.snower.slock;

import com.github.snower.slock.commands.*;
import com.github.snower.slock.exceptions.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class Client implements Runnable, IClient {
    private String host;
    private int port;
    private boolean closed;
    private byte[] clientId;
    private Thread thread;
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Database[] databases;
    private ConcurrentHashMap<String, Command> requests;
    private ReplsetClient replsetClient;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
        this.databases = new Database[256];
        this.requests = new ConcurrentHashMap<>();
        this.closed = false;
    }

    public Client(String host, int port, ReplsetClient replsetClient, Database[] databases) {
        this(host, port);
        this.replsetClient = replsetClient;
        this.databases = databases;
        this.requests = new ConcurrentHashMap<>();
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
    }

    public boolean tryOpen() {
        try {
            open();
            return true;
        } catch (IOException e) {
            return false;
        }
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
    }

    @Override
    public void run() {
        try {
            while (!closed) {
                try {
                    byte[] buf = new byte[64];
                    while (!closed && socket != null) {
                        try {
                            int n = inputStream.readNBytes(buf, 0, 64);
                            if (n < 64) {
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
                        }
                    }
                } catch (Exception ignored) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored1) {
                    }
                }

                closeSocket();
                reconnect();
            }
        } finally {
            closeSocket();
            thread = null;
        }
        replsetClient = null;
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

        initClient();
        if(replsetClient != null) {
            replsetClient.addLivedClient(this);
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

    protected void initClient() throws IOException {
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
            int n = inputStream.readNBytes(buf, 0, 64);
            if (n < 64) {
                throw new IOException("read result error");
            }
            InitResultCommand initResultCommand = new InitResultCommand();
            if (initResultCommand.parseCommand(buf) != null) {
                if(initResultCommand.getResult() != ICommand.COMMAND_RESULT_SUCCED) {
                    throw new IOException("init commnad error");
                }
            }
        } catch (IOException e) {
            closeSocket();
            throw e;
        }
    }

    protected void handleCommand(CommandResult commandResult) {
        Command command;
        String requestId = new String(commandResult.getRequestId());
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

        String requestId = new String(command.getRequestId());
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
