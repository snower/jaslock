package io.github.snower.jaslock;

import static org.junit.Assert.assertTrue;

import io.github.snower.jaslock.exceptions.SlockException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Unit test for simple App.
 */
public class ClientTest
{
    static String clientHost = "172.27.214.150";
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
        Client client = new Client(clientHost, clinetPort);
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
        ReplsetClient client = new ReplsetClient(new String[]{clientHost + ":" + clinetPort});
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
    public void testEventDefaultSeted() throws IOException, SlockException {
        Client client = new Client(clientHost, clinetPort);
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
        Client client = new Client(clientHost, clinetPort);
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
}
