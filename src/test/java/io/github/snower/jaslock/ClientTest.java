package io.github.snower.jaslock;

import io.github.snower.jaslock.callback.CallbackCommandResult;
import io.github.snower.jaslock.callback.CallbackFuture;
import io.github.snower.jaslock.commands.ICommand;
import io.github.snower.jaslock.commands.LockCommand;
import io.github.snower.jaslock.datas.*;
import io.github.snower.jaslock.exceptions.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ClientTest
{
    static String clientHost = "127.0.0.1";
    static int clinetPort = 5658;

    @Test
    public void testClientLock() throws IOException, SlockException {
        SlockClient client = new SlockClient(clientHost, clinetPort);
        client.open();
        try {
            Lock lock = client.newLock("test1".getBytes(StandardCharsets.UTF_8), 5, 5);
            lock.acquire();
            lock.release();

            Lock lock1 = client.newLock("test1".getBytes(StandardCharsets.UTF_8), 5, 5);
            lock1.setCount((short) 10);
            Lock lock2 = client.newLock("test1".getBytes(StandardCharsets.UTF_8), 5, 5);
            lock2.setCount((short) 10);

            lock1.acquire(new LockSetData("aaa"));
            Assert.assertNull(lock1.getCurrentLockDataAsString());
            lock2.acquire(new LockSetData("bbb"));
            Assert.assertEquals(lock2.getCurrentLockDataAsString(), "aaa");
            lock1.release(new LockSetData("ccc"));
            Assert.assertEquals(lock1.getCurrentLockDataAsString(), "bbb");
            lock2.release();
            Assert.assertEquals(lock2.getCurrentLockDataAsString(), "ccc");
        } finally {
            client.close();
        }
    }

    @Test
    public void testReplsetClientLock() throws SlockException {
        SlockReplsetClient client = new SlockReplsetClient(new String[]{clientHost + ":" + clinetPort});
        client.open();
        try {
            Lock lock = client.newLock("test2".getBytes(StandardCharsets.UTF_8), 5, 5);
            lock.acquire();
            lock.release();

            Lock lock1 = client.newLock("test2".getBytes(StandardCharsets.UTF_8), 5, 5);
            lock1.setCount((short) 10);
            Lock lock2 = client.newLock("test2".getBytes(StandardCharsets.UTF_8), 5, 5);
            lock2.setCount((short) 10);

            lock1.acquire(new LockSetData("aaa"));
            Assert.assertNull(lock1.getCurrentLockDataAsString());
            lock2.acquire(new LockSetData("bbb"));
            Assert.assertEquals(lock2.getCurrentLockDataAsString(), "aaa");
            lock1.release(new LockSetData("ccc"));
            Assert.assertEquals(lock1.getCurrentLockDataAsString(), "bbb");
            lock2.release();
            Assert.assertEquals(lock2.getCurrentLockDataAsString(), "ccc");
        } finally {
            client.close();
        }
    }

    @Test
    public void testClientAsyncLock() throws SlockException, IOException {
        SlockClient client = new SlockClient(clientHost, clinetPort);
        client.enableAsyncCallback();
        client.open();
        try {
            Lock lock = client.newLock("test_async1".getBytes(StandardCharsets.UTF_8), 5, 5);
            try {
                Semaphore waiter = new Semaphore(1);
                AtomicReference<CallbackCommandResult> callbackCommandResult = new AtomicReference<>();
                waiter.acquire();
                lock.acquire((byte) 0, dcr -> {
                    callbackCommandResult.set(dcr);
                    waiter.release();
                });
                waiter.acquire();
                callbackCommandResult.get().getResult();
                lock.release((byte) 0, dcr -> {
                    callbackCommandResult.set(dcr);
                    waiter.release();
                });
                waiter.acquire();
                callbackCommandResult.get().getResult();
            } catch (InterruptedException e) {}
        } finally {
            client.close();
        }
    }

    @Test
    public void testReplsetClientAsyncLock() throws SlockException, ExecutionException, InterruptedException {
        SlockReplsetClient client = new SlockReplsetClient(new String[]{clientHost + ":" + clinetPort});
        client.enableAsyncCallback();
        client.open();
        try {
            Lock lock = client.newLock("test_async2".getBytes(StandardCharsets.UTF_8), 5, 5);
            CallbackFuture<Boolean> callbackFuture = lock.acquire((Consumer<CallbackFuture<Boolean>>) null);
            callbackFuture.get();
            callbackFuture = lock.release((Consumer<CallbackFuture<Boolean>>) null);
            callbackFuture.get();

            Lock lock1 = client.newLock("test_async2".getBytes(StandardCharsets.UTF_8), 5, 5);
            lock1.setCount((short) 10);
            Lock lock2 = client.newLock("test_async2".getBytes(StandardCharsets.UTF_8), 5, 5);
            lock2.setCount((short) 10);

            callbackFuture = lock1.acquire(new LockSetData("aaa"), null);
            callbackFuture.get();
            Assert.assertNull(lock1.getCurrentLockDataAsString());
            callbackFuture = lock2.acquire(new LockSetData("bbb"), null);
            callbackFuture.get();
            Assert.assertEquals(lock2.getCurrentLockDataAsString(), "aaa");
            callbackFuture = lock1.release(new LockSetData("ccc"), null);
            callbackFuture.get();
            Assert.assertEquals(lock1.getCurrentLockDataAsString(), "bbb");
            callbackFuture = lock2.release((Consumer<CallbackFuture<Boolean>>) null);
            callbackFuture.get();
            Assert.assertEquals(lock2.getCurrentLockDataAsString(), "ccc");
        } finally {
            client.close();
        }
    }

    @Test
    public void testEventDefaultSeted() throws IOException, SlockException {
        SlockClient client = new SlockClient(clientHost, clinetPort);
        client.open();

        try {
            Event event = client.newEvent("event1".getBytes(StandardCharsets.UTF_8), 5, 60, true);
            Assert.assertTrue(event.isSet());
            event.clear();
            Assert.assertFalse(event.isSet());
            event.set();
            Assert.assertTrue(event.isSet());
            event.wait(2);

            event = client.newEvent("event1".getBytes(StandardCharsets.UTF_8), 5, 60, true);
            Assert.assertTrue(event.isSet());
            event.clear();
            Assert.assertFalse(event.isSet());
            event.set("aaa");
            Assert.assertTrue(event.isSet());
            event.wait(2);
            Assert.assertEquals(event.getCurrentLockDataAsString(), "aaa");
        } finally {
            client.close();
        }
    }

    @Test
    public void testEventDefaultUnseted() throws IOException, SlockException {
        SlockClient client = new SlockClient(clientHost, clinetPort);
        client.open();

        try {
            Event event = client.newEvent("event2".getBytes(StandardCharsets.UTF_8), 5, 60, false);
            Assert.assertFalse(event.isSet());
            event.set();
            Assert.assertTrue(event.isSet());
            event.clear();
            Assert.assertFalse(event.isSet());
            event.set();
            Assert.assertTrue(event.isSet());
            event.wait(2);
            event.clear();

            event = client.newEvent("event2".getBytes(StandardCharsets.UTF_8), 5, 60, false);
            Assert.assertFalse(event.isSet());
            event.set("aaa");
            Assert.assertTrue(event.isSet());
            event.clear();
            Assert.assertFalse(event.isSet());
            event.set("bbb");
            Assert.assertTrue(event.isSet());
            event.wait(2);
            Assert.assertEquals(event.getCurrentLockDataAsString(), "bbb");
            event.clear();
        } finally {
            client.close();
        }
    }

    @Test
    public void testEventAsyncDefaultSeted() throws IOException, SlockException {
        SlockClient client = new SlockClient(clientHost, clinetPort);
        client.enableAsyncCallback();
        client.open();

        try {
            Event event = client.newEvent("event_async1".getBytes(StandardCharsets.UTF_8), 5, 60, true);
            Assert.assertTrue(event.isSet());
            event.clear();
            Assert.assertFalse(event.isSet());
            event.set();
            Assert.assertTrue(event.isSet());
            try {
                Semaphore waiter = new Semaphore(1);
                AtomicReference<CallbackFuture<Boolean>> callbackResult = new AtomicReference<>();
                waiter.acquire();
                event.wait(2, dr -> {
                    callbackResult.set(dr);
                    waiter.release();
                });
                waiter.acquire();
                callbackResult.get().getResult();
            } catch (InterruptedException e) {}

            event = client.newEvent("event_async1".getBytes(StandardCharsets.UTF_8), 5, 60, true);
            Assert.assertTrue(event.isSet());
            event.clear();
            Assert.assertFalse(event.isSet());
            event.set("aaa");
            Assert.assertTrue(event.isSet());
            try {
                Semaphore waiter = new Semaphore(1);
                AtomicReference<CallbackFuture<Boolean>> callbackResult = new AtomicReference<>();
                waiter.acquire();
                event.wait(2, dr -> {
                    callbackResult.set(dr);
                    waiter.release();
                });
                waiter.acquire();
                callbackResult.get().getResult();
                Assert.assertEquals(event.getCurrentLockDataAsString(), "aaa");
            } catch (InterruptedException e) {}
        } finally {
            client.close();
        }
    }

    @Test
    public void testEventAsyncDefaultUnseted() throws IOException, SlockException, ExecutionException, InterruptedException {
        SlockClient client = new SlockClient(clientHost, clinetPort);
        client.enableAsyncCallback();
        client.open();

        try {
            Event event = client.newEvent("event_async2".getBytes(StandardCharsets.UTF_8), 5, 60, false);
            CallbackFuture<Boolean> callbackFuture = event.isSet(null);
            Assert.assertFalse(callbackFuture.get());
            callbackFuture = event.set((Consumer<CallbackFuture<Boolean>>) null);
            callbackFuture.get();
            callbackFuture = event.isSet(null);
            Assert.assertTrue(callbackFuture.get());
            callbackFuture = event.clear((Consumer<CallbackFuture<Boolean>>) null);
            callbackFuture.get();
            callbackFuture = event.isSet(null);
            Assert.assertFalse(callbackFuture.get());
            callbackFuture = event.set((Consumer<CallbackFuture<Boolean>>) null);
            callbackFuture.get();
            callbackFuture = event.isSet(null);
            Assert.assertTrue(callbackFuture.get());
            callbackFuture = event.wait(2, null);
            callbackFuture.get();
            callbackFuture = event.clear((Consumer<CallbackFuture<Boolean>>) null);
            callbackFuture.get();

            event = client.newEvent("event_async2".getBytes(StandardCharsets.UTF_8), 5, 60, false);
            callbackFuture = event.isSet(null);
            Assert.assertFalse(callbackFuture.get());
            callbackFuture = event.set("aaa", (Consumer<CallbackFuture<Boolean>>) null);
            callbackFuture.get();
            callbackFuture = event.isSet(null);
            Assert.assertTrue(callbackFuture.get());
            callbackFuture = event.clear((Consumer<CallbackFuture<Boolean>>) null);
            callbackFuture.get();
            callbackFuture = event.isSet(null);
            Assert.assertFalse(callbackFuture.get());
            callbackFuture = event.set("bbb", (Consumer<CallbackFuture<Boolean>>) null);
            callbackFuture.get();
            callbackFuture = event.isSet(null);
            Assert.assertTrue(callbackFuture.get());
            callbackFuture = event.wait(2, null);
            callbackFuture.get();
            Assert.assertEquals(event.getCurrentLockDataAsString(), "bbb");
            callbackFuture = event.clear((Consumer<CallbackFuture<Boolean>>) null);
            callbackFuture.get();
        } finally {
            client.close();
        }
    }

    @Test
    public void testGroupEvent() throws IOException, SlockException {
        SlockClient client = new SlockClient(clientHost, clinetPort);
        client.open();

        try {
            GroupEvent groupEvent = client.newGroupEvent("groupEvent1".getBytes(StandardCharsets.UTF_8), 1, 1, 5, 60);
            GroupEvent groupEventWaiter = client.newGroupEvent("groupEvent1".getBytes(StandardCharsets.UTF_8), 2, 0, 5, 60);
            Assert.assertTrue(groupEvent.isSet());
            groupEvent.clear();
            Assert.assertFalse(groupEvent.isSet());

            groupEvent.wakeup("aaa");
            Assert.assertEquals(2, groupEvent.getVersionId());
            Assert.assertFalse(groupEvent.isSet());
            groupEventWaiter.wait(2);
            Assert.assertEquals(2, groupEventWaiter.getVersionId());
            Assert.assertEquals("aaa", groupEventWaiter.getCurrentLockDataAsString());

            groupEvent.wakeup("bbb");
            Assert.assertEquals(3, groupEvent.getVersionId());
            Assert.assertFalse(groupEvent.isSet());
            groupEventWaiter.wait(2);
            Assert.assertEquals(3, groupEventWaiter.getVersionId());
            Assert.assertEquals("bbb", groupEventWaiter.getCurrentLockDataAsString());

            groupEvent.set();
            Assert.assertTrue(groupEvent.isSet());
            groupEvent.wait(2);
            Assert.assertEquals(3, groupEvent.getVersionId());
        } finally {
            client.close();
        }
    }

    @Test
    public void testGroupEventAsync() throws IOException, SlockException, ExecutionException, InterruptedException {
        SlockClient client = new SlockClient(clientHost, clinetPort);
        client.enableAsyncCallback();
        client.open();

        try {
            GroupEvent groupEvent = client.newGroupEvent("groupEvent2".getBytes(StandardCharsets.UTF_8), 1, 1, 5, 10);
            GroupEvent groupEventWaiter = client.newGroupEvent("groupEvent2".getBytes(StandardCharsets.UTF_8), 2, 0, 5, 60);
            CallbackFuture<Boolean> callbackFuture = groupEvent.isSet(null);
            Assert.assertTrue(callbackFuture.get());
            callbackFuture = groupEvent.clear((Consumer<CallbackFuture<Boolean>>) null);
            callbackFuture.get();
            callbackFuture = groupEvent.isSet(null);
            Assert.assertFalse(callbackFuture.get());

            callbackFuture = groupEvent.wakeup("aaa", null);
            callbackFuture.get();
            Assert.assertEquals(2, groupEvent.getVersionId());
            callbackFuture = groupEvent.isSet(null);
            Assert.assertFalse(callbackFuture.get());
            callbackFuture = groupEventWaiter.wait(2, null);
            callbackFuture.get();
            Assert.assertEquals(2, groupEventWaiter.getVersionId());
            Assert.assertEquals("aaa", groupEventWaiter.getCurrentLockDataAsString());

            callbackFuture = groupEvent.wakeup("bbb", null);
            callbackFuture.get();
            Assert.assertEquals(3, groupEvent.getVersionId());
            callbackFuture = groupEvent.isSet(null);
            Assert.assertFalse(callbackFuture.get());
            callbackFuture = groupEventWaiter.wait(2, null);
            callbackFuture.get();
            Assert.assertEquals(3, groupEventWaiter.getVersionId());
            Assert.assertEquals("bbb", groupEventWaiter.getCurrentLockDataAsString());

            groupEvent.set((Consumer<CallbackFuture<Boolean>>) null);
            callbackFuture = groupEvent.isSet(null);
            Assert.assertTrue(callbackFuture.get());
            callbackFuture = groupEvent.wait(2, null);
            callbackFuture.get();
            Assert.assertEquals(3, groupEvent.getVersionId());
        } finally {
            client.close();
        }
    }

    @Test
    public void testReadWriteLock() throws IOException, SlockException {
        SlockClient client = new SlockClient(clientHost, clinetPort);
        client.open();

        try {
            ReadWriteLock readLock = client.newReadWriteLock("readWriteLock1".getBytes(StandardCharsets.UTF_8), 0, 60);
            ReadWriteLock writeLock = client.newReadWriteLock("readWriteLock1".getBytes(StandardCharsets.UTF_8), 0, 60);
            readLock.acquireRead();
            readLock.acquireRead();
            readLock.releaseRead();
            readLock.releaseRead();

            readLock.acquireRead();
            try {
                writeLock.acquireWrite();
                writeLock.releaseWrite();
                throw new SlockException("lock error");
            } catch (LockTimeoutException e) {}
            readLock.releaseRead();

            writeLock.acquireWrite();
            try {
                readLock.acquireRead();
                readLock.releaseRead();
                throw new SlockException("lock error");
            } catch (LockTimeoutException e) {}
            writeLock.releaseWrite();
            readLock.acquireRead();
            readLock.acquireRead();
            readLock.releaseRead();
            readLock.releaseRead();
        } finally {
            client.close();
        }
    }

    @Test
    public void testReadWriteLockAsync() throws IOException, SlockException, ExecutionException, InterruptedException {
        SlockClient client = new SlockClient(clientHost, clinetPort);
        client.enableAsyncCallback();
        client.open();

        try {
            ReadWriteLock readLock = client.newReadWriteLock("readWriteLock2".getBytes(StandardCharsets.UTF_8), 0, 60);
            ReadWriteLock writeLock = client.newReadWriteLock("readWriteLock2".getBytes(StandardCharsets.UTF_8), 0, 60);
            CallbackFuture<Boolean> callbackFuture = readLock.acquireRead(null);
            callbackFuture.get();
            callbackFuture = readLock.acquireRead(null);
            callbackFuture.get();
            callbackFuture = readLock.releaseRead(null);
            callbackFuture.get();
            callbackFuture = readLock.releaseRead(null);
            callbackFuture.get();

            callbackFuture = readLock.acquireRead(null);
            callbackFuture.get();
            try {
                callbackFuture = writeLock.acquireWrite(null);
                callbackFuture.get();
                callbackFuture = writeLock.releaseWrite(null);
                callbackFuture.get();
                throw new SlockException("lock error");
            } catch (ExecutionException e) {
                if (!(e.getCause() instanceof LockTimeoutException)) {
                    callbackFuture = writeLock.releaseWrite(null);
                    callbackFuture.get();
                    throw new SlockException("lock error");
                }
            }
            callbackFuture = readLock.releaseRead(null);
            callbackFuture.get();

            callbackFuture = writeLock.acquireWrite(null);
            callbackFuture.get();
            try {
                callbackFuture = readLock.acquireRead(null);
                callbackFuture.get();
                callbackFuture = readLock.releaseRead(null);
                callbackFuture.get();
                throw new SlockException("lock error");
            } catch (ExecutionException e) {
                if (!(e.getCause() instanceof LockTimeoutException)) {
                    callbackFuture = readLock.releaseRead(null);
                    callbackFuture.get();
                    throw new SlockException("lock error");
                }
            }
            callbackFuture = writeLock.releaseWrite(null);
            callbackFuture.get();
            callbackFuture = readLock.acquireRead(null);
            callbackFuture.get();
            callbackFuture = readLock.acquireRead(null);
            callbackFuture.get();
            callbackFuture = readLock.releaseRead(null);
            callbackFuture.get();
            callbackFuture = readLock.releaseRead(null);
            callbackFuture.get();
        } finally {
            client.close();
        }
    }

    @Test
    public void testReentrantLock() throws IOException, SlockException {
        SlockClient client = new SlockClient(clientHost, clinetPort);
        client.open();

        try {
            ReentrantLock reentrantLock = client.newReentrantLock("reentrantLock1".getBytes(StandardCharsets.UTF_8), 0, 60);
            for (int i = 0; i < 10; i++) {
                reentrantLock.acquire();
            }
            for (int i = 0; i < 10; i++) {
                reentrantLock.release();
            }
            Lock lock = client.newLock("reentrantLock1".getBytes(StandardCharsets.UTF_8), 0, 60);
            try {
                lock.releaseHead();
                throw new SlockException("lock error");
            } catch (LockUnlockedException e) {}
            lock.acquire();
            lock.release();
        } finally {
            client.close();
        }
    }

    @Test
    public void testReentrantLockAsync() throws IOException, SlockException, ExecutionException, InterruptedException {
        SlockClient client = new SlockClient(clientHost, clinetPort);
        client.enableAsyncCallback();
        client.open();

        try {
            ReentrantLock reentrantLock = client.newReentrantLock("reentrantLock2".getBytes(StandardCharsets.UTF_8), 0, 60);
            for (int i = 0; i < 10; i++) {
                CallbackFuture<Boolean> callbackFuture = reentrantLock.acquire(null);
                callbackFuture.get();
            }
            for (int i = 0; i < 10; i++) {
                CallbackFuture<Boolean> callbackFuture = reentrantLock.release(null);
                callbackFuture.get();
            }
            Lock lock = client.newLock("reentrantLock2".getBytes(StandardCharsets.UTF_8), 0, 60);
            try {
                lock.releaseHead();
                throw new SlockException("lock error");
            } catch (LockUnlockedException e) {}
            lock.acquire();
            lock.release();
        } finally {
            client.close();
        }
    }

    @Test
    public void testSemaphore() throws IOException, SlockException {
        SlockClient client = new SlockClient(clientHost, clinetPort);
        client.open();

        try {
            io.github.snower.jaslock.Semaphore semaphore = client.newSemaphore("semaphore1".getBytes(StandardCharsets.UTF_8), (short) 10, 0, 60);
            for (int i = 0; i < 10; i++) {
                semaphore.acquire();
            }
            try {
                semaphore.acquire();
                throw new SlockException("lock error");
            } catch (LockTimeoutException e) {}
            for (int i = 0; i < 10; i++) {
                semaphore.release();
            }
            semaphore.acquire();
            semaphore.release();
            Lock lock = client.newLock("semaphore1".getBytes(StandardCharsets.UTF_8), 0, 60);
            try {
                lock.releaseHead();
                throw new SlockException("lock error");
            } catch (LockUnlockedException e) {}
            lock.acquire();
            lock.release();
        } finally {
            client.close();
        }
    }

    @Test
    public void testSemaphoreAsync() throws IOException, SlockException, ExecutionException, InterruptedException {
        SlockClient client = new SlockClient(clientHost, clinetPort);
        client.enableAsyncCallback();
        client.open();

        try {
            io.github.snower.jaslock.Semaphore semaphore = client.newSemaphore("semaphore2".getBytes(StandardCharsets.UTF_8), (short) 10, 0, 60);
            for (int i = 0; i < 10; i++) {
                CallbackFuture<Boolean> callbackFuture = semaphore.acquire(null);
                callbackFuture.get();
            }
            try {
                CallbackFuture<Boolean> callbackFuture = semaphore.acquire(null);
                callbackFuture.get();
                throw new SlockException("lock error");
            } catch (ExecutionException e) {
                if (!(e.getCause() instanceof LockTimeoutException)) {
                    throw new SlockException("lock error");
                }
            }
            for (int i = 0; i < 10; i++) {
                CallbackFuture<Boolean> callbackFuture = semaphore.release(null);
                callbackFuture.get();
            }
            CallbackFuture<Boolean> callbackFuture = semaphore.acquire(null);
            callbackFuture.get();
            callbackFuture = semaphore.release(null);
            callbackFuture.get();
            Lock lock = client.newLock("semaphore2".getBytes(StandardCharsets.UTF_8), 0, 60);
            try {
                lock.releaseHead();
                throw new SlockException("lock error");
            } catch (LockUnlockedException e) {}
            lock.acquire();
            lock.release();
        } finally {
            client.close();
        }
    }

    private void testChildTreeLock(SlockClient client, TreeLock rootLock, TreeLock childLock, TreeLock.TreeLeafLock lock, int depth) throws IOException, SlockException {
        TreeLock.TreeLeafLock clock1 = childLock.newLeafLock();
        clock1.acquire();

        TreeLock.TreeLeafLock clock2 = childLock.newLeafLock();
        clock2.acquire();

        Lock testLock = client.newLock(rootLock.getLockKey(), 0, 0);
        try {
            testLock.acquire();
            throw new SlockException();
        } catch (LockTimeoutException e) {
        }
        testLock = client.newLock(childLock.getLockKey(), 0, 0);
        try {
            testLock.acquire();
            throw new SlockException();
        } catch (LockTimeoutException e) {
        }

        lock.release();
        testLock = client.newLock(rootLock.getLockKey(), 0, 0);
        try {
            testLock.acquire();
            throw new SlockException();
        } catch (LockTimeoutException e) {
        }
        testLock = client.newLock(childLock.getLockKey(), 0, 0);
        try {
            testLock.acquire();
            throw new SlockException();
        } catch (LockTimeoutException e) {
        }

        if (depth - 1  > 0) {
            lock.acquire();
            testChildTreeLock(client, childLock, childLock.newChild(), lock, depth - 1);
            lock.acquire();
            testChildTreeLock(client, childLock, childLock.newChild(), lock, depth - 1);
        }

        clock1.release();
        testLock = client.newLock(rootLock.getLockKey(), 0, 0);
        try {
            testLock.acquire();
            throw new SlockException();
        } catch (LockTimeoutException e) {
        }
        testLock = client.newLock(childLock.getLockKey(), 0, 0);
        try {
            testLock.acquire();
            throw new SlockException();
        } catch (LockTimeoutException e) {
        }

        clock2.release();
        testLock = client.newLock(childLock.getLockKey(), 0, 0);
        testLock.acquire();
    }

    @Test
    public void testTreeLock() throws IOException, SlockException {
        SlockClient client = new SlockClient(clientHost, clinetPort);
        client.open();

        TreeLock rootLock = client.newTreeLock("TestTreeLock", 5, 10);

        rootLock.acquire();
        rootLock.release();
        rootLock.wait(1);

        TreeLock.TreeLeafLock lock = rootLock.newLeafLock();
        lock.acquire();

        testChildTreeLock(client, rootLock, rootLock.newChild(),  lock,5);

        rootLock.wait(1);
        Lock testLock = client.newLock(rootLock.getLockKey(), 0, 0);
        testLock.acquire();
    }

    @Test
    public void testMaxConcurrentFlow() throws IOException, SlockException, InterruptedException {
        SlockClient client = new SlockClient(clientHost, clinetPort);
        client.open();

        try {
            MaxConcurrentFlow maxConcurrentFlow1 = client.newMaxConcurrentFlow("maxconcurrentflow1".getBytes(StandardCharsets.UTF_8), (short) 5, 0, 10);
            MaxConcurrentFlow maxConcurrentFlow2 = client.newMaxConcurrentFlow("maxconcurrentflow1".getBytes(StandardCharsets.UTF_8), (short) 5, 0, 10);
            maxConcurrentFlow1.acquire();
            maxConcurrentFlow2.acquire();
            maxConcurrentFlow1.release();
            maxConcurrentFlow2.release();

            Lock lock = client.newLock("maxconcurrentflow1".getBytes(StandardCharsets.UTF_8), 0, 10);
            lock.acquire();
            lock.release();

            MaxConcurrentFlow[] maxConcurrentFlows = new MaxConcurrentFlow[5];
            for (int i = 0; i < 5; i++) {
                maxConcurrentFlows[i] = client.newMaxConcurrentFlow("maxconcurrentflow1".getBytes(StandardCharsets.UTF_8), (short) 5, 0, 10);
                maxConcurrentFlows[i].acquire();
            }
            try {
                maxConcurrentFlow1.acquire();
                throw new SlockException("acquire error");
            } catch (LockTimeoutException e) {}
            for (int i = 0; i < 5; i++) {
                maxConcurrentFlows[i].release();
            }
            maxConcurrentFlow1.acquire();
            maxConcurrentFlow1.release();

            for (int i = 0; i < 5; i++) {
                maxConcurrentFlows[i] = client.newMaxConcurrentFlow("maxconcurrentflow1".getBytes(StandardCharsets.UTF_8), (short) 5, 0, 100);
                maxConcurrentFlows[i].setExpriedFlag((short) ICommand.EXPRIED_FLAG_MILLISECOND_TIME);
                maxConcurrentFlows[i].acquire();
            }
            Thread.sleep(200);
            maxConcurrentFlow1.acquire();
            maxConcurrentFlow1.release();
        } finally {
            client.close();
        }
    }

    @Test
    public void testMaxConcurrentFlowAsync() throws IOException, SlockException, ExecutionException, InterruptedException {
        SlockClient client = new SlockClient(clientHost, clinetPort);
        client.enableAsyncCallback();
        client.open();
        try {
            MaxConcurrentFlow maxConcurrentFlow1 = client.newMaxConcurrentFlow("maxconcurrentflow1".getBytes(StandardCharsets.UTF_8), (short) 5, 0, 60);
            MaxConcurrentFlow maxConcurrentFlow2 = client.newMaxConcurrentFlow("maxconcurrentflow1".getBytes(StandardCharsets.UTF_8), (short) 5, 0, 60);
            CallbackFuture<Boolean> callbackFuture = maxConcurrentFlow1.acquire(null);
            callbackFuture.get();
            callbackFuture = maxConcurrentFlow2.acquire(null);
            callbackFuture.get();
            callbackFuture = maxConcurrentFlow1.release(null);
            callbackFuture.get();
            callbackFuture = maxConcurrentFlow2.release(null);
            callbackFuture.get();
        } finally {
            client.close();
        }
    }

    @Test
    public void testTokenBucketFlow() throws IOException, SlockException, InterruptedException {
        SlockClient client = new SlockClient(clientHost, clinetPort);
        client.open();

        try {
            TokenBucketFlow tokenBucketFlow1 = client.newTokenBucketFlow("tokenbucketflow1".getBytes(StandardCharsets.UTF_8), (short) 5, 0, 0.1);
            TokenBucketFlow tokenBucketFlow2 = client.newTokenBucketFlow("tokenbucketflow1".getBytes(StandardCharsets.UTF_8), (short) 5, 0, 0.1);
            tokenBucketFlow1.acquire();
            tokenBucketFlow2.acquire();

            Thread.sleep(200);
            Lock lock = client.newLock("tokenbucketflow1".getBytes(StandardCharsets.UTF_8), 0, 10);
            lock.acquire();
            lock.release();

            TokenBucketFlow tokenBucketFlow = client.newTokenBucketFlow("tokenbucketflow1".getBytes(StandardCharsets.UTF_8), (short) 50, 5, 0.05);
            int count = 0;
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 2000; i++) {
                tokenBucketFlow.acquire();
                count++;
            }
            long endTime = System.currentTimeMillis();
            Thread.sleep(100);
            lock = client.newLock("tokenbucketflow1".getBytes(StandardCharsets.UTF_8), 0, 10);
            lock.acquire();
            lock.release();

            int rate = (int) (count * 1000 / (endTime - startTime));
            System.out.println(rate);
        } finally {
            client.close();
        }
    }

    @Test
    public void testTokenBucketFlowAsync() throws IOException, SlockException, ExecutionException, InterruptedException {
        SlockClient client = new SlockClient(clientHost, clinetPort);
        client.enableAsyncCallback();
        client.open();
        try {
            TokenBucketFlow tokenBucketFlow1 = client.newTokenBucketFlow("tokenbucketflow2".getBytes(StandardCharsets.UTF_8), (short) 5, 0, 0.1);
            TokenBucketFlow tokenBucketFlow2 = client.newTokenBucketFlow("tokenbucketflow2".getBytes(StandardCharsets.UTF_8), (short) 5, 0, 0.1);
            CallbackFuture<Boolean> callbackFuture = tokenBucketFlow1.acquire(null);
            callbackFuture.get();
            callbackFuture = tokenBucketFlow2.acquire(null);
            callbackFuture.get();
        } finally {
            client.close();
        }
    }

    @Test
    public void testLockData() throws IOException, SlockException, ExecutionException, InterruptedException {
        SlockClient client = new SlockClient(clientHost, clinetPort);
        client.open();

        try {
            Lock lock1 = client.newLock("lockdata1".getBytes(StandardCharsets.UTF_8), 0, 10);
            lock1.setCount((short) 10);
            Lock lock2 = client.newLock("lockdata1".getBytes(StandardCharsets.UTF_8), 0, 10);
            lock2.setCount((short) 10);
            lock1.acquire(new LockSetData("aaa"));
            Assert.assertNull(lock1.getCurrentLockDataAsString());
            lock2.acquire(new LockSetData("bbb"));
            Assert.assertEquals(lock2.getCurrentLockDataAsString(), "aaa");
            lock1.release(new LockSetData("ccc"));
            Assert.assertEquals(lock1.getCurrentLockDataAsString(), "bbb");
            lock2.release();
            Assert.assertEquals(lock2.getCurrentLockDataAsString(), "ccc");

            lock1 = client.newLock("lockdata2".getBytes(StandardCharsets.UTF_8), 0, 10);
            lock1.setCount((short) 10);
            lock2 = client.newLock("lockdata2".getBytes(StandardCharsets.UTF_8), 0, 10);
            lock2.setCount((short) 10);
            lock1.acquire(new LockIncrData(1));
            Assert.assertEquals((long) lock1.getCurrentLockDataAsLong(), 0);
            lock2.acquire(new LockIncrData(-3));
            Assert.assertEquals((long) lock2.getCurrentLockDataAsLong(), 1);
            lock1.release(new LockIncrData(4));
            Assert.assertEquals((long) lock1.getCurrentLockDataAsLong(), -2);
            lock2.release();
            Assert.assertEquals((long) lock2.getCurrentLockDataAsLong(), 2);

            lock1 = client.newLock("lockdata3".getBytes(StandardCharsets.UTF_8), 0, 10);
            lock1.setCount((short) 10);
            lock2 = client.newLock("lockdata3".getBytes(StandardCharsets.UTF_8), 0, 10);
            lock2.setCount((short) 10);
            lock1.acquire(new LockAppendData("aaa"));
            Assert.assertNull(lock1.getCurrentLockDataAsString());
            lock2.acquire(new LockAppendData("bbb"));
            Assert.assertEquals(lock2.getCurrentLockDataAsString(), "aaa");
            lock1.release(new LockAppendData("ccc"));
            Assert.assertEquals(lock1.getCurrentLockDataAsString(), "aaabbb");
            lock2.release();
            Assert.assertEquals(lock2.getCurrentLockDataAsString(), "aaabbbccc");

            lock1 = client.newLock("lockdata4".getBytes(StandardCharsets.UTF_8), 0, 10);
            lock1.setCount((short) 10);
            lock2 = client.newLock("lockdata4".getBytes(StandardCharsets.UTF_8), 0, 10);
            lock2.setCount((short) 10);
            lock1.acquire(new LockSetData("aaabbbccc"));
            Assert.assertNull(lock1.getCurrentLockDataAsString());
            lock2.acquire(new LockShiftData(4));
            Assert.assertEquals(lock2.getCurrentLockDataAsString(), "aaabbbccc");
            lock1.release(new LockShiftData(2));
            Assert.assertEquals(lock1.getCurrentLockDataAsString(), "bbccc");
            lock2.release();
            Assert.assertEquals(lock2.getCurrentLockDataAsString(), "ccc");

            byte[] lockKey = LockCommand.genLockId();
            lock1 = client.newLock("lockdata5".getBytes(StandardCharsets.UTF_8), 0, 10);
            lock1.acquire(new LockExecuteData(new LockCommand(ICommand.COMMAND_TYPE_LOCK, (byte) 0, lockKey, LockCommand.genLockId(), 0, 10, new LockSetData("aaa"))));
            Thread.sleep(100);
            lock2 = client.newLock(lockKey, 0, 10);
            try {
                lock2.acquire();
                lock2.release();
                throw new SlockException("lock error");
            } catch (LockTimeoutException e) {}
            lock1.release();

            lockKey = LockCommand.genLockId();
            lock1 = client.newLock("lockdata6".getBytes(StandardCharsets.UTF_8), 0, 10);
            lock1.acquire(new LockPipelineData(new LockData[] {
                    new LockSetData("aaa"),
                    new LockExecuteData(new LockCommand(ICommand.COMMAND_TYPE_LOCK, (byte) 0, lockKey, LockCommand.genLockId(), 0, 10, new LockSetData("aaa"))),
            }));
            Thread.sleep(100);
            lock2 = client.newLock(lockKey, 0, 10);
            try {
                lock2.acquire();
                lock2.release();
                throw new SlockException("lock error");
            } catch (LockTimeoutException e) {}
            lock1.release();
            Assert.assertEquals(lock1.getCurrentLockDataAsString(), "aaa");
        } finally {
            client.close();
        }
    }

    @Test
    public void testBenchmark() throws IOException, InterruptedException {
        SlockClient client = new SlockClient(clientHost, clinetPort);
        client.open();

        int totalCount = 400000;
        try {
            AtomicInteger count = new AtomicInteger(0);
            List<Thread> threads = new ArrayList<>();
            long startMs = System.currentTimeMillis();
            for (int i = 0; i < 256; i++) {
                Thread thread = new Thread(() -> {
                    while (count.get() < totalCount) {
                        Lock lock = client.newLock("benchmark" + count.get(), 5, 10);
                        try {
                            lock.acquire();
                            count.incrementAndGet();
                            lock.release();
                            count.incrementAndGet();
                        } catch (SlockException e) {
                            e.printStackTrace();
                        }
                    }
                });
                thread.setDaemon(true);
                thread.start();
                threads.add(thread);
            }
            for (Thread thread : threads) {
                thread.join();
            }
            long endMs = System.currentTimeMillis();
            System.out.println("Benchmark " + totalCount + " Count Lock and Unlock: " + (((double) totalCount) / ((endMs - startMs) / 1000d)) + "r/s " + (endMs - startMs) + "ms");
        } finally {
            client.close();
        }
    }
}
