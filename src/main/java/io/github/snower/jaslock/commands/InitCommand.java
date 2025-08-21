package io.github.snower.jaslock.commands;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class InitCommand extends Command {
    private static final AtomicInteger clientIdIndex = new AtomicInteger(0);

    protected byte[] clientId;

    public InitCommand(byte[] clientId) {
        super(COMMAND_TYPE_INIT);

        this.clientId = clientId;
    }

    public byte[] getClientId() {
        return clientId;
    }

    @Override
    public ICommand parseCommand(byte[] buf) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buf);
        magic = (byte) byteArrayInputStream.read();
        version = (byte) byteArrayInputStream.read();
        commandType = (byte) byteArrayInputStream.read();
        requestId = new byte[16];
        byteArrayInputStream.read(requestId, 0, 16);
        clientId = new byte[16];
        byteArrayInputStream.read(clientId, 0, 16);
        return this;
    }

    @Override
    public byte[] dumpCommand() {
        ByteArrayOutputStream byteArrayOutputStream = new CapacityByteArrayOutputStream(64);
        byteArrayOutputStream.write(MAGIC);
        byteArrayOutputStream.write(VERSION);
        byteArrayOutputStream.write(commandType);
        byteArrayOutputStream.write(requestId, 0, 16);
        byteArrayOutputStream.write(clientId, 0, 16);
        byteArrayOutputStream.write(new byte[29], 0, 29);
        return byteArrayOutputStream.toByteArray();
    }

    public static byte[] genClientId() {
        ByteArrayOutputStream byteArrayOutputStream = new CapacityByteArrayOutputStream(16);
        long timestamp = System.currentTimeMillis();
        long randNumber = random.nextLong();
        long ri = ((long) clientIdIndex.addAndGet(1)) & 0x7fffffffL;
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
}
