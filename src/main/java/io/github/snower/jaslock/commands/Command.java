package io.github.snower.jaslock.commands;

import io.github.snower.jaslock.exceptions.SlockException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class Command implements ICommand {
    private static final AtomicInteger requestIdIndex = new AtomicInteger(0);
    private static final Random random = new Random();

    protected byte magic;
    protected byte version;
    protected byte commandType;
    protected byte[] requestId;
    protected Semaphore waiter;
    protected Consumer<CommandResult> waiterCallback;
    protected int retryType = 0;
    public CommandResult commandResult;
    public Throwable exception;

    public Command(byte commandType) {
        this.commandType = commandType;
        this.requestId = genRequestId();
    }

    public int getRetryType() {
        return retryType;
    }

    public void setRetryType(int retryType) {
        this.retryType = retryType;
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
        magic = (byte) byteArrayInputStream.read();
        version = (byte) byteArrayInputStream.read();
        commandType = (byte) byteArrayInputStream.read();
        requestId = new byte[16];
        byteArrayInputStream.read(requestId, 0, 16);
        return this;
    }

    @Override
    public byte[] dumpCommand() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(MAGIC);
        byteArrayOutputStream.write(VERSION);
        byteArrayOutputStream.write(commandType);
        byteArrayOutputStream.write(requestId, 0, 16);
        byteArrayOutputStream.write(new byte[45], 0, 45);
        return byteArrayOutputStream.toByteArray();
    }

    @Override
    public boolean hasExtraData() {
        return false;
    }

    public byte[] getExtraData() throws SlockException {
        return null;
    }

    public static byte[] genRequestId() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        long timestamp = System.currentTimeMillis();
        long randNumber = random.nextLong();
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
        if (waiterCallback != null) return false;
        waiter = new Semaphore(1);
        try {
            waiter.acquire();
        } catch (InterruptedException e) {
            waiter = null;
            return false;
        }
        return true;
    }

    public int setWaiterCallback(Consumer<CommandResult> waiterCallback) {
        if (waiter != null) return -1;
        this.waiterCallback = waiterCallback;
        return 120;
    }

    public boolean wakeupWaiter() {
        if (waiter == null) {
            if (waiterCallback != null) {
                waiterCallback.accept(this.commandResult);
            }
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
            return waiter.tryAcquire(1, 120, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }
}
