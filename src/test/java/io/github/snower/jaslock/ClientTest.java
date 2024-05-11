package io.github.snower.jaslock;

import static org.junit.Assert.assertTrue;

import io.github.snower.jaslock.callback.CallbackCommandResult;
import io.github.snower.jaslock.callback.CallbackFuture;
import io.github.snower.jaslock.datas.LockSetData;
import io.github.snower.jaslock.exceptions.LockTimeoutException;
import io.github.snower.jaslock.exceptions.SlockException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Unit test for simple App.
 */
public class ClientTest
{
    static String clientHost = "localhost";
    static int clinetPort = 5658;

    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }

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
    public void testMaxConcurrentFlow() throws IOException, SlockException {
        SlockClient client = new SlockClient(clientHost, clinetPort);
        client.open();

        try {
            MaxConcurrentFlow maxConcurrentFlow1 = client.newMaxConcurrentFlow("maxconcurrentflow1".getBytes(StandardCharsets.UTF_8), (short) 5, 60, 60);
            MaxConcurrentFlow maxConcurrentFlow2 = client.newMaxConcurrentFlow("maxconcurrentflow1".getBytes(StandardCharsets.UTF_8), (short) 5, 60, 60);
            maxConcurrentFlow1.acquire();
            maxConcurrentFlow2.acquire();
            maxConcurrentFlow1.release();
            maxConcurrentFlow2.release();
        } finally {
            client.close();
        }
    }

    @Test
    public void testMaxConcurrentFlowAsync() throws IOException, SlockException {
        SlockClient client = new SlockClient(clientHost, clinetPort);
        client.enableAsyncCallback();
        client.open();
        try {
            MaxConcurrentFlow maxConcurrentFlow1 = client.newMaxConcurrentFlow("maxconcurrentflow1".getBytes(StandardCharsets.UTF_8), (short) 5, 60, 60);
            MaxConcurrentFlow maxConcurrentFlow2 = client.newMaxConcurrentFlow("maxconcurrentflow1".getBytes(StandardCharsets.UTF_8), (short) 5, 60, 60);
            CallbackFuture<Boolean> callbackFuture = maxConcurrentFlow1.acquire(null);
            callbackFuture.get();
            callbackFuture = maxConcurrentFlow2.acquire(null);
            callbackFuture.get();
            callbackFuture = maxConcurrentFlow1.release(null);
            callbackFuture.get();
            callbackFuture = maxConcurrentFlow2.release(null);
            callbackFuture.get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            client.close();
        }
    }

    @Test
    public void testTokenBucketFlow() throws IOException, SlockException {
        SlockClient client = new SlockClient(clientHost, clinetPort);
        client.open();

        try {
            TokenBucketFlow tokenBucketFlow1 = client.newTokenBucketFlow("tokenbucketflow1".getBytes(StandardCharsets.UTF_8), (short) 5, 60, 0.1);
            TokenBucketFlow tokenBucketFlow2 = client.newTokenBucketFlow("tokenbucketflow1".getBytes(StandardCharsets.UTF_8), (short) 5, 60, 0.1);
            tokenBucketFlow1.acquire();
            tokenBucketFlow2.acquire();
        } finally {
            client.close();
        }
    }

    @Test
    public void testTokenBucketFlowAsync() throws IOException, SlockException {
        SlockClient client = new SlockClient(clientHost, clinetPort);
        client.enableAsyncCallback();
        client.open();
        try {
            TokenBucketFlow tokenBucketFlow1 = client.newTokenBucketFlow("tokenbucketflow2".getBytes(StandardCharsets.UTF_8), (short) 5, 60, 0.1);
            TokenBucketFlow tokenBucketFlow2 = client.newTokenBucketFlow("tokenbucketflow2".getBytes(StandardCharsets.UTF_8), (short) 5, 60, 0.1);
            CallbackFuture<Boolean> callbackFuture = tokenBucketFlow1.acquire(null);
            callbackFuture.get();
            callbackFuture = tokenBucketFlow2.acquire(null);
            callbackFuture.get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            client.close();
        }
    }
}
