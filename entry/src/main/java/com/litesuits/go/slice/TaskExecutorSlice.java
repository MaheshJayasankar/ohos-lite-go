/*
 * Copyright (C) 2020-21 Application Library Engineering Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.litesuits.go.slice;

import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.agp.components.DirectionalLayout;
import ohos.agp.components.Text;
import ohos.hiviewdfx.HiLog;
import ohos.hiviewdfx.HiLogLabel;
import com.litesuits.go.OverloadPolicy;
import com.litesuits.go.ResourceTable;
import com.litesuits.go.SchedulePolicy;
import com.litesuits.go.SmartExecutor;
import java.util.LinkedList;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * This AbilitySlice will perform the chosen operation and display the results.
 */
public class TaskExecutorSlice extends AbilitySlice {
    // OHOS Log parameters
    private static final String TAG = TaskExecutorSlice.class.getSimpleName();
    private static final int DOMAIN = 0x00101;
    private static final HiLogLabel LABEL = new HiLogLabel(HiLog.LOG_APP, DOMAIN, TAG);
    public static final String SELECTED_OPTION = "selectedOption";
    public static final int LOGGER_TIMEOUT = 5000;
    private DirectionalLayout logContainer;
    private final LinkedList<String> logQueue = new LinkedList<>();
    protected SmartExecutor mainExecutor;

    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);
        super.setUIContent(ResourceTable.Layout_ability_task_executor);
        initViews();
        initSmartExecutor();

        int selectedOption = intent.getIntParam(SELECTED_OPTION, 0);
        runSelectedOption(selectedOption);

    }

    @Override
    protected void onActive() {
        super.onActive();
        logContainer.removeAllComponents();
        listenForLogs();
    }

    private void initViews() {
        logContainer = (DirectionalLayout) findComponentById(ResourceTable.Id_log_container);
    }

    private void initSmartExecutor() {
        if (mainExecutor == null) {
            // set this temporary parameter, just for test
            // Intelligent Concurrent Scheduling Controller: Set the [Maximum Concurrency Number]
            // and [Waiting Queue] size for testing purposes only, according to actual scenarios
            mainExecutor = new SmartExecutor();

            // To turn on Debugging and Logging features. It is recommended to turn off in production.
            mainExecutor.setLoggerEnabled(true);

            // number of concurrent threads at the same time, recommended core size is CPU count
            mainExecutor.setCoreSize(2);

            // adjust maximum number of waiting queue size by yourself or based on device performance
            mainExecutor.setQueueSize(100);

            // If number of tasks exceeds number of cores,
            // decide how to deal with the tasks when they exit the waiting queue
            mainExecutor.setSchedulePolicy(SchedulePolicy.LAST_IN_FIRST_RUN);

            // If number of tasks exceed the length of the waiting queue, decide to discard oldest task in queue
            mainExecutor.setOverloadPolicy(OverloadPolicy.DISCARD_OLD_TASK_IN_QUEUE);
            printAndLog("Smart Executor has been Initialized.");
        }
    }

    private void runSelectedOption(final int option) {
        switch (option) {
            case 0:
                printAndLog("Trying to submit runnable.");
                // 0. Submit Runnable
                mainExecutor.submit(() -> {
                    printAndLog(" Runnable start!  thread id: %d",
                            Thread.currentThread().getId());
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                    printAndLog(" Runnable end!  thread id: %{public}d",
                            Thread.currentThread().getId());
                });
                break;

            case 1:
                // 1. Submit FutureTask
                FutureTask<String> futureTask = new FutureTask<>(() -> {
                    printAndLog(" FutureTask thread id: %{public}d",
                            Thread.currentThread().getId());
                    return "FutureTask";
                });
                mainExecutor.submit(futureTask);
                printAndLog("FutureTask submitted");
                break;

            case 2:
                // 2. Submit Callable
                mainExecutor.submit(() -> {
                    printAndLog(" Callable thread id: %{public}d",
                            Thread.currentThread().getId());
                    return "Callable";
                });
                printAndLog("Callable submitted");
                break;

            case 3:
                // 3. Strategy Test

                // Create a SmartExecutor object with default parameters. They are specified in the following lines
                SmartExecutor smartExecutor = new SmartExecutor();

                // Number of concurrent threads at the same time, recommended core size is CPU count
                smartExecutor.setCoreSize(2);

                // adjust maximum capacity of waiting queue by yourself or based on device performance
                smartExecutor.setQueueSize(2);

                // After the number of tasks exceeds Maximum Number of Concurrent tasks (core size), any new tasks
                // automatically enter the Waiting Queue and wait for the completion of the currently executing tasks.
                // After a executing task finishes, a task from the waiting queue enters the execution state according
                // to the strategy: last-in first-run
                smartExecutor.setSchedulePolicy(SchedulePolicy.LAST_IN_FIRST_RUN);

                // When the number of new tasks added subsequently exceeds the maximum capacity of the waiting queue,
                // the overload strategy is executed. In this case, the oldest task in the queue is discarded.
                smartExecutor.setOverloadPolicy(OverloadPolicy.DISCARD_OLD_TASK_IN_QUEUE);

                smartExecutor.setLoggerEnabled(true);

                // put in 4 tasks at once
                for (int i = 0; i < 4; i++) {
                    final int j = i;
                    smartExecutor.execute(() -> {
                        printAndLog(" TASK %d is running now ----------->", j);
                        try {
                            Thread.sleep(j * (long) 200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            Thread.currentThread().interrupt();
                        }
                    });
                }

                // A new task is added to the list, but it is immediately canceled.
                Future<?> future = smartExecutor.submit(() -> {
                    printAndLog(" TASK 4 will be cancelled ... ------------>");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                });
                boolean cancelResult = future.cancel(false);
                printAndLog("TASK 4 was cancelled successfully? %s", cancelResult);
                break;
            default: break;
        }
    }

    private void printAndLog(String format, Object... args) {
        String outputString = String.format(format, args);
        HiLog.info(LABEL, outputString);
        synchronized (logQueue) {
            logQueue.add(outputString);
            logQueue.notifyAll();
        }
    }

    private void createTextLog(String textContent) {
        Text text = new Text(this);
        text.setText(textContent);
        text.setPadding(16, 16, 16, 32);
        logContainer.addComponent(text);
    }

    private void listenForLogs() {
        while (true) {
            try {
                synchronized (logQueue) {
                    logQueue.wait(LOGGER_TIMEOUT);
                }
                if (!logQueue.isEmpty()) {
                    String latestLog = logQueue.pollFirst();
                    createTextLog(latestLog);
                } else {
                    break;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }
}
