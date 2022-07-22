package io.github.snower.jaslock.callback;

import io.github.snower.jaslock.exceptions.SlockException;

public class DeferredResult<T> {
    private final T result;
    private final SlockException exception;

    public DeferredResult(SlockException exception) {
        this.result = null;
        this.exception = exception;
    }

    public DeferredResult(T result) {
        this.result = result;
        this.exception = null;
    }

    public Exception getException() {
        return exception;
    }

    public T getResult() throws SlockException {
        if (exception != null) {
            throw exception;
        }
        return result;
    }
}
