/*
 * Copyright 2016 litesuits.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.litesuits.go;

import com.litesuits.go.utils.GoUtil;
import com.litesuits.go.utils.LogHelper;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A smart thread pool executor. About {@link SmartExecutor}:
 *
 * <ul>
 * <li>keep {@link #coreSize} tasks concurrent, and put them in {@link #runningList},
 * maximum number of running-tasks at the same time is {@link #coreSize}.</li>
 * <li>when {@link #runningList} is full, put new task in {@link #waitingList} waiting for execution,
 * maximum of waiting-tasks number is {@link #queueSize}.</li>
 * <li>when {@link #waitingList} is full, new task is performed by {@link OverloadPolicy}.</li>
 * <li>when running task is completed, take it out from {@link #runningList}.</li>
 * <li>schedule next by {@link SchedulePolicy}, take next task out from {@link #waitingList} to execute,
 * and so on until {@link #waitingList} is empty.</li>
 *
 * </ul>
 *
 * @author MaTianyu
 * @date 2015-04-23
 */
public class SmartExecutor implements Executor {

    // Logging Parameters
    private static final String TAG = SmartExecutor.class.getSimpleName();
    public static final String SMART_EXECUTOR_LOG_FORMAT =
            "SmartExecutor core-queue size: %{public}d - %{public}d  running-wait task: %{public}d - %{public}d";
    private static final int DOMAIN = 0x00201;
    private final LogHelper logHelper = new LogHelper(DOMAIN, TAG);

    private static final int DEFAULT_CACHE_SECOND = 5;
    /**
     * Static lock for synchronous static operations.
     */
    private static final Object staticLock = new Object();

    private static ThreadPoolExecutor threadPool;
    /**
     * Lock objects for synchronous operations.
     */
    private final Object lock = new Object();
    private final LinkedList<WrappedRunnable> runningList = new LinkedList<>();
    private final LinkedList<WrappedRunnable> waitingList = new LinkedList<>();
    /**
     * To log Debug Messages like thread start details, task finished messages, etc.
     */
    private boolean loggerEnabled = false;
    /**
     * Core size, or the maximum number of concurrent threads, as used by the SmartExecutor.
     * By default, this equals the number of CPU cores.
     */
    private int coreSize;
    private int queueSize;
    private SchedulePolicy schedulePolicy = SchedulePolicy.FIRST_IN_FIRST_RUN;
    private OverloadPolicy overloadPolicy = OverloadPolicy.DISCARD_OLD_TASK_IN_QUEUE;

    /**
     * Create a SmartExecutor Object with number of cores equal to the CPU core count,
     * length of waiting queue equal to 32 times number of cores.
     * <br/>
     * <br/>
     * The default {@link OverloadPolicy} is {@code DISCARD_OLD_TASK_IN_QUEUE}.
     * <br/>
     * The default {@link SchedulePolicy} is {@code FIRST_IN_FIRST_RUN}.
     */
    public SmartExecutor() {
        coreSize = GoUtil.getCoresCount();
        queueSize = coreSize * 32;
        initThreadPool();
    }

    /**
     * Create a SmartExecutor Object specifying number of cores and queue size.
     * <br/>
     * <br/>
     * The default {@link OverloadPolicy} is {@code DISCARD_OLD_TASK_IN_QUEUE}.
     * <br/>
     * The default {@link SchedulePolicy} is {@code FIRST_IN_FIRST_RUN}.
     *
     * @param coreSize  Number of concurrent tasks
     * @param queueSize Maximum number of tasks in the waiting queue
     */
    public SmartExecutor(int coreSize, int queueSize) {
        this.coreSize = coreSize;
        this.queueSize = queueSize;
        initThreadPool();
    }

    /**
     * Create a ThreadPoolExecutor used internally to handle threads.
     *
     * @return Reference to newly initialized ThreadPoolExecutor object
     */
    private static ThreadPoolExecutor createDefaultThreadPool() {
        // Keep up to 4 threads in the pool
        int coresCount = GoUtil.getCoresCount();
        int corePoolSize = Math.min(4, coresCount);
        return new ThreadPoolExecutor(
                corePoolSize,
                Integer.MAX_VALUE,
                DEFAULT_CACHE_SECOND, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new ThreadFactory() {
                    static final String NAME = "lite-";
                    AtomicInteger IDS = new AtomicInteger(1);

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, NAME + IDS.getAndIncrement());
                    }
                },
                new ThreadPoolExecutor.DiscardPolicy());
    }

    public static ThreadPoolExecutor getThreadPool() {
        return threadPool;
    }

    public static void setThreadPool(ThreadPoolExecutor threadPool) {
        SmartExecutor.threadPool = threadPool;
    }

    /**
     * Initializes the (static) thread pool if not already initialized by another instance of this object.
     */
    protected void initThreadPool() {
        synchronized (staticLock) {
            logHelper.logInfo(SMART_EXECUTOR_LOG_FORMAT,
                    coreSize, queueSize, runningList.size(), waitingList.size());

            if (threadPool == null) {
                threadPool = createDefaultThreadPool();
            }
        }
    }

    public boolean isLoggerEnabled() {
        return loggerEnabled;
    }

    /**
     * Set or disable the logging utility while using this SmartExecutor object.
     *
     * @param loggerEnabled Indicates whether or not the SmartExecutor will write logs.
     */
    public void setLoggerEnabled(boolean loggerEnabled) {
        this.loggerEnabled = loggerEnabled;
    }

    /**
     * Cancels all instances of the task from the waiting queue, if existing.
     *
     * @param command The task to be cancelled
     * @return If the task was cancelled or not
     */
    public boolean cancelWaitingTask(Runnable command) {
        boolean removed = false;
        synchronized (lock) {
            int size = waitingList.size();
            if (size > 0) {
                for (int i = size - 1; i >= 0; i--) {
                    if (waitingList.get(i).getRealRunnable() == command) {
                        synchronized (waitingList) {
                            waitingList.remove(i);
                        }
                        removed = true;
                    }
                }
            }
        }
        return removed;
    }

    /**
     * Create a runnable FutureTask.
     */
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new FutureTask<>(runnable, value);
    }

    /**
     * Create a callable FutureTask.
     */
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new FutureTask<>(callable);
    }

    /**
     * Submit a runnable.
     *
     * @param task The Runnable to be executed.
     */
    public Future<Void> submit(Runnable task) {
        RunnableFuture<Void> futureTask = newTaskFor(task, null);
        execute(futureTask);
        return futureTask;
    }

    /**
     * Creates a {@code FutureTask} that will, upon running, execute the
     * given {@code Runnable}, and following that, {@code get} will return the
     * given result on successful completion.
     *
     * @param task   The runnable task to be submitted.
     * @param result the result to return on successful completion. If
     *               you don't need a particular result, consider using
     *               constructions of the form:
     *               {@code Future<?> f = new FutureTask<Void>(runnable, null)}
     * @param <T> The type of the result to be returned.
     * @throws NullPointerException if the runnable is null
     */
    public <T> Future<T> submit(Runnable task, T result) {
        RunnableFuture<T> futureTask = newTaskFor(task, result);
        execute(futureTask);
        return futureTask;
    }

    /**
     * Submit callable.
     *
     * @param task The Callable object to be executed.
     * @param <T> The result type for the Callable object.
     */
    public <T> Future<T> submit(Callable<T> task) {
        RunnableFuture<T> futureTask = newTaskFor(task);
        execute(futureTask);
        return futureTask;
    }

    /**
     * Submit RunnableFuture task.
     *
     * @param task The RunnableFuture task to be executed.
     * @param <T> The result type of the RunnableFuture task.
     */
    public <T> void submit(RunnableFuture<T> task) {
        execute(task);
    }

    /**
     * Execute Runnable using the SmartExecutor.
     * When this method is called, {@link SmartExecutor} will perform action as per:
     * <ol>
     * <li>if fewer than {@link #coreSize} tasks are running,
     * post new task in {@link #runningList} and execute it immediately.</li>
     * <li>if more than {@link #coreSize} tasks are running,
     * and fewer than {@link #queueSize} tasks are waiting, put task in {@link #waitingList}.</li>
     * <li>if more than {@link #queueSize} tasks are waiting ,schedule new task by {@link OverloadPolicy}</li>
     * <li>if running task is completed, schedule next task by {@link SchedulePolicy}
     * until {@link #waitingList} is empty.</li>
     * </ol>
     *
     * @param command The Runnable object that is to be executed.
     */
    @Override
    public void execute(final Runnable command) {
        if (command == null) {
            return;
        }

        WrappedRunnable scheduler = new WrappedRunnable(command);

        boolean callerRun = false;
        synchronized (staticLock) {
            if (runningList.size() < coreSize) {
                synchronized (runningList) {
                    runningList.add(scheduler);
                }
                threadPool.execute(scheduler);
            } else if (waitingList.size() < queueSize) {
                synchronized (waitingList) {
                    waitingList.addLast(scheduler);
                }
            } else {
                WrappedRunnable discardedTask;
                switch (overloadPolicy) {
                    case DISCARD_NEW_TASK_IN_QUEUE:
                        discardedTask = waitingList.pollLast();
                        if (discardedTask != null) {
                            discardedTask.setCancelled();
                        }
                        synchronized (waitingList) {
                            waitingList.addLast(scheduler);
                        }
                        break;
                    case DISCARD_OLD_TASK_IN_QUEUE:
                        discardedTask = waitingList.pollFirst();
                        if (discardedTask != null) {
                            discardedTask.setCancelled();
                        }
                        synchronized (waitingList) {
                            waitingList.addLast(scheduler);
                        }
                        break;
                    case CALLER_RUNS:
                        callerRun = true;
                        break;
                    case DISCARD_CURRENT_TASK:
                        break;
                    case THROW_EXCEPTION:
                        throw new TaskOverloadException(
                                "Task rejected from lite smart executor. " + command.toString());
                    default:
                        break;
                }
            }
        }
        if (callerRun) {
            logHelper.logInfo("SmartExecutor task running in caller thread");

            command.run();
        }
    }

    private static class TaskOverloadException extends RuntimeException {
        public TaskOverloadException(String message) {
            super(message);
        }
    }

    private void scheduleNext(WrappedRunnable scheduler) {
        synchronized (staticLock) {
            boolean suc;
            synchronized (runningList) {
                suc = runningList.remove(scheduler);
            }
            if (!suc) {
                synchronized (runningList) {
                    runningList.clear();
                }
                logHelper.logError(
                        "SmartExecutor scheduler remove failed."
                                + " Please clear all running tasks to avoid unpredictable error : %{public}s",
                        scheduler);

            }
            if (!waitingList.isEmpty()) {
                WrappedRunnable waitingRun;
                switch (schedulePolicy) {
                    case LAST_IN_FIRST_RUN:
                        synchronized (waitingList) {
                            waitingRun = waitingList.pollLast();
                        }
                        break;
                    case FIRST_IN_FIRST_RUN:
                        synchronized (waitingList) {
                            waitingRun = waitingList.pollFirst();
                        }
                        break;
                    default:
                        synchronized (waitingList) {
                            waitingRun = waitingList.pollLast();
                        }
                        break;
                }
                if (waitingRun != null) {
                    synchronized (runningList) {
                        runningList.add(waitingRun);
                    }
                    threadPool.execute(waitingRun);
                    logHelper.logInfo("Thread %{public}s is executing the next task...",
                        Thread.currentThread().getName());


                } else {
                    logHelper.logError("SmartExecutor got a NULL task from waiting queue: %{public}s",
                            Thread.currentThread().getName());

                }
            } else {
                logHelper.logInfo("SmartExecutor: All tasks are completed. Current thread: %{public}s",
                        Thread.currentThread().getName());

            }
        }
    }

    /**
     * Function will await the current thread until ALL tasks in the waiting list AND running list are completed,
     * cancelled, or interrupted.
     */
    public void awaitAll() {
        awaitWaitingListEmpty();
        awaitRunningListEmpty();
    }

    private void awaitWaitingListEmpty() {
        boolean isWaitingListEmpty = false;
        while (!isWaitingListEmpty) {
            WrappedRunnable firstTaskInWaitList;
            synchronized (waitingList) {
                if (!waitingList.isEmpty()) {
                    firstTaskInWaitList = waitingList.getFirst();
                } else {
                    isWaitingListEmpty = true;
                    continue;
                }
            }
            synchronized (firstTaskInWaitList) {
                try {
                    if (!firstTaskInWaitList.isCancelledOrCompleted()) {
                        firstTaskInWaitList.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }

        }
    }

    private void awaitRunningListEmpty() {
        boolean isRunningListEmpty = false;
        while (!isRunningListEmpty) {
            WrappedRunnable firstTaskInRunList;
            synchronized (runningList) {
                if (!runningList.isEmpty()) {
                    firstTaskInRunList = runningList.getFirst();
                } else {
                    isRunningListEmpty = true;
                    continue;
                }
            }
            synchronized (firstTaskInRunList) {
                try {
                    if (!firstTaskInRunList.isCancelledOrCompleted()) {
                        firstTaskInRunList.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }

        }
    }

    /**
     * Logs information about the {@link #threadPool} attached to this object.
     * Has no effect if {@link #loggerEnabled} is {@code false}
     */
    public void printThreadPoolInfo() {

        logHelper.logInfo("___________________________");
        logHelper.logInfo("state (shutdown - terminating - terminated): %{public}s - %{public}s - %{public}s",
                threadPool.isShutdown(), threadPool.isTerminating(), threadPool.isTerminated());
        logHelper.logInfo("pool size (core - max): %{public}d - %{public}d}",
                threadPool.getCorePoolSize(), threadPool.getMaximumPoolSize());
        logHelper.logInfo("task (active - complete - total): %{public}d - %{public}d - %{public}d",
                threadPool.getActiveCount(), threadPool.getCompletedTaskCount(), threadPool.getTaskCount());
        logHelper.logInfo("waitingList size : %{public}d , %{public}s",
                threadPool.getQueue().size(), threadPool.getQueue());

    }

    public int getCoreSize() {
        return coreSize;
    }

    /**
     * Set maximum number of concurrent tasks at the same time.
     * Recommended core size is CPU count.
     *
     * @param coreSize number of concurrent tasks at the same time
     * @return this
     */
    public SmartExecutor setCoreSize(int coreSize) {
        if (coreSize <= 0) {
            throw new IllegalArgumentException("coreSize can not be <= 0 !");
        }
        this.coreSize = coreSize;
        logHelper.logInfo(SMART_EXECUTOR_LOG_FORMAT,
                coreSize, queueSize, runningList.size(), waitingList.size());

        return this;
    }

    /** Gets the current size of the running queue of tasks.
     *
     * @return The current size of the running queue.
     */
    public int getRunningSize() {
        return runningList.size();
    }

    /** Gets the current size of the waiting queue of tasks.
     *
     * @return The current size of the waiting queue.
     */
    public int getWaitingSize() {
        return waitingList.size();
    }

    /** Gets the maximum queue size.
     *
     * @return The maximum size of the queue.
     */
    public int getQueueSize() {
        return queueSize;
    }

    /**
     * Adjust maximum number of waiting queue size by yourself or based on phone performance.
     * For example: CPU count * 32;
     *
     * @param queueSize waiting queue size
     * @return this
     * @throws NullPointerException If the queue size is less than 0.
     */
    public SmartExecutor setQueueSize(int queueSize) {
        if (queueSize < 0) {
            throw new NullPointerException("queueSize can not < 0 !");
        }

        this.queueSize = queueSize;
        logHelper.logInfo(SMART_EXECUTOR_LOG_FORMAT,
                coreSize, queueSize, runningList.size(), waitingList.size());

        return this;
    }

    public OverloadPolicy getOverloadPolicy() {
        return overloadPolicy;
    }

    /**
     * Sets the Overload Policy for this SmartExecutor object.
     * Overload Policy is used to handle the case when the waiting queue of tasks exceeds the maximum capacity.
     *
     * @param overloadPolicy The Overload Policy to be used.
     */
    public void setOverloadPolicy(OverloadPolicy overloadPolicy) {
        if (overloadPolicy == null) {
            throw new NullPointerException("OverloadPolicy can not be null !");
        }
        this.overloadPolicy = overloadPolicy;
    }

    public SchedulePolicy getSchedulePolicy() {
        return schedulePolicy;
    }

    /**
     * Sets the Schedule Policy for this SmartExecutor object.
     * Schedule Policy is used to decide which task will be run next, out of the waiting queue of tasks.
     *
     * @param schedulePolicy The Schedule Policy to be followed.
     */
    public void setSchedulePolicy(SchedulePolicy schedulePolicy) {
        if (schedulePolicy == null) {
            throw new NullPointerException("SchedulePolicy can not be null !");
        }
        this.schedulePolicy = schedulePolicy;
    }

    class WrappedRunnable implements Runnable {
        private final Runnable realRunnable;

        private boolean started = false;
        private boolean completed = false;
        private boolean cancelled = false;

        public boolean isStarted() {
            return started;
        }

        public boolean isCompleted() {
            return completed;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public boolean isCancelledOrCompleted() {
            return cancelled || completed;
        }

        public WrappedRunnable(Runnable command) {
            super();
            realRunnable = command;
        }

        public Runnable getRealRunnable() {
            return realRunnable;
        }

        @Override
        public void run() {
            synchronized (this) {
                started = true;
            }
            try {
                realRunnable.run();
            } finally {
                scheduleNext(this);
                synchronized (this) {
                    completed = true;
                    notifyAll();
                }
            }
        }

        public void setCancelled() {
            synchronized (this) {
                cancelled = true;
                notifyAll();
            }
        }
    }

}
