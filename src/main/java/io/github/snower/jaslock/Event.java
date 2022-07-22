package io.github.snower.jaslock;

import io.github.snower.jaslock.callback.DeferredResult;
import io.github.snower.jaslock.commands.ICommand;
import io.github.snower.jaslock.exceptions.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.function.Consumer;

public class Event {
    private final SlockDatabase database;
    private byte[] eventKey;
    private final int timeout;
    private final int expried;
    private Lock eventLock;
    private Lock checkLock;
    private Lock waitLock;
    private final boolean defaultSeted;

    public Event(SlockDatabase database, byte[] eventKey, int timeout, int expried, boolean defaultSeted) {
        this.database = database;
        if(eventKey.length > 16) {
            try {
                MessageDigest digest = MessageDigest.getInstance("MD5");
                this.eventKey = digest.digest(eventKey);
            } catch (NoSuchAlgorithmException e) {
                this.eventKey = Arrays.copyOfRange(eventKey, 0, 16);
            }
        } else {
            this.eventKey = new byte[16];
            System.arraycopy(eventKey, 0, this.eventKey, 16 - eventKey.length, eventKey.length);
        }
        this.timeout = timeout;
        this.expried = expried;
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
        if(defaultSeted) {
            synchronized (this) {
                if(eventLock == null) {
                    eventLock = new Lock(database, eventKey, eventKey, timeout, expried, (short) 0, (byte) 0);
                }
            }
            try {
                eventLock.acquire(ICommand.LOCK_FLAG_UPDATE_WHEN_LOCKED);
            } catch (LockLockedException ignored) {
            }
            return;
        }

        synchronized (this) {
            if(eventLock == null) {
                eventLock = new Lock(database, eventKey, eventKey, timeout, expried, (short) 1, (byte) 0);
            }
        }
        try {
            eventLock.release();
        } catch (LockUnlockedException ignored) {
        }
    }

    public void set() throws SlockException {
        if(defaultSeted) {
            synchronized (this) {
                if(eventLock == null) {
                    eventLock = new Lock(database, eventKey, eventKey, timeout, expried, (short) 0, (byte) 0);
                }
            }
            try {
                eventLock.release();
            } catch (LockUnlockedException ignored) {
            }
            return;
        }

        synchronized (this) {
            if(eventLock == null) {
                eventLock = new Lock(database, eventKey, eventKey, timeout, expried, (short) 1, (byte) 0);
            }
        }
        try {
            eventLock.acquire(ICommand.LOCK_FLAG_UPDATE_WHEN_LOCKED);
        } catch (LockLockedException ignored) {
        }
    }

    public boolean isSet() throws SlockException {
        if(defaultSeted) {
            synchronized (this) {
                if(checkLock == null) {
                    checkLock = new Lock(database, eventKey, null, 0, 0, (short) 0, (byte) 0);
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
                checkLock = new Lock(database, eventKey, null, 0x02000000, 0, (short) 1, (byte) 0);
            }
        }
        try {
            checkLock.acquire();
        } catch (LockNotOwnException | LockTimeoutException ignored) {
            return false;
        }
        return true;
    }

    public void wait(int timeout) throws SlockException {
        if(defaultSeted) {
            synchronized (this) {
                if(waitLock == null) {
                    waitLock = new Lock(database, eventKey, null, timeout, 0, (short) 0, (byte) 0);
                }
            }
            try {
                waitLock.acquire();
            } catch (LockTimeoutException | ClientCommandTimeoutException ignored) {
                throw new EventWaitTimeoutException();
            }
            return;
        }

        synchronized (this) {
            if(waitLock == null) {
                waitLock = new Lock(database, eventKey, null, timeout | 0x02000000, 0, (short) 1, (byte) 0);
            }
        }
        try {
            waitLock.acquire();
        } catch (LockTimeoutException | ClientCommandTimeoutException ignored) {
            throw new EventWaitTimeoutException();
        }
    }

    public void waitAndTimeoutRetryClear(int timeout) throws SlockException {
        if(defaultSeted) {
            synchronized (this) {
                if(waitLock == null) {
                    waitLock = new Lock(database, eventKey, null, timeout, 0, (short) 0, (byte) 0);
                }
            }

            try {
                waitLock.acquire();
            } catch (LockTimeoutException | ClientCommandTimeoutException ignored) {
                synchronized (this) {
                    if(eventLock == null) {
                        eventLock = new Lock(database, eventKey, eventKey, this.timeout, expried, (short) 0, (byte) 0);
                    }
                }

                try {
                    eventLock.acquire(ICommand.LOCK_FLAG_UPDATE_WHEN_LOCKED);
                    try {
                        eventLock.release();
                    } catch (SlockException ignored2) {}
                    return;
                } catch (SlockException ignored2) {}
                throw new EventWaitTimeoutException();
            }
            return;
        }

        synchronized (this) {
            if(waitLock == null) {
                waitLock = new Lock(database, eventKey, null, timeout | 0x02000000, 0, (short) 1, (byte) 0);
            }
        }
        try {
            waitLock.acquire();
        } catch (LockTimeoutException | ClientCommandTimeoutException ignored) {
            throw new EventWaitTimeoutException();
        }
        synchronized (this) {
            if(eventLock == null) {
                eventLock = new Lock(database, eventKey, eventKey, this.timeout, expried, (short) 1, (byte) 0);
            }
        }
        try {
            eventLock.release();
        } catch (SlockException ignored) {}
    }

    public void wait(int timeout, Consumer<DeferredResult<Object>> callback) throws SlockException {
        if(defaultSeted) {
            synchronized (this) {
                if(waitLock == null) {
                    waitLock = new Lock(database, eventKey, null, timeout, 0, (short) 0, (byte) 0);
                }
            }
            waitLock.acquire(deferredCommandResult -> {
                try {
                    deferredCommandResult.getResult();
                } catch (LockTimeoutException | ClientCommandTimeoutException ignored) {
                    callback.accept(new DeferredResult<>(new EventWaitTimeoutException()));
                    return;
                } catch (Exception e) {
                    callback.accept(new DeferredResult<>(e));
                    return;
                }
                callback.accept(new DeferredResult<>(null));
            });
            return;
        }

        synchronized (this) {
            if(waitLock == null) {
                waitLock = new Lock(database, eventKey, null, timeout | 0x02000000, 0, (short) 1, (byte) 0);
            }
        }
        waitLock.acquire(deferredCommandResult -> {
            try {
                deferredCommandResult.getResult();
            } catch (LockTimeoutException | ClientCommandTimeoutException ignored) {
                callback.accept(new DeferredResult<>(new EventWaitTimeoutException()));
                return;
            } catch (Exception e) {
                callback.accept(new DeferredResult<>(e));
                return;
            }
            callback.accept(new DeferredResult<>(null));
        });
    }

    public void waitAndTimeoutRetryClear(int timeout, Consumer<DeferredResult<Object>> callback) throws SlockException {
        if(defaultSeted) {
            synchronized (this) {
                if(waitLock == null) {
                    waitLock = new Lock(database, eventKey, null, timeout, 0, (short) 0, (byte) 0);
                }
            }

            waitLock.acquire(deferredCommandResult -> {
                try {
                    deferredCommandResult.getResult();
                } catch (LockTimeoutException | ClientCommandTimeoutException ignored) {
                    synchronized (this) {
                        if (eventLock == null) {
                            eventLock = new Lock(database, eventKey, eventKey, this.timeout, expried, (short) 0, (byte) 0);
                        }
                    }

                    try {
                        eventLock.acquire(ICommand.LOCK_FLAG_UPDATE_WHEN_LOCKED);
                        try {
                            eventLock.release();
                        } catch (SlockException ignored2) {}
                        callback.accept(new DeferredResult<>(null));
                        return;
                    } catch (SlockException ignored2) {}
                    callback.accept(new DeferredResult<>(new EventWaitTimeoutException()));
                    return;
                } catch (Exception e) {
                    callback.accept(new DeferredResult<>(e));
                    return;
                }
                callback.accept(new DeferredResult<>(null));
            });
            return;
        }

        synchronized (this) {
            if(waitLock == null) {
                waitLock = new Lock(database, eventKey, null, timeout | 0x02000000, 0, (short) 1, (byte) 0);
            }
        }
        waitLock.acquire(deferredCommandResult -> {
            try {
                deferredCommandResult.getResult();
            } catch (LockTimeoutException | ClientCommandTimeoutException ignored) {
                callback.accept(new DeferredResult<>(new EventWaitTimeoutException()));
                return;
            } catch (Exception e) {
                callback.accept(new DeferredResult<>(e));
                return;
            }
            synchronized (this) {
                if (eventLock == null) {
                    eventLock = new Lock(database, eventKey, eventKey, this.timeout, expried, (short) 1, (byte) 0);
                }
            }
            try {
                eventLock.release();
            } catch (SlockException ignored) {}
            callback.accept(new DeferredResult<>(null));
        });
    }
}
