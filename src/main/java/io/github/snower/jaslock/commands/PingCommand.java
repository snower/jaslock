package io.github.snower.jaslock.commands;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class PingCommand extends Command {
    public PingCommand() {
        super(COMMAND_TYPE_PING);
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
        ByteArrayOutputStream byteArrayOutputStream = new CapacityByteArrayOutputStream(64);
        byteArrayOutputStream.write(MAGIC);
        byteArrayOutputStream.write(VERSION);
        byteArrayOutputStream.write(commandType);
        byteArrayOutputStream.write(requestId, 0, 16);
        byteArrayOutputStream.write(new byte[45], 0, 45);
        return byteArrayOutputStream.toByteArray();
    }
}

