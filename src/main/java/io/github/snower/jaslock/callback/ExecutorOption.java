package io.github.snower.jaslock.callback;

public class ExecutorOption {
    public final static ExecutorOption DefaultOption = new ExecutorOption(1, 4, 120);

    private final int workerCount;
    private final int maxWorkerCount;
    private final int workerKeepAliveTime;

    public ExecutorOption(int workerCount, int maxWorkerCount, int workerKeepAliveTime) {
        this.workerCount = workerCount;
        this.maxWorkerCount = maxWorkerCount;
        this.workerKeepAliveTime = workerKeepAliveTime;
    }

    public int getWorkerCount() {
        return workerCount;
    }

    public int getMaxWorkerCount() {
        return maxWorkerCount;
    }

    public int getWorkerKeepAliveTime() {
        return workerKeepAliveTime;
    }
}
