package io.github.snower.jaslock.deferred;

import io.github.snower.jaslock.commands.Command;
import io.github.snower.jaslock.exceptions.ClientClosedException;
import io.github.snower.jaslock.exceptions.ClientCommandTimeoutException;

import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class DeferredManager {
    private final DeferredOption deferredOption;
    private ExecutorService deferredExecutor;
    private ScheduledExecutorService timeoutExecutor;
    private ConcurrentHashMap<Long, LinkedList<DeferredCommand>> timeoutQueues;
    private long currentTimeoutAt;

    public DeferredManager(DeferredOption deferredOption) {
        this.deferredOption = deferredOption;
        this.currentTimeoutAt = (new Date()).getTime() / 1000;
    }

    public void start() {
        if (deferredExecutor != null || timeoutExecutor != null) return;

        deferredExecutor = Executors.newFixedThreadPool(deferredOption.getWorkerCount());
        timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
        timeoutQueues = new ConcurrentHashMap<>();

        timeoutExecutor.scheduleAtFixedRate(() -> {
            long now = (new Date()).getTime() / 1000;
            while (currentTimeoutAt <  now) {
                LinkedList<DeferredCommand> timeoutAtQueues = timeoutQueues.remove(currentTimeoutAt);
                if (timeoutAtQueues != null) {
                    while (!timeoutAtQueues.isEmpty()) {
                        DeferredCommand deferredCommand = timeoutAtQueues.pop();
                        if (deferredCommand.isFinished()) {
                            continue;
                        }
                        deferredCommand.setFinished(true);
                        deferredExecutor.submit(() -> {
                            deferredCommand.getCallback().accept(new DeferredCommandResult(deferredCommand.getCommand(),
                                    null, new ClientCommandTimeoutException()));
                        });
                    }
                }
                currentTimeoutAt++;
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void addCommand(Command command, Consumer<DeferredCommandResult> callback) {
        DeferredCommand deferredCommand = new DeferredCommand(command, callback);
        int timeout = command.setWaiterCallback(commandResult -> {
            if (deferredCommand.isFinished()) return;

            deferredExecutor.submit(() -> {
                if (commandResult == null) {
                    callback.accept(new DeferredCommandResult(command, null, new ClientClosedException()));
                } else {
                    callback.accept(new DeferredCommandResult(command, commandResult, null));
                }
            });
        });
        deferredCommand.setTimeoutAt(Math.max((new Date()).getTime() / 1000 + timeout, currentTimeoutAt + 1));
        LinkedList<DeferredCommand> timeoutAtQueues = timeoutQueues.get(deferredCommand.getTimeoutAt());
        if (timeoutAtQueues == null) {
            synchronized (this) {
                timeoutAtQueues = timeoutQueues.computeIfAbsent(deferredCommand.getTimeoutAt(), k -> new LinkedList<>());
            }
        }
        timeoutAtQueues.add(deferredCommand);
    }
}
