package io.github.snower.jaslock.callback;

import io.github.snower.jaslock.commands.Command;
import io.github.snower.jaslock.exceptions.ClientAsyncCallbackStopedException;
import io.github.snower.jaslock.exceptions.ClientClosedException;
import io.github.snower.jaslock.exceptions.ClientCommandTimeoutException;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class CallbackExecutorManager {
    protected final ExecutorOption executorOption;
    protected ExecutorService callbackExecutor;
    protected ScheduledExecutorService timeoutExecutor;
    protected ConcurrentHashMap<Long, List<DeferredCommand>> timeoutQueues;
    protected long currentTimeoutAt;
    protected boolean isExternCallbackExecutor;
    protected boolean isExternTimeoutExecutor;
    protected boolean isRuning;

    public CallbackExecutorManager(ExecutorOption executorOption) {
        this.executorOption = executorOption;
        this.currentTimeoutAt = (new Date()).getTime() / 1000;
        this.isRuning = false;
    }

    public CallbackExecutorManager(ExecutorService callbackExecutor) {
        this.executorOption = ExecutorOption.DefaultOption;
        this.currentTimeoutAt = (new Date()).getTime() / 1000;
        this.isRuning = false;
        this.callbackExecutor = callbackExecutor;
        this.isExternCallbackExecutor = true;
    }

    public CallbackExecutorManager(ScheduledExecutorService timeoutExecutor) {
        this.executorOption = ExecutorOption.DefaultOption;
        this.currentTimeoutAt = (new Date()).getTime() / 1000;
        this.isRuning = false;
        this.timeoutExecutor = timeoutExecutor;
        this.isExternTimeoutExecutor = true;
    }

    public CallbackExecutorManager(ExecutorService callbackExecutor, ScheduledExecutorService timeoutExecutor) {
        this.executorOption = ExecutorOption.DefaultOption;
        this.currentTimeoutAt = (new Date()).getTime() / 1000;
        this.isRuning = false;
        this.callbackExecutor = callbackExecutor;
        this.isExternCallbackExecutor = true;
        this.timeoutExecutor = timeoutExecutor;
        this.isExternTimeoutExecutor = true;
    }

    public void start() {
        if (isRuning) return;

        if (callbackExecutor == null) {
            callbackExecutor = new ThreadPoolExecutor(executorOption.getWorkerCount(), executorOption.getMaxWorkerCount(), executorOption.getWorkerKeepAliveTime(),
                    TimeUnit.SECONDS, new LinkedBlockingQueue<>());
            isExternCallbackExecutor = false;
        }
        if (timeoutExecutor == null) {
            timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
            isExternTimeoutExecutor = false;
        }
        timeoutQueues = new ConcurrentHashMap<>();

        timeoutExecutor.scheduleAtFixedRate(() -> {
            long now = (new Date()).getTime() / 1000;
            while (currentTimeoutAt <  now) {
                List<DeferredCommand> timeoutAtQueues = timeoutQueues.remove(currentTimeoutAt);
                if (timeoutAtQueues != null) {
                    for (DeferredCommand deferredCommand : timeoutAtQueues) {
                        synchronized (deferredCommand) {
                            if (deferredCommand.isFinished()) return;
                            deferredCommand.close();
                        }

                        callbackExecutor.submit(() -> {
                            deferredCommand.getTimeoutCallback().accept(new DeferredCommandResult(deferredCommand.getCommand(),
                                    null, new ClientCommandTimeoutException()));
                        });
                    }
                }
                currentTimeoutAt++;
            }
        }, 0, 1, TimeUnit.SECONDS);
        isRuning = true;
    }

    public void stop() {
        if (!isRuning) return;

        if (!isExternCallbackExecutor && timeoutExecutor != null) {
            timeoutExecutor.shutdown();
            try {
                timeoutExecutor.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}
        }
        timeoutExecutor = null;
        if (!isExternTimeoutExecutor && callbackExecutor != null) {
            callbackExecutor.shutdown();
            try {
                callbackExecutor.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}
        }
        callbackExecutor = null;
        timeoutQueues = new ConcurrentHashMap<>();
        isRuning = false;
    }

    public DeferredCommand addCommand(Command command, Consumer<DeferredCommandResult> callback, Consumer<DeferredCommandResult> timeoutCallback) throws ClientAsyncCallbackStopedException {
        if (!isRuning) {
            throw new ClientAsyncCallbackStopedException();
        }

        DeferredCommand deferredCommand = new DeferredCommand(command, callback, timeoutCallback);
        int timeout = command.setWaiterCallback(commandResult -> {
            synchronized (deferredCommand) {
                if (deferredCommand.isFinished()) return;
                deferredCommand.close();
            }

            callbackExecutor.submit(() -> {
                if (commandResult == null) {
                    callback.accept(new DeferredCommandResult(command, null, new ClientClosedException()));
                } else {
                    callback.accept(new DeferredCommandResult(command, commandResult, null));
                }
            });
        });
        deferredCommand.setTimeoutAt(Math.max((new Date()).getTime() / 1000 + timeout, currentTimeoutAt + 1));
        List<DeferredCommand> timeoutAtQueues = timeoutQueues.get(deferredCommand.getTimeoutAt());
        if (timeoutAtQueues == null) {
            synchronized (this) {
                timeoutAtQueues = timeoutQueues.computeIfAbsent(deferredCommand.getTimeoutAt(), k -> Collections.synchronizedList(new LinkedList<>()));
            }
        }
        deferredCommand.addTimeoutQueues(timeoutAtQueues);
        return deferredCommand;
    }
}
