package io.github.snower.jaslock;

import io.github.snower.jaslock.commands.ICommand;
import io.github.snower.jaslock.commands.LockCommand;
import io.github.snower.jaslock.exceptions.LockLockedException;
import io.github.snower.jaslock.exceptions.SlockException;

import java.nio.charset.StandardCharsets;

public class TreeLock {
    private final SlockDatabase database;
    private final byte[] parentKey;
    private final byte[] lockKey;
    private final int timeout;
    private final int expried;
    private final boolean isRoot;

    public TreeLock(SlockDatabase database, byte[] parentKey, byte[] lockKey, int timeout, int expried) {
        this.database = database;
        this.parentKey = parentKey;
        this.lockKey = lockKey;
        this.timeout = timeout;
        this.expried = expried;
        this.isRoot = parentKey == null;
    }

    public TreeLock(SlockDatabase database, String parentKey, String lockKey, int timeout, int expried) {
        this(database, parentKey.getBytes(StandardCharsets.UTF_8), lockKey.getBytes(StandardCharsets.UTF_8), timeout, expried);
    }

    public TreeLock(SlockDatabase database, byte[] lockKey, int timeout, int expried) {
        this(database, null, lockKey, timeout, expried);
    }

    public TreeLock(SlockDatabase database, String lockKey, int timeout, int expried) {
        this(database, null, lockKey.getBytes(StandardCharsets.UTF_8), timeout, expried);
    }

    public TreeLockLock newLock() {
        return new TreeLockLock(database, this, new Lock(database, lockKey, LockCommand.genLockId(), timeout, expried, (short) 0xffff, (byte) 0));
    }

    public TreeLockLock loadLock(byte[] lockId) {
        return new TreeLockLock(database, this, new Lock(database, lockKey, lockId, timeout, expried, (short) 0xffff, (byte) 0));
    }

    public TreeLock newChild() {
        return new TreeLock(database, lockKey, LockCommand.genLockId(), timeout, expried);
    }

    public TreeLock loadChild(byte[] lockKey) {
        return new TreeLock(database, this.lockKey, lockKey, timeout, expried);
    }

    public byte[] getParentKey() {
        return parentKey;
    }

    public byte[] getLockKey() {
        return lockKey;
    }

    public boolean isRoot() {
        return isRoot;
    }

    public class TreeLockLock {
        private final SlockDatabase database;
        private final TreeLock treeLock;
        private final Lock lock;

        public TreeLockLock(SlockDatabase database, TreeLock treeLock, Lock lock) {
            this.database = database;
            this.treeLock = treeLock;
            this.lock = lock;
        }

        public void acquire() throws SlockException {
            Lock childCheckLock = null;
            Lock parentCheckLock = null;

            if (!treeLock.isRoot()) {
                childCheckLock = new Lock(database, treeLock.getLockKey(), treeLock.getParentKey(), 0, expried, (short) 0xffff, (byte) 0);
                try {
                    childCheckLock.acquire(ICommand.LOCK_FLAG_LOCK_TREE_LOCK);
                    parentCheckLock = new Lock(database, treeLock.getParentKey(), LockCommand.genLockId(), 0, expried, (short) 0xffff, (byte) 0);
                    try {
                        parentCheckLock.acquire();
                    } catch (LockLockedException ignored) {
                    } catch (Exception e) {
                        childCheckLock.release();
                        throw e;
                    }
                } catch (LockLockedException ignored) {
                }
            }

            try {
                lock.acquire();
            } catch (Exception e) {
                if (childCheckLock != null) {
                    try {
                        childCheckLock.release();
                    } catch (SlockException ignored) {
                    }
                }
                if (parentCheckLock != null) {
                    try {
                        parentCheckLock.release();
                    } catch (SlockException ignored) {
                    }
                }
                throw e;
            }
        }

        public void release() throws SlockException {
            lock.release(ICommand.UNLOCK_FLAG_UNLOCK_TREE_LOCK);
        }

        public byte[] getLockKey() {
            return lock.getLockKey();
        }

        public byte[] getLockId() {
            return lock.getLockId();
        }
    }
}
