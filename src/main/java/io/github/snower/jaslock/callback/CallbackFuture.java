package io.github.snower.jaslock.callback;

import io.github.snower.jaslock.exceptions.SlockException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class CallbackFuture<V> implements Future<V> {
    private final Consumer<CallbackFuture<V>> callback;
    private volatile List<Runnable> doneRunnables;
    private volatile Semaphore waiter;
    private V result;
    private SlockException exception;
    private boolean isSetedResult;

    public CallbackFuture(Consumer<CallbackFuture<V>> callback) {
        this.callback = callback;
        this.isSetedResult = false;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return isSetedResult;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        if (isSetedResult) {
            if (exception != null) {
                throw new ExecutionException(exception);
            }
            return result;
        }

        createWaiter();
        waiter.acquire(1);
        if (exception != null) {
            throw new ExecutionException(exception);
        }
        return result;
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (isSetedResult) {
            if (exception != null) {
                throw new ExecutionException(exception);
            }
            return result;
        }

        createWaiter();
        if (!waiter.tryAcquire(1, timeout, unit)) {
            throw new TimeoutException();
        }
        if (exception != null) {
            throw new ExecutionException(exception);
        }
        return result;
    }

    public SlockException getException() {
        return exception;
    }

    public V getResult() throws SlockException {
        if (exception != null) {
            throw exception;
        }
        return result;
    }

    public boolean setResult(V result, SlockException exception) {
        if (this.isSetedResult) return false;

        this.result = result;
        this.exception = exception;
        this.isSetedResult = true;

        if (waiter != null) {
            synchronized (this) {
                if (waiter != null) {
                    waiter.release();
                }
            }
        }

        try {
            callback.accept(this);
        } finally {
            if (doneRunnables != null) {
                for (Runnable runnable : doneRunnables) {
                    try {
                        runnable.run();
                    } catch (Exception ignored) {}
                }
            }
        }
        return true;
    }

    public boolean setResult(V result) {
        return setResult(result, null);
    }

    public void addDoneRunnable(Runnable runnable) {
        if (doneRunnables == null) {
            synchronized (this) {
                if (doneRunnables == null) {
                    doneRunnables = new ArrayList<>();
                }
            }
        }
        doneRunnables.add(runnable);
    }

    private void createWaiter() throws InterruptedException {
        synchronized (this) {
            if (waiter != null) {
                throw new InterruptedException();
            }
            waiter = new Semaphore(1);
            waiter.acquire();
        }
    }
}
