package io.github.snower.jaslock.callback;

import io.github.snower.jaslock.commands.Command;
import io.github.snower.jaslock.exceptions.ClientClosedException;
import io.github.snower.jaslock.exceptions.ClientCommandTimeoutException;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class CallbackExecutorManager {
    private final ExecutorOption executorOption;
    private ExecutorService callbackExecutor;
    private ScheduledExecutorService timeoutExecutor;
    private ConcurrentHashMap<Long, List<DeferredCommand>> timeoutQueues;
    private long currentTimeoutAt;

    public CallbackExecutorManager(ExecutorOption executorOption) {
        this.executorOption = executorOption;
        this.currentTimeoutAt = (new Date()).getTime() / 1000;
    }

    public void start() {
        if (callbackExecutor != null || timeoutExecutor != null) return;

        callbackExecutor = new ThreadPoolExecutor(executorOption.getWorkerCount(), executorOption.getMaxWorkerCount(), executorOption.getWorkerKeepAliveTime(),
                TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
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
    }

    public void stop() {
        if (timeoutExecutor != null) {
            timeoutExecutor.shutdown();
            try {
                timeoutExecutor.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}
            timeoutExecutor = null;
        }
        if (callbackExecutor != null) {
            callbackExecutor.shutdown();
            try {
                callbackExecutor.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}
            callbackExecutor = null;
        }
        timeoutQueues = new ConcurrentHashMap<>();
    }

    public DeferredCommand addCommand(Command command, Consumer<DeferredCommandResult> callback, Consumer<DeferredCommandResult> timeoutCallback) {
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
