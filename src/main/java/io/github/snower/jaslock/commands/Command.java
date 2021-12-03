package io.github.snower.jaslock.commands;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Command implements ICommand {
    private static final AtomicInteger requestIdIndex = new AtomicInteger(0);

    protected byte magic;
    protected byte version;
    protected byte commandType;
    protected byte[] requestId;
    protected Semaphore waiter;
    public CommandResult commandResult;

    public Command(byte commandType) {
        this.commandType = commandType;
        this.requestId = genRequestId();
    }

    public byte getMagic() {
        return magic;
    }

    public byte getVersion() {
        return version;
    }

    @Override
    public int getCommandType() {
        return commandType;
    }

    @Override
    public byte[] getRequestId() {
        return requestId;
    }

    @Override
    public ICommand parseCommand(byte[] buf) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buf);
        try {
            magic = (byte) byteArrayInputStream.read();
            version = (byte) byteArrayInputStream.read();
            commandType = (byte) byteArrayInputStream.read();
            requestId = byteArrayInputStream.readNBytes(16);
        } catch (IOException e) {
            return null;
        }
        return this;
    }

    @Override
    public byte[] dumpCommand() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(MAGIC);
        byteArrayOutputStream.write(VERSION);
        byteArrayOutputStream.write(commandType);
        byteArrayOutputStream.write(requestId, 0, 16);
        byteArrayOutputStream.writeBytes(new byte[45]);
        return byteArrayOutputStream.toByteArray();
    }

    public static byte[] genRequestId() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        long timestamp = (new Date()).getTime();
        long randNumber = (new Random()).nextLong();
        long ri = ((long) requestIdIndex.addAndGet(1)) & 0x7fffffffL;
        byteArrayOutputStream.write((byte) (timestamp >> 40) & 0xff);
        byteArrayOutputStream.write((byte) (timestamp >> 32) & 0xff);
        byteArrayOutputStream.write((byte) (timestamp >> 24) & 0xff);
        byteArrayOutputStream.write((byte) (timestamp >> 16) & 0xff);
        byteArrayOutputStream.write((byte) (timestamp >> 8) & 0xff);
        byteArrayOutputStream.write((byte) timestamp & 0xff);
        byteArrayOutputStream.write((byte) (randNumber >> 40) & 0xff);
        byteArrayOutputStream.write((byte) (randNumber >> 32) & 0xff);
        byteArrayOutputStream.write((byte) (randNumber >> 24) & 0xff);
        byteArrayOutputStream.write((byte) (randNumber >> 16) & 0xff);
        byteArrayOutputStream.write((byte) (randNumber >> 8) & 0xff);
        byteArrayOutputStream.write((byte) randNumber & 0xff);
        byteArrayOutputStream.write((byte) (ri >> 24) & 0xff);
        byteArrayOutputStream.write((byte) (ri >> 16) & 0xff);
        byteArrayOutputStream.write((byte) (ri >> 8) & 0xff);
        byteArrayOutputStream.write((byte) ri & 0xff);
        return byteArrayOutputStream.toByteArray();
    }

    public boolean createWaiter() {
        waiter = new Semaphore(1);
        try {
            waiter.acquire();
        } catch (InterruptedException e) {
            waiter = null;
            return false;
        }
        return true;
    }

    public boolean wakeupWaiter() {
        if (waiter == null) {
            return false;
        }
        waiter.release();
        return true;
    }

    public boolean waiteWaiter() {
        if (waiter == null) {
            return false;
        }
        try {
            return waiter.tryAcquire(1, 5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }
}
