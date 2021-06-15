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
package com.applibgroup.harmony_os_lite_go;

import com.applibgroup.harmony_os_lite_go.utils.GoUtil;
import com.applibgroup.harmony_os_lite_go.utils.LogHelper;

import java.util.LinkedList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A smart thread pool executor, about {@link SmartExecutor}:
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

    /**
     * HMOS Log parameters
     */
    private static final String TAG = SmartExecutor.class.getSimpleName();
    private final int DOMAIN = 0x00201;
    private final LogHelper logHelper = new LogHelper(DOMAIN, TAG);

    private static final int DEFAULT_CACHE_SECOND = 5;
    /**
     * Static lock for synchronous static operations
     */
    private static final Object staticLock = new Object();

    private static ThreadPoolExecutor threadPool;
    /**
     * Lock objects for synchronous operation
     */
    private final Object lock = new Object();
    private final LinkedList<WrappedRunnable> runningList = new LinkedList<>();
    private final LinkedList<WrappedRunnable> waitingList = new LinkedList<>();
    /**
     * To log Debug Messages like thread start details, task finished messages, etc.
     */
    private boolean debug = false;
    /**
     * Core size, or the maximum number of concurrent threads, as used by the SmartExecutor.
     * By default, this equals the number of CPU cores.
     */
    private int coreSize;
    private int queueSize;
    private SchedulePolicy schedulePolicy = SchedulePolicy.FirstInFirstRun;
    private OverloadPolicy overloadPolicy = OverloadPolicy.DiscardOldTaskInQueue;

    /**
     * Create a SmartExecutor Object with number of cores equal to the CPU core count,
     * length of waiting queue equal to 32 times number of cores.
     */
    public SmartExecutor() {
        coreSize = GoUtil.getCoresCount();
        queueSize = coreSize * 32;
        initThreadPool();
    }

    /**
     * Create a SmartExecutor Object specifying number of cores and queue size.
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
     * Create a ThreadPoolExecutor used internally to handle threads
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
            if (debug) {
                logHelper.logInfo("SmartExecutor core-queue size: %{public}d - %{public}d  running-wait task: %{public}d - %{public}d",
                        coreSize, queueSize, runningList.size(), waitingList.size());
            }
            if (threadPool == null) {
                threadPool = createDefaultThreadPool();
            }
        }
    }

    public boolean isDebug() {
        return debug;
    }

    /**
     * turn on or turn off debug mode
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
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
                        waitingList.remove(i);
                        removed = true;
                    }
                }
            }
        }
        return removed;
    }

    /**
     * Create a runnable FutureTask
     */
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new FutureTask<>(runnable, value);
    }

    /**
     * Create a callable FutureTask
     */
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new FutureTask<>(callable);
    }

    /**
     * submit runnable
     */
    public Future<?> submit(Runnable task) {
        RunnableFuture<Void> futureTask = newTaskFor(task, null);
        execute(futureTask);
        return futureTask;
    }

    /**
     * Creates a {@code FutureTask} that will, upon running, execute the
     * given {@code Runnable}, and arrange that {@code get} will return the
     * given result on successful completion.
     *
     * @param task   the runnable task
     * @param result the result to return on successful completion. If
     *               you don't need a particular result, consider using
     *               constructions of the form:
     *               {@code Future<?> f = new FutureTask<Void>(runnable, null)}
     * @throws NullPointerException if the runnable is null
     */
    public <T> Future<T> submit(Runnable task, T result) {
        RunnableFuture<T> futureTask = newTaskFor(task, result);
        execute(futureTask);
        return futureTask;
    }

    /**
     * submit callable
     */
    public <T> Future<T> submit(Callable<T> task) {
        RunnableFuture<T> futureTask = newTaskFor(task);
        execute(futureTask);
        return futureTask;
    }

    /**
     * submit RunnableFuture task
     */
    public <T> void submit(RunnableFuture<T> task) {
        execute(task);
    }

    /**
     * When this method is called, {@link SmartExecutor} will perform action as per:
     * <ol>
     * <li>if fewer than {@link #coreSize} tasks are running, post new task in {@link #runningList} and execute it immediately.</li>
     * <li>if more than {@link #coreSize} tasks are running, and fewer than {@link #queueSize} tasks are waiting, put task in {@link #waitingList}.</li>
     * <li>if more than {@link #queueSize} tasks are waiting ,schedule new task by {@link OverloadPolicy}</li>
     * <li>if running task is completed, schedule next task by {@link SchedulePolicy} until {@link #waitingList} is empty.</li>
     * </ol>
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
                runningList.add(scheduler);
                threadPool.execute(scheduler);
            } else if (waitingList.size() < queueSize) {
                waitingList.addLast(scheduler);
            } else {
                WrappedRunnable discardedTask;
                switch (overloadPolicy) {
                    case DiscardNewTaskInQueue:
                        discardedTask = waitingList.pollLast();
                        if (discardedTask != null)
                            discardedTask.setCancelled();
                        waitingList.addLast(scheduler);
                        break;
                    case DiscardOldTaskInQueue:
                        discardedTask = waitingList.pollFirst();
                        if (discardedTask != null)
                            discardedTask.setCancelled();
                        waitingList.addLast(scheduler);
                        break;
                    case CallerRuns:
                        callerRun = true;
                        break;
                    case DiscardCurrentTask:
                        break;
                    case ThrowException:
                        throw new RuntimeException("Task rejected from lite smart executor. " + command.toString());
                    default:
                        break;
                }
            }
        }
        if (callerRun) {
            if (debug) {
                logHelper.logInfo( "SmartExecutor task running in caller thread");
            }
            command.run();
        }
    }

    private void scheduleNext(WrappedRunnable scheduler) {
        synchronized (staticLock) {
            boolean suc = runningList.remove(scheduler);
            if (!suc) {
                runningList.clear();
                logHelper.logError(
                        "SmartExecutor scheduler remove failed. Please clear all running tasks to avoid unpredictable error : %{public}s",
                        scheduler);

            }
            if (waitingList.size() > 0) {
                WrappedRunnable waitingRun;
                switch (schedulePolicy) {
                    case LastInFirstRun:
                        waitingRun = waitingList.pollLast();
                        break;
                    case FirstInFirstRun:
                        waitingRun = waitingList.pollFirst();
                        break;
                    default:
                        waitingRun = waitingList.pollLast();
                        break;
                }
                if (waitingRun != null) {
                    runningList.add(waitingRun);
                    threadPool.execute(waitingRun);
                    if (debug) {
                        logHelper.logInfo("Thread %{public}s is executing the next task...",
                            Thread.currentThread().getName());
                    }

                } else {
                    if (debug) {
                        logHelper.logError("SmartExecutor got a NULL task from waiting queue: %{public}s",
                                Thread.currentThread().getName());
                    }
                }
            } else {
                if (debug) {
                    logHelper.logInfo("SmartExecutor: All tasks are completed. Current thread: %{public}s",
                            Thread.currentThread().getName());
                }
            }
        }
    }

    /**
     * Function will await the current thread until ALL tasks in the waiting list AND running list are completed, cancelled, or interrupted.
     */
    public void awaitAll() {
        while (!waitingList.isEmpty()){
            synchronized (waitingList.getFirst()) {
                try {
                    waitingList.getFirst().wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        while (!runningList.isEmpty()){
            synchronized (runningList.getFirst()) {
                try {
                    runningList.getFirst().wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Logs information about the threadPool attached to this object. Has no effect if {@link #debug} is {@code false}
     */
    public void printThreadPoolInfo() {
        if (debug) {

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
        if (debug) {
            logHelper.logInfo("SmartExecutor core-queue size: %{public}d - %{public}d  running-wait task: %{public}d - %{public}d",
                    coreSize, queueSize, runningList.size(), waitingList.size());
        }
        return this;
    }

    public int getRunningSize() {
        return runningList.size();
    }

    public int getWaitingSize() {
        return waitingList.size();
    }

    public int getQueueSize() {
        return queueSize;
    }

    /**
     * Adjust maximum number of waiting queue size by yourself or based on phone performance.
     * For example: CPU count * 32;
     *
     * @param queueSize waiting queue size
     * @return this
     */
    public SmartExecutor setQueueSize(int queueSize) {
        if (queueSize < 0) {
            throw new NullPointerException("queueSize can not < 0 !");
        }

        this.queueSize = queueSize;
        if (debug) {
            logHelper.logInfo("SmartExecutor core-queue size: %{public}d - %{public}d  running-wait task: %{public}d - %{public}d",
                    coreSize, queueSize, runningList.size(), waitingList.size());
        }
        return this;
    }

    public OverloadPolicy getOverloadPolicy() {
        return overloadPolicy;
    }

    public void setOverloadPolicy(OverloadPolicy overloadPolicy) {
        if (overloadPolicy == null) {
            throw new NullPointerException("OverloadPolicy can not be null !");
        }
        this.overloadPolicy = overloadPolicy;
    }

    public SchedulePolicy getSchedulePolicy() {
        return schedulePolicy;
    }

    public void setSchedulePolicy(SchedulePolicy schedulePolicy) {
        if (schedulePolicy == null) {
            throw new NullPointerException("SchedulePolicy can not be null !");
        }
        this.schedulePolicy = schedulePolicy;
    }

    class WrappedRunnable implements Runnable{

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

        public WrappedRunnable(Runnable command) {
            super();
            realRunnable = command;
        }

        public Runnable getRealRunnable() {
            return realRunnable;
        }

        @Override
        public void run() {
            synchronized (this){
                started = true;
            }
            try {
                realRunnable.run();
            } finally {
                scheduleNext(this);
                synchronized (this) {
                    completed = true;
                    notify();
                }
            }
        }

        public void setCancelled() {
            synchronized (this){
                cancelled = true;
                notify();
            }
        }
    }

}
