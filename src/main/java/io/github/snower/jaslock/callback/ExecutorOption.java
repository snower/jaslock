package io.github.snower.jaslock.callback;

import java.util.concurrent.TimeUnit;

public class ExecutorOption {
    public final static ExecutorOption DefaultOption = new ExecutorOption(2, 4, 65536, 120, TimeUnit.SECONDS);

    private final int workerCount;
    private final int maxWorkerCount;
    private final int maxCapacity;
    private final int workerKeepAliveTime;
    private final TimeUnit workerKeepAliveTimeUnit;

    public ExecutorOption(int workerCount, int maxWorkerCount, int maxCapacity, int workerKeepAliveTime, TimeUnit workerKeepAliveTimeUnit) {
        this.workerCount = workerCount;
        this.maxWorkerCount = maxWorkerCount;
        this.maxCapacity = maxCapacity;
        this.workerKeepAliveTime = workerKeepAliveTime;
        this.workerKeepAliveTimeUnit = workerKeepAliveTimeUnit;
    }

    public int getWorkerCount() {
        return workerCount;
    }

    public int getMaxWorkerCount() {
        return maxWorkerCount;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public int getWorkerKeepAliveTime() {
        return workerKeepAliveTime;
    }

    public TimeUnit getWorkerKeepAliveTimeUnit() {
        return workerKeepAliveTimeUnit;
    }
}
