package io.github.snower.jaslock.commands;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class CommandResult implements ICommand {
    protected byte magic;
    protected byte version;
    protected byte commandType;
    protected byte[] requestId;
    protected byte result;

    public CommandResult() {

    }

    public CommandResult(byte result) {
        this.result = result;
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

    public byte getResult() {
        return result;
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
        return this;
    }

    public CommandResult loadCommandData(byte[] buf) {
        return this;
    }

    @Override
    public byte[] dumpCommand() {
        ByteArrayOutputStream byteArrayOutputStream = new CapacityByteArrayOutputStream(64);
        byteArrayOutputStream.write(MAGIC);
        byteArrayOutputStream.write(VERSION);
        byteArrayOutputStream.write(commandType);
        byteArrayOutputStream.write(requestId, 0, 16);
        byteArrayOutputStream.write(new byte[44], 0, 44);
        return byteArrayOutputStream.toByteArray();
    }

    @Override
    public boolean hasExtraData() {
        return false;
    }
}
