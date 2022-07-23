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
    protected ScheduledExecutorService timeoutScheduledExecutor;
    protected ConcurrentHashMap<Long, List<CallbackCommand>> timeoutQueues;
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

    public CallbackExecutorManager(ScheduledExecutorService timeoutScheduledExecutor) {
        this.executorOption = ExecutorOption.DefaultOption;
        this.currentTimeoutAt = (new Date()).getTime() / 1000;
        this.isRuning = false;
        this.timeoutScheduledExecutor = timeoutScheduledExecutor;
        this.isExternTimeoutExecutor = true;
    }

    public CallbackExecutorManager(ExecutorService callbackExecutor, ScheduledExecutorService timeoutScheduledExecutor) {
        this.executorOption = ExecutorOption.DefaultOption;
        this.currentTimeoutAt = (new Date()).getTime() / 1000;
        this.isRuning = false;
        this.callbackExecutor = callbackExecutor;
        this.isExternCallbackExecutor = true;
        this.timeoutScheduledExecutor = timeoutScheduledExecutor;
        this.isExternTimeoutExecutor = true;
    }

    public void start() {
        if (isRuning) return;

        if (callbackExecutor == null) {
            callbackExecutor = new ThreadPoolExecutor(executorOption.getWorkerCount(), executorOption.getMaxWorkerCount(), executorOption.getWorkerKeepAliveTime(),
                    TimeUnit.SECONDS, new LinkedBlockingQueue<>());
            isExternCallbackExecutor = false;
        }
        if (timeoutScheduledExecutor == null) {
            timeoutScheduledExecutor = Executors.newSingleThreadScheduledExecutor();
            isExternTimeoutExecutor = false;
        }
        timeoutQueues = new ConcurrentHashMap<>();

        timeoutScheduledExecutor.scheduleAtFixedRate(() -> {
            long now = (new Date()).getTime() / 1000;
            while (currentTimeoutAt <  now) {
                List<CallbackCommand> timeoutAtQueues = timeoutQueues.remove(currentTimeoutAt);
                if (timeoutAtQueues != null) {
                    for (CallbackCommand callbackCommand : timeoutAtQueues) {
                        synchronized (callbackCommand) {
                            if (callbackCommand.isFinished()) return;
                            callbackCommand.close();
                        }

                        callbackExecutor.submit(() -> {
                            callbackCommand.getTimeoutCallback().accept(new CallbackCommandResult(callbackCommand.getCommand(),
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

        if (!isExternCallbackExecutor && timeoutScheduledExecutor != null) {
            timeoutScheduledExecutor.shutdown();
            try {
                timeoutScheduledExecutor.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}
        }
        timeoutScheduledExecutor = null;
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

    public CallbackCommand addCommand(Command command, Consumer<CallbackCommandResult> callback, Consumer<CallbackCommandResult> timeoutCallback) throws ClientAsyncCallbackStopedException {
        if (!isRuning) {
            throw new ClientAsyncCallbackStopedException();
        }

        CallbackCommand callbackCommand = new CallbackCommand(command, callback, timeoutCallback);
        int timeout = command.setWaiterCallback(commandResult -> {
            synchronized (callbackCommand) {
                if (callbackCommand.isFinished()) return;
                callbackCommand.close();
            }

            callbackExecutor.submit(() -> {
                if (commandResult == null) {
                    callback.accept(new CallbackCommandResult(command, null, new ClientClosedException()));
                } else {
                    callback.accept(new CallbackCommandResult(command, commandResult, null));
                }
            });
        });
        callbackCommand.setTimeoutAt(Math.max((new Date()).getTime() / 1000 + timeout, currentTimeoutAt + 1));
        List<CallbackCommand> timeoutAtQueues = timeoutQueues.get(callbackCommand.getTimeoutAt());
        if (timeoutAtQueues == null) {
            synchronized (this) {
                timeoutAtQueues = timeoutQueues.computeIfAbsent(callbackCommand.getTimeoutAt(), k -> Collections.synchronizedList(new LinkedList<>()));
            }
        }
        callbackCommand.addTimeoutQueues(timeoutAtQueues);
        return callbackCommand;
    }
}
