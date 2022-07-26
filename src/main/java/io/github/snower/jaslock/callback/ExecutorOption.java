package io.github.snower.jaslock.callback;

import java.util.concurrent.TimeUnit;

public class ExecutorOption {
    public final static ExecutorOption DefaultOption = new ExecutorOption(1, 4, 120, TimeUnit.SECONDS);

    private final int workerCount;
    private final int maxWorkerCount;
    private final int workerKeepAliveTime;
    private final TimeUnit workerKeepAliveTimeUnit;

    public ExecutorOption(int workerCount, int maxWorkerCount, int workerKeepAliveTime, TimeUnit workerKeepAliveTimeUnit) {
        this.workerCount = workerCount;
        this.maxWorkerCount = maxWorkerCount;
        this.workerKeepAliveTime = workerKeepAliveTime;
        this.workerKeepAliveTimeUnit = workerKeepAliveTimeUnit;
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

    public TimeUnit getWorkerKeepAliveTimeUnit() {
        return workerKeepAliveTimeUnit;
    }
}
