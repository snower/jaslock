package io.github.snower.jaslock.deferred;

public class DeferredOption {
    private final int workerCount;

    public DeferredOption(int workerCount) {
        this.workerCount = workerCount;
    }

    public int getWorkerCount() {
        return workerCount;
    }
}
