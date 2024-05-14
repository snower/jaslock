package io.github.snower.jaslock.callback;

import io.github.snower.jaslock.commands.Command;
import io.github.snower.jaslock.exceptions.*;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
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
                    executorOption.getWorkerKeepAliveTimeUnit(), executorOption.getMaxCapacity() <= 0 ? new SynchronousQueue<>() : new LinkedBlockingQueue<>(executorOption.getMaxCapacity()),
                    new CallbackExecutorThreadFactory("slock-callback-"), (r, executor) -> r.run());
            isExternCallbackExecutor = false;
        }
        if (timeoutScheduledExecutor == null) {
            timeoutScheduledExecutor = Executors.newSingleThreadScheduledExecutor(new CallbackExecutorThreadFactory("slock-schedule-" ));
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

    public CallbackCommand addCommand(Command command, Consumer<CallbackCommandResult> callback, Consumer<CallbackCommandResult> timeoutCallback) throws SlockException {
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
        if (timeout <= 0) {
            throw new ClientAsyncCallbackWaitedException();
        }

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

    private static class CallbackExecutorThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        public CallbackExecutorThreadFactory(String nameGroup) {
            SecurityManager s = System.getSecurityManager();
            this.group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            this.namePrefix = nameGroup + poolNumber.getAndIncrement() + "-thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
}
