package io.github.snower.jaslock.commands;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class InitCommandResult extends CommandResult {
    protected byte initType;

    public InitCommandResult() {
        super();
    }

    public byte getInitType() {
        return initType;
    }

    @Override
    public ICommand parseCommand(byte[] buf) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buf);
        magic = (byte) byteArrayInputStream.read();
        version = (byte) byteArrayInputStream.read();
        commandType = (byte) byteArrayInputStream.read();
        requestId = new byte[16];
        byteArrayInputStream.read(requestId, 0, 16);
        result = (byte) byteArrayInputStream.read();
        initType = (byte) byteArrayInputStream.read();
        return this;
    }

    @Override
    public byte[] dumpCommand() {
        ByteArrayOutputStream byteArrayOutputStream = new CapacityByteArrayOutputStream(64);
        byteArrayOutputStream.write(MAGIC);
        byteArrayOutputStream.write(VERSION);
        byteArrayOutputStream.write(commandType);
        byteArrayOutputStream.write(requestId, 0, 16);
        byteArrayOutputStream.write(initType);
        byteArrayOutputStream.write(new byte[43], 0, 43);
        return byteArrayOutputStream.toByteArray();
    }
}
