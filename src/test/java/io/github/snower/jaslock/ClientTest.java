package io.github.snower.jaslock;

import static org.junit.Assert.assertTrue;

import io.github.snower.jaslock.callback.DeferredCommandResult;
import io.github.snower.jaslock.callback.DeferredResult;
import io.github.snower.jaslock.exceptions.SlockException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

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
                AtomicReference<DeferredCommandResult> deferredCommandResult = new AtomicReference<>();
                waiter.acquire();
                lock.acquire(dcr -> {
                    deferredCommandResult.set(dcr);
                    waiter.release();
                });
                waiter.acquire();
                deferredCommandResult.get().getResult();
                lock.release(dcr -> {
                    deferredCommandResult.set(dcr);
                    waiter.release();
                });
                waiter.acquire();
                deferredCommandResult.get().getResult();
            } catch (InterruptedException e) {}
        } finally {
            client.close();
        }
    }

    @Test
    public void testReplsetClientAsyncLock() throws SlockException {
        SlockReplsetClient client = new SlockReplsetClient(new String[]{clientHost + ":" + clinetPort});
        client.enableAsyncCallback();
        client.open();
        try {
            Lock lock = client.newLock("test_async2".getBytes(StandardCharsets.UTF_8), 5, 5);
            try {
                Semaphore waiter = new Semaphore(1);
                AtomicReference<DeferredCommandResult> deferredCommandResult = new AtomicReference<>();
                waiter.acquire();
                lock.acquire(dcr -> {
                    deferredCommandResult.set(dcr);
                    waiter.release();
                });
                waiter.acquire();
                deferredCommandResult.get().getResult();
                lock.release(dcr -> {
                    deferredCommandResult.set(dcr);
                    waiter.release();
                });
                waiter.acquire();
                deferredCommandResult.get().getResult();
            } catch (InterruptedException e) {}
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
                AtomicReference<DeferredResult<Object>> deferredResult = new AtomicReference<>();
                waiter.acquire();
                event.wait(2, dr -> {
                    deferredResult.set(dr);
                    waiter.release();
                });
                waiter.acquire();
                deferredResult.get().getResult();
            } catch (InterruptedException e) {}
        } finally {
            client.close();
        }
    }

    @Test
    public void testEventAsyncDefaultUnseted() throws IOException, SlockException {
        SlockClient client = new SlockClient(clientHost, clinetPort);
        client.enableAsyncCallback();
        client.open();

        try {
            Event event = client.newEvent("event_async2".getBytes(StandardCharsets.UTF_8), 5, 60, false);
            Assert.assertFalse(event.isSet());
            event.set();
            Assert.assertTrue(event.isSet());
            event.clear();
            Assert.assertFalse(event.isSet());
            event.set();
            Assert.assertTrue(event.isSet());
            try {
                Semaphore waiter = new Semaphore(1);
                AtomicReference<DeferredResult<Object>> deferredResult = new AtomicReference<>();
                waiter.acquire();
                event.wait(2, dr -> {
                    deferredResult.set(dr);
                    waiter.release();
                });
                waiter.acquire();
                deferredResult.get().getResult();
            } catch (InterruptedException e) {}
        } finally {
            client.close();
        }
    }

    @Test
    public void testTreeLock() throws IOException, SlockException {
        SlockClient client = new SlockClient(clientHost, clinetPort);
        client.open();

        TreeLock rootLock = client.newTreeLock("TestTreeLock", 5, 10);
        TreeLock.TreeLockLock lock = rootLock.newLock();
        lock.acquire();

        TreeLock childLock = rootLock.newChild();
        TreeLock.TreeLockLock clock1 = childLock.newLock();
        clock1.acquire();

        TreeLock.TreeLockLock clock2 = childLock.newLock();
        clock2.acquire();

        Lock testLock = client.newLock(rootLock.getLockKey(), 0, 0);
        try {
            testLock.acquire();
            throw new SlockException();
        } catch (SlockException e) {
        }
        testLock = client.newLock(childLock.getLockKey(), 0, 0);
        try {
            testLock.acquire();
            throw new SlockException();
        } catch (SlockException e) {
        }

        lock.release();
        testLock = client.newLock(rootLock.getLockKey(), 0, 0);
        try {
            testLock.acquire();
            throw new SlockException();
        } catch (SlockException e) {
        }
        testLock = client.newLock(childLock.getLockKey(), 0, 0);
        try {
            testLock.acquire();
            throw new SlockException();
        } catch (SlockException e) {
        }

        clock1.release();
        testLock = client.newLock(rootLock.getLockKey(), 0, 0);
        try {
            testLock.acquire();
            throw new SlockException();
        } catch (SlockException e) {
        }
        testLock = client.newLock(childLock.getLockKey(), 0, 0);
        try {
            testLock.acquire();
            throw new SlockException();
        } catch (SlockException e) {
        }

        clock2.release();
        testLock = client.newLock(rootLock.getLockKey(), 0, 0);
        testLock.acquire();
        testLock = client.newLock(childLock.getLockKey(), 0, 0);
        testLock.acquire();
    }
}
