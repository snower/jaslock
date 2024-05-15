package io.github.snower.jaslock.exceptions;

public class EventWaitTimeoutException extends SlockException {
    public EventWaitTimeoutException() {
        super("event wait timeout");
    }
}
