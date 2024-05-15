package io.github.snower.jaslock.exceptions;

public class SlockException extends Exception {
    public SlockException() {
        super("slock exception");
    }

    public SlockException(String message) {
        super(message);
    }
}
