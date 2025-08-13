package io.github.snower.jaslock.commands;

import java.io.ByteArrayOutputStream;

public class CapacityByteArrayOutputStream extends ByteArrayOutputStream {
    private final int capacitySize;

    public CapacityByteArrayOutputStream(int size) {
        super(size);

        this.capacitySize = size;
    }

    @Override
    public byte[] toByteArray() {
        if (buf.length == capacitySize && count == capacitySize) {
            return buf;
        }
        return super.toByteArray();
    }
}
