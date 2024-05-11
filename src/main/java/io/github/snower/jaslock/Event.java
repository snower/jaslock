package io.github.snower.jaslock;

import io.github.snower.jaslock.callback.CallbackFuture;
import io.github.snower.jaslock.commands.ICommand;
import io.github.snower.jaslock.commands.LockCommandResult;
import io.github.snower.jaslock.datas.LockData;
import io.github.snower.jaslock.datas.LockSetData;
import io.github.snower.jaslock.exceptions.*;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class Event extends AbstractExecution {
    private Lock eventLock;
    private Lock checkLock;
    private Lock waitLock;
    private final boolean defaultSeted;

    public Event(SlockDatabase database, byte[] eventKey, int timeout, int expried, boolean defaultSeted) {
        super(database, eventKey, timeout, expried);
        this.defaultSeted = defaultSeted;
    }

    public Event(SlockDatabase database, byte[] eventKey, int timeout, int expried) {
        this(database, eventKey, timeout, expried, true);
    }

    public Event(SlockDatabase database, String eventKey, int timeout, int expried, boolean defaultSeted) {
        this(database, eventKey.getBytes(StandardCharsets.UTF_8), timeout, expried, defaultSeted);
    }

    public Event(SlockDatabase database, String eventKey, int timeout, int expried) {
        this(database, eventKey.getBytes(StandardCharsets.UTF_8), timeout, expried, true);
    }

    public void clear() throws SlockException {
        clear((LockData) null);
    }

    public void clear(byte[] data) throws SlockException {
        clear(data != null ? new LockSetData(data) : null);
    }

    public void clear(String data) throws SlockException {
        clear(data != null ? new LockSetData(data) : null);
    }

    public void clear(LockData lockData) throws SlockException {
        if(defaultSeted) {
            synchronized (this) {
                if(eventLock == null) {
                    eventLock = new Lock(database, lockKey, lockKey, timeout, expried, (short) 0, (byte) 0);
                }
            }
            try {
                eventLock.acquire(ICommand.LOCK_FLAG_UPDATE_WHEN_LOCKED, lockData);
            } catch (LockLockedException ignored) {}
            return;
        }

        synchronized (this) {
            if(eventLock == null) {
                eventLock = new Lock(database, lockKey, lockKey, timeout, expried, (short) 1, (byte) 0);
            }
        }
        try {
            eventLock.release((byte) 0, lockData);
        } catch (LockUnlockedException ignored) {}
    }

    public CallbackFuture<Boolean> clear(Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        return clear((LockData) null, callback);
    }

    public CallbackFuture<Boolean> clear(byte[] data, Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        return clear(data != null ? new LockSetData(data) : null, callback);
    }

    public CallbackFuture<Boolean> clear(String data, Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        return clear(data != null ? new LockSetData(data) : null, callback);
    }

    public CallbackFuture<Boolean> clear(LockData lockData, Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        CallbackFuture<Boolean> callbackFuture = new CallbackFuture<>(callback);
        if(defaultSeted) {
            synchronized (this) {
                if(eventLock == null) {
                    eventLock = new Lock(database, lockKey, lockKey, timeout, expried, (short) 0, (byte) 0);
                }
            }
            eventLock.acquire(ICommand.LOCK_FLAG_UPDATE_WHEN_LOCKED, lockData, callbackCommandResult -> {
                try {
                    callbackCommandResult.getResult();
                    callbackFuture.setResult(true);
                } catch (LockLockedException ignored) {
                    callbackFuture.setResult(true);
                } catch (SlockException e) {
                    callbackFuture.setResult(false, e);
                }
            });
            return callbackFuture;
        }

        synchronized (this) {
            if(eventLock == null) {
                eventLock = new Lock(database, lockKey, lockKey, timeout, expried, (short) 1, (byte) 0);
            }
        }
        eventLock.release((byte) 0, lockData, callbackCommandResult -> {
            try {
                callbackCommandResult.getResult();
                callbackFuture.setResult(true);
            } catch (LockUnlockedException ignored) {
                callbackFuture.setResult(true);
            } catch (SlockException e) {
                callbackFuture.setResult(false, e);
            }
        });
        return callbackFuture;
    }

    public void set() throws SlockException {
        set((LockData) null);
    }

    public void set(byte[] data) throws SlockException {
        set(data != null ? new LockSetData(data) : null);
    }

    public void set(String data) throws SlockException {
        set(data != null ? new LockSetData(data) : null);
    }

    public void set(LockData lockData) throws SlockException {
        if(defaultSeted) {
            synchronized (this) {
                if(eventLock == null) {
                    eventLock = new Lock(database, lockKey, lockKey, timeout, expried, (short) 0, (byte) 0);
                }
            }
            try {
                eventLock.release((byte) 0, lockData);
            } catch (LockUnlockedException ignored) {
            }
            return;
        }

        synchronized (this) {
            if(eventLock == null) {
                eventLock = new Lock(database, lockKey, lockKey, timeout, expried, (short) 1, (byte) 0);
            }
        }
        try {
            eventLock.acquire(ICommand.LOCK_FLAG_UPDATE_WHEN_LOCKED, lockData);
        } catch (LockLockedException ignored) {
        }
    }

    public CallbackFuture<Boolean> set(Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        return set((LockData) null, callback);
    }

    public CallbackFuture<Boolean> set(byte[] data, Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        return set(data != null ? new LockSetData(data) : null, callback);
    }

    public CallbackFuture<Boolean> set(String data, Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        return set(data != null ? new LockSetData(data) : null, callback);
    }

    public CallbackFuture<Boolean> set(LockData lockData, Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        CallbackFuture<Boolean> callbackFuture = new CallbackFuture<>(callback);
        if(defaultSeted) {
            synchronized (this) {
                if(eventLock == null) {
                    eventLock = new Lock(database, lockKey, lockKey, timeout, expried, (short) 0, (byte) 0);
                }
            }
            eventLock.release((byte) 0, lockData, callbackCommandResult -> {
                try {
                    callbackCommandResult.getResult();
                    callbackFuture.setResult(true);
                } catch (LockUnlockedException ignored) {
                    callbackFuture.setResult(true);
                } catch (SlockException e) {
                    callbackFuture.setResult(false, e);
                }
            });
            return callbackFuture;
        }

        synchronized (this) {
            if(eventLock == null) {
                eventLock = new Lock(database, lockKey, lockKey, timeout, expried, (short) 1, (byte) 0);
            }
        }
        eventLock.acquire(ICommand.LOCK_FLAG_UPDATE_WHEN_LOCKED, lockData, callbackCommandResult -> {
            try {
                callbackCommandResult.getResult();
                callbackFuture.setResult(true);
            } catch (LockLockedException ignored) {
                callbackFuture.setResult(true);
            } catch (SlockException e) {
                callbackFuture.setResult(false, e);
            }
        });
        return callbackFuture;
    }

    public boolean isSet() throws SlockException {
        if(defaultSeted) {
            synchronized (this) {
                if(checkLock == null) {
                    checkLock = new Lock(database, lockKey, null, 0, 0, (short) 0, (byte) 0);
                }
            }
            try {
                checkLock.acquire();
            } catch (LockTimeoutException ignored) {
                return false;
            }
            return true;
        }

        synchronized (this) {
            if(checkLock == null) {
                checkLock = new Lock(database, lockKey, null, 0x02000000, 0, (short) 1, (byte) 0);
            }
        }
        try {
            checkLock.acquire();
        } catch (LockNotOwnException | LockTimeoutException ignored) {
            return false;
        }
        return true;
    }

    public CallbackFuture<Boolean> isSet(Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        CallbackFuture<Boolean> callbackFuture = new CallbackFuture<>(callback);
        if(defaultSeted) {
            synchronized (this) {
                if(checkLock == null) {
                    checkLock = new Lock(database, lockKey, null, 0, 0, (short) 0, (byte) 0);
                }
            }
            checkLock.acquire((byte) 0, callbackCommandResult -> {
                try {
                    callbackCommandResult.getResult();
                    callbackFuture.setResult(true);
                } catch (LockTimeoutException ignored) {
                    callbackFuture.setResult(false);
                } catch (SlockException e) {
                    callbackFuture.setResult(false, e);
                }
            });
            return callbackFuture;
        }

        synchronized (this) {
            if(checkLock == null) {
                checkLock = new Lock(database, lockKey, null, 0x02000000, 0, (short) 1, (byte) 0);
            }
        }
        checkLock.acquire((byte) 0, callbackCommandResult -> {
            try {
                callbackCommandResult.getResult();
                callbackFuture.setResult(true);
            } catch (LockNotOwnException | LockTimeoutException ignored) {
                callbackFuture.setResult(false);
            } catch (SlockException e) {
                callbackFuture.setResult(false, e);
            }
        });
        return callbackFuture;
    }

    public void wait(int timeout) throws SlockException {
        if(defaultSeted) {
            synchronized (this) {
                if(waitLock == null) {
                    waitLock = new Lock(database, lockKey, null, timeout, 0, (short) 0, (byte) 0);
                }
            }
            try {
                waitLock.acquire();
            } catch (LockTimeoutException | ClientCommandTimeoutException ignored) {
                throw new EventWaitTimeoutException();
            }
            currentLockData = waitLock.getCurrentLockData();
            return;
        }

        synchronized (this) {
            if(waitLock == null) {
                waitLock = new Lock(database, lockKey, null, timeout | 0x02000000, 0, (short) 1, (byte) 0);
            }
        }
        try {
            waitLock.acquire();
        } catch (LockTimeoutException | ClientCommandTimeoutException ignored) {
            throw new EventWaitTimeoutException();
        }
        currentLockData = waitLock.getCurrentLockData();
    }

    public void waitAndTimeoutRetryClear(int timeout) throws SlockException {
        if(defaultSeted) {
            synchronized (this) {
                if(waitLock == null) {
                    waitLock = new Lock(database, lockKey, null, timeout, 0, (short) 0, (byte) 0);
                }
            }

            try {
                waitLock.acquire();
            } catch (LockTimeoutException | ClientCommandTimeoutException ignored) {
                synchronized (this) {
                    if(eventLock == null) {
                        eventLock = new Lock(database, lockKey, lockKey, this.timeout, expried, (short) 0, (byte) 0);
                    }
                }

                try {
                    eventLock.acquire(ICommand.LOCK_FLAG_UPDATE_WHEN_LOCKED);
                    try {
                        eventLock.release();
                    } catch (SlockException ignored2) {}
                    currentLockData = waitLock.getCurrentLockData();
                    return;
                } catch (SlockException ignored2) {}
                throw new EventWaitTimeoutException();
            }
            currentLockData = waitLock.getCurrentLockData();
            return;
        }

        synchronized (this) {
            if(waitLock == null) {
                waitLock = new Lock(database, lockKey, null, timeout | 0x02000000, 0, (short) 1, (byte) 0);
            }
        }
        try {
            waitLock.acquire();
        } catch (LockTimeoutException | ClientCommandTimeoutException ignored) {
            throw new EventWaitTimeoutException();
        }
        currentLockData = waitLock.getCurrentLockData();
        synchronized (this) {
            if(eventLock == null) {
                eventLock = new Lock(database, lockKey, lockKey, this.timeout, expried, (short) 1, (byte) 0);
            }
        }
        try {
            eventLock.release();
        } catch (SlockException ignored) {}
    }

    public CallbackFuture<Boolean> wait(int timeout, Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        CallbackFuture<Boolean> callbackFuture = new CallbackFuture<>(callback);
        if(defaultSeted) {
            synchronized (this) {
                if(waitLock == null) {
                    waitLock = new Lock(database, lockKey, null, timeout, 0, (short) 0, (byte) 0);
                }
            }
            waitLock.acquire((byte) 0, callbackCommandResult -> {
                try {
                    callbackCommandResult.getResult();
                } catch (LockTimeoutException | ClientCommandTimeoutException ignored) {
                    callbackFuture.setResult(false, new EventWaitTimeoutException());
                    return;
                } catch (SlockException e) {
                    callbackFuture.setResult(false, e);
                    return;
                }
                currentLockData = ((LockCommandResult) callbackCommandResult.getCommandResult()).getLockResultData();
                callbackFuture.setResult(true);
            });
            return callbackFuture;
        }

        synchronized (this) {
            if(waitLock == null) {
                waitLock = new Lock(database, lockKey, null, timeout | 0x02000000, 0, (short) 1, (byte) 0);
            }
        }
        waitLock.acquire((byte) 0, callbackCommandResult -> {
            try {
                callbackCommandResult.getResult();
            } catch (LockTimeoutException | ClientCommandTimeoutException ignored) {
                callbackFuture.setResult(false, new EventWaitTimeoutException());
                return;
            } catch (SlockException e) {
                callbackFuture.setResult(false, e);
                return;
            }
            currentLockData = ((LockCommandResult) callbackCommandResult.getCommandResult()).getLockResultData();
            callbackFuture.setResult(true);
        });
        return callbackFuture;
    }

    public CallbackFuture<Boolean> waitAndTimeoutRetryClear(int timeout, Consumer<CallbackFuture<Boolean>> callback) throws SlockException {
        CallbackFuture<Boolean> callbackFuture = new CallbackFuture<>(callback);
        if(defaultSeted) {
            synchronized (this) {
                if(waitLock == null) {
                    waitLock = new Lock(database, lockKey, null, timeout, 0, (short) 0, (byte) 0);
                }
            }

            waitLock.acquire((byte) 0, callbackCommandResult -> {
                try {
                    callbackCommandResult.getResult();
                } catch (LockTimeoutException | ClientCommandTimeoutException ignored) {
                    synchronized (this) {
                        if (eventLock == null) {
                            eventLock = new Lock(database, lockKey, lockKey, this.timeout, expried, (short) 0, (byte) 0);
                        }
                    }

                    try {
                        eventLock.acquire(ICommand.LOCK_FLAG_UPDATE_WHEN_LOCKED);
                        try {
                            eventLock.release();
                        } catch (SlockException ignored2) {}
                        currentLockData = eventLock.getCurrentLockData();
                        callbackFuture.setResult(true);
                        return;
                    } catch (SlockException ignored2) {}
                    callbackFuture.setResult(false, new EventWaitTimeoutException());
                    return;
                } catch (SlockException e) {
                    callbackFuture.setResult(false, e);
                    return;
                }
                currentLockData = ((LockCommandResult) callbackCommandResult.getCommandResult()).getLockResultData();
                callbackFuture.setResult(true);
            });
            return callbackFuture;
        }

        synchronized (this) {
            if(waitLock == null) {
                waitLock = new Lock(database, lockKey, null, timeout | 0x02000000, 0, (short) 1, (byte) 0);
            }
        }
        waitLock.acquire((byte) 0, callbackCommandResult -> {
            try {
                callbackCommandResult.getResult();
            } catch (LockTimeoutException | ClientCommandTimeoutException ignored) {
                callbackFuture.setResult(false, new EventWaitTimeoutException());
                return;
            } catch (SlockException e) {
                callbackFuture.setResult(false, e);
                return;
            }
            synchronized (this) {
                if (eventLock == null) {
                    eventLock = new Lock(database, lockKey, lockKey, this.timeout, expried, (short) 1, (byte) 0);
                }
            }
            try {
                eventLock.release();
            } catch (SlockException ignored) {}
            currentLockData = ((LockCommandResult) callbackCommandResult.getCommandResult()).getLockResultData();
            callbackFuture.setResult(true);
        });
        return callbackFuture;
    }
}
