package com.github.snower.slock;

import com.github.snower.slock.commands.ICommand;
import com.github.snower.slock.commands.Command;
import com.github.snower.slock.commands.CommandResult;
import com.github.snower.slock.commands.LockCommandResult;
import com.github.snower.slock.exceptions.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;

public class Client implements Runnable, IClient {
    private String host;
    private int port;
    private boolean closed = false;
    private Thread thread = null;
    private Socket socket = null;
    private InputStream inputStream = null;
    private OutputStream outputStream = null;
    private Database[] databases;
    private HashMap<String, Command> requests;
    private ReplsetClient replsetClient = null;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
        this.databases = new Database[256];
        this.requests = new HashMap<>();
    }

    public Client(String host, int port, ReplsetClient replsetClient, Database[] databases) {
        this.host = host;
        this.port = port;
        this.replsetClient = replsetClient;
        this.databases = databases;
        this.requests = new HashMap<>();
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
            for(Command command : requests.values()) {
                command.commandResult = null;
                command.wakeupWaiter();
            }
            requests = new HashMap<>();

            for(Database database : databases) {
                if(database != null ) {
                    database.close();
                }
                databases = new Database[256];
            }
        }
    }

    @Override
    public void run() {
        try {
            while (!closed) {
                byte[] buf = new byte[64];
                while (!closed && socket != null) {
                    try {
                        int n = inputStream.readNBytes(buf, 0, 64);
                        if (n < 64) {
                            break;
                        }
                    } catch (IOException ignored) {
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
                reconnect();
            }
        } finally {
            if(socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
                socket = null;
                inputStream = null;
                outputStream = null;
                if(replsetClient != null) {
                    replsetClient.removeLivedClient(this);
                }
            }
            thread = null;
        }
        replsetClient = null;
    }

    protected void connect() throws IOException {
        socket = new Socket(host, port);
        try {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            if(replsetClient != null) {
                replsetClient.addLivedClient(this);
            }
        } catch (IOException e) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            socket = null;
            inputStream = null;
            outputStream = null;
            throw e;
        }
    }

    protected void reconnect() {
        for(Command command : requests.values()) {
            command.commandResult = null;
            command.wakeupWaiter();
        }
        requests = new HashMap<>();

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

    protected void handleCommand(CommandResult commandResult) {
        Command command = null;
        String requestId = new String(commandResult.getRequestId());
        synchronized (this) {
            if (!requests.containsKey(requestId)) {
                return;
            }

            command = requests.remove(requestId);
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
                throw new ClientOutputStreamException();
            }
        }

        if(!command.waiteWaiter()) {
            synchronized (this) {
                requests.remove(requestId);
            }
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
}
