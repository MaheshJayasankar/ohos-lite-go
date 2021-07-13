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

package com.litesuits.go;

import com.litesuits.go.utils.GoUtil;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class SmartExecutorTest {
    @BeforeClass
    public static void setUpTestEnvironment()
    {
        GoUtil.setTestingEnv(true);
        int coresCount = GoUtil.getCoresCount();
        assertTrue(coresCount > 0);
    }
    @Test
    public void testSmartExecutorInit()
    {
        SmartExecutor smartExecutor1 = new SmartExecutor();
        assertTrue(smartExecutor1.getCoreSize() > 0);
        SmartExecutor smartExecutor2 = new SmartExecutor(2,2);
        assertTrue(smartExecutor2.getCoreSize() > 0);
        SmartExecutor smartExecutor3 = new SmartExecutor(2,2);
        smartExecutor3.setSchedulePolicy(SchedulePolicy.FIRST_IN_FIRST_RUN);
        smartExecutor3.setOverloadPolicy(OverloadPolicy.DISCARD_NEW_TASK_IN_QUEUE);
        assertTrue(smartExecutor3.getCoreSize() > 0);
    }
    @Test
    public void testPrintThreadPoolInfo() {
        SmartExecutor smartExecutor = new SmartExecutor();
        smartExecutor.setLoggerEnabled(false);
        smartExecutor.printThreadPoolInfo();
        assertFalse(smartExecutor.isLoggerEnabled());
    }
    @Test
    public void testGetThreadPool() {
        assertNotNull(SmartExecutor.getThreadPool());
    }
    @Test
    public void testSetThreadPool() {
        ThreadPoolExecutor newThreadPool = new ThreadPoolExecutor(
                2,
                Integer.MAX_VALUE,
                2, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new ThreadFactory() {
                    static final String NAME = "lite-";
                    final AtomicInteger ids = new AtomicInteger(1);

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, NAME + ids.getAndIncrement());
                    }
                },
                new ThreadPoolExecutor.DiscardPolicy());
        SmartExecutor.setThreadPool(newThreadPool);
        assertSame(SmartExecutor.getThreadPool(), newThreadPool);
    }
    @Test
    public void testCancelWaitingTask() {
        // Add 3 tasks, and then cancel the last one. Confirm if task was successfully cancelled.
        SmartExecutor smartExecutor = new SmartExecutor(2, 2);
        List<Integer> tasksStartedOrder = new ArrayList<>();
        List<Integer> tasksFinishedOrder = new ArrayList<>();
        TestRunnable command1 = new TestRunnable(tasksStartedOrder, tasksFinishedOrder, 1);
        TestRunnable command2 = new TestRunnable(tasksStartedOrder, tasksFinishedOrder, 2);
        TestRunnable command3 = new TestRunnable(tasksStartedOrder, tasksFinishedOrder, 3);
        smartExecutor.execute(command1);
        smartExecutor.execute(command2);
        smartExecutor.execute(command3);
        assertTrue(smartExecutor.cancelWaitingTask(command3));
    }
    @Test
    public void testSubmitRunnable() {
        SmartExecutor smartExecutor = new SmartExecutor(2, 2);
        Future<Void> future = smartExecutor.submit(() -> {});
        assertNotNull(future);
    }

    @Test
    public void testSubmitRunnableResult() {
        SmartExecutor smartExecutor = new SmartExecutor(2, 2);
        Integer result = null;
        Future<Integer> future = smartExecutor.submit(()->{}, 1);
        smartExecutor.awaitAll();
        try {
            result = future.get();
        } catch (ExecutionException ignored) {
            // Exception is ignored
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertEquals(Integer.valueOf(1), result);
    }

    @Test
    public void testSubmitCallable() {
        SmartExecutor smartExecutor = new SmartExecutor(2, 2);
        Future<Void> future = smartExecutor.submit(() -> null);
        assertNotNull(future);
    }

    @Test
    public void testSubmitRunnableFuture() {
        SmartExecutor smartExecutor = new SmartExecutor(2, 2);
        smartExecutor.submit(new RunnableFuture<Void>() {
            @Override
            public boolean cancel(boolean b) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public Void get() {
                return null;
            }

            @Override
            public Void get(long l, TimeUnit timeUnit) {
                return null;
            }

            @Override
            public void run() {
                // Left empty so that the task finishes execution immediately.
            }
        });
        smartExecutor.awaitAll();
        assertEquals(0, smartExecutor.getRunningSize());
    }

    @Test
    public void testExecute() {
        SmartExecutor smartExecutor = new SmartExecutor(2,2);
        smartExecutor.execute(null);
        smartExecutor.execute(() -> {});
        smartExecutor.awaitAll();
        assertEquals(0, smartExecutor.getRunningSize());
    }

    @Test
    public void testAwaitAll() {
        // Execute 3 tasks, and await their completion. Test whether no tasks are still running after waiting.
        SmartExecutor smartExecutor = new SmartExecutor(2,2);
        smartExecutor.execute(() -> {});
        smartExecutor.execute(() -> {});
        smartExecutor.execute(() -> {});
        smartExecutor.awaitAll();
        assertEquals(0, smartExecutor.getRunningSize());
    }

    @Test
    public void testGetCoreSize() {
        SmartExecutor smartExecutor = new SmartExecutor(2,2);
        assertEquals(2,smartExecutor.getCoreSize());
    }

    @Test
    public void testSetCoreSize() {
        SmartExecutor smartExecutor = new SmartExecutor(1,2);
        smartExecutor.setCoreSize(2);
        assertEquals(2,smartExecutor.getCoreSize());
    }
    @Test
    public void testSetCoreSizeThrowException() {
        SmartExecutor smartExecutor = new SmartExecutor(1,2);
        assertThrows(IllegalArgumentException.class,() -> smartExecutor.setCoreSize(-1));
    }

    @Test
    public void testGetRunningSize() {
        SmartExecutor smartExecutor = new SmartExecutor(2,2);
        assertEquals(0, smartExecutor.getRunningSize());

    }

    @Test
    public void testGetWaitingSize() {
        SmartExecutor smartExecutor = new SmartExecutor(2,2);
        assertEquals(0, smartExecutor.getWaitingSize());
    }

    @Test
    public void testGetQueueSize() {
        SmartExecutor smartExecutor = new SmartExecutor(2,2);
        assertEquals(2, smartExecutor.getQueueSize());
    }

    @Test
    public void testSetQueueSize() {
        SmartExecutor smartExecutor = new SmartExecutor(2,1);
        smartExecutor.setQueueSize(2);
        assertEquals(2, smartExecutor.getQueueSize());
    }
    @Test
    public void testSetQueueSizeThrowException() {
        SmartExecutor smartExecutor = new SmartExecutor(2,1);
        assertThrows(NullPointerException.class,()-> smartExecutor.setQueueSize(-1));
    }

    @Test
    public void testGetOverloadPolicy() {
        SmartExecutor smartExecutor = new SmartExecutor(2,2);
        assertEquals(OverloadPolicy.DISCARD_OLD_TASK_IN_QUEUE, smartExecutor.getOverloadPolicy());
    }

    @Test
    public void testSetOverloadPolicy() {
        SmartExecutor smartExecutor = new SmartExecutor(2,2);
        smartExecutor.setOverloadPolicy(OverloadPolicy.DISCARD_NEW_TASK_IN_QUEUE);
        assertEquals(OverloadPolicy.DISCARD_NEW_TASK_IN_QUEUE, smartExecutor.getOverloadPolicy());
    }

    @Test
    public void testGetSchedulePolicy() {
        SmartExecutor smartExecutor = new SmartExecutor(2,2);
        assertEquals(SchedulePolicy.FIRST_IN_FIRST_RUN, smartExecutor.getSchedulePolicy());
    }

    @Test
    public void testSetSchedulePolicy() {
        SmartExecutor smartExecutor = new SmartExecutor(2,2);
        smartExecutor.setSchedulePolicy(SchedulePolicy.LAST_IN_FIRST_RUN);
        assertEquals(SchedulePolicy.LAST_IN_FIRST_RUN, smartExecutor.getSchedulePolicy());
    }
    @Test
    public void testOverloadPolicyDiscardOld() {
        // Overload Policy Discard Old Task will discard oldest task in waiting queue when new one is added.
        // If tasks are added in sequence of 0,1,2,3,4,5. With 2 tasks running concurrently and 2 maximum tasks in queue
        // Then the tasks 2,3 are discarded by policy, and the result is that tasks 0,1,4,5 are completed.
        List<Integer> expectedTaskFinishOrder = Arrays.asList(0, 1, 4, 5);
        TaskListExecutionResult taskSequenceResults = runSequenceOfTestTasks(6, SchedulePolicy.FIRST_IN_FIRST_RUN, OverloadPolicy.DISCARD_OLD_TASK_IN_QUEUE);
        List<Integer> observedTaskFinishOrder = taskSequenceResults.getTasksFinishedOrder();

        assertEquals(expectedTaskFinishOrder, observedTaskFinishOrder);
    }
    @Test
    public void testOverloadPolicyDiscardNew() {
        // Overload Policy Discard New Task will discard the most recent task in waiting queue when new one is added.
        // If tasks are added in sequence of 0,1,2,3,4,5. With 2 tasks running concurrently and 2 maximum tasks in queue
        // Then the tasks 3,4 are discarded by policy, and the result is that tasks 0,1,2,5 are completed.
        List<Integer> expectedTaskFinishOrder = Arrays.asList(0,1,2,5);
        TaskListExecutionResult taskSequenceResults =  runSequenceOfTestTasks(6, SchedulePolicy.FIRST_IN_FIRST_RUN, OverloadPolicy.DISCARD_NEW_TASK_IN_QUEUE);
        List<Integer> observedTaskFinishOrder = taskSequenceResults.getTasksFinishedOrder();

        assertEquals(expectedTaskFinishOrder, observedTaskFinishOrder);
    }
    @Test
    public void testOverloadPolicyDiscardCurrent() {
        // Overload Policy Discard Current Task will discard the task to be added if the queue is full.
        // If tasks are added in sequence of 0,1,2,3,4,5. With 2 tasks running concurrently and 2 maximum tasks in queue
        // Then the tasks 4,5 are discarded by policy, and the result is that tasks 0,1,2,3 are completed.
        List<Integer> expectedTaskFinishOrder = Arrays.asList(0,1,2,3);
        TaskListExecutionResult taskSequenceResults =  runSequenceOfTestTasks(6, SchedulePolicy.FIRST_IN_FIRST_RUN, OverloadPolicy.DISCARD_CURRENT_TASK);
        List<Integer> observedTaskFinishOrder = taskSequenceResults.getTasksFinishedOrder();

        assertEquals(expectedTaskFinishOrder, observedTaskFinishOrder);
    }
    @Test
    public void testOverloadPolicyCallerRuns() {
        // Overload Policy Discard Caller Runs will run the task to be added on the current thread.
        // If tasks are added in sequence of 0,1,2,3,4,5. With 2 tasks running concurrently and 2 maximum tasks in queue
        // The task 4 is run on the current thread, freezing the thread until completion of task 4. Meanwhile, the rest
        // of the tasks in the waiting list will finish execution, providing space in the queue for the next added task
        // The result is that all tasks will execute, as no task is discarded
        List<Integer> expectedTaskFinishOrder = Arrays.asList(0,1,2,3,4,5);
        TaskListExecutionResult taskSequenceResults =  runSequenceOfTestTasks(6, SchedulePolicy.FIRST_IN_FIRST_RUN, OverloadPolicy.CALLER_RUNS);
        List<Integer> observedTaskFinishOrder = taskSequenceResults.getTasksFinishedOrder();

        assertEquals(expectedTaskFinishOrder, observedTaskFinishOrder);
    }
    @Test
    public void testOverloadPolicyThrowException() {
        // TaskOverloadException, a subclass of RuntimeException is thrown in case a task is attempted to be added to
        // the already full queue.
        assertThrows(RuntimeException.class, ()->
                runSequenceOfTestTasks(5, SchedulePolicy.FIRST_IN_FIRST_RUN, OverloadPolicy.THROW_EXCEPTION
        ));
    }
    @Test
    public void testSchedulePolicyFirstInFirstRun() {
        // Schedule Policy Discard First in First Run will execute the first added task in the waiting queue first.
        // If tasks are added in sequence of 0,1,2,3,4,5. With 2 tasks running concurrently and 2 maximum tasks in queue
        // With the default overload policy, only tasks 4 and 5 will remain in the queue. Out of this, task 4 starts
        // execution before task 5 due to the fact that it was added first.
        List<Integer> expectedTaskStartOrder = Arrays.asList(0, 1, 4, 5);
        TaskListExecutionResult taskSequenceResults = runSequenceOfTestTasks(6, SchedulePolicy.FIRST_IN_FIRST_RUN, OverloadPolicy.DISCARD_OLD_TASK_IN_QUEUE);
        List<Integer> observedTaskStartOrder = taskSequenceResults.getTasksStartedOrder();

        assertEquals(expectedTaskStartOrder, observedTaskStartOrder);
    }
    @Test
    public void testSchedulePolicyLastInFirstRun() {
        // Schedule Policy Discard Last in First Run will execute the last added task in the waiting queue first.
        // If tasks are added in sequence of 0,1,2,3,4,5. With 2 tasks running concurrently and 2 maximum tasks in queue
        // With the default overload policy, only tasks 4 and 5 will remain in the queue. Out of this, task 5 starts
        // execution before task 4 due to the fact that it was added last.
        List<Integer> expectedTaskStartOrder = Arrays.asList(0, 1, 5, 4);
        TaskListExecutionResult taskSequenceResults =  runSequenceOfTestTasks(6, SchedulePolicy.LAST_IN_FIRST_RUN, OverloadPolicy.DISCARD_OLD_TASK_IN_QUEUE);
        List<Integer> observedTaskStartOrder = taskSequenceResults.getTasksStartedOrder();

        assertEquals(expectedTaskStartOrder, observedTaskStartOrder);
    }

    /**
     * Method used to define a smart executor with the given SchedulePolicy and OverloadPolicy, with a core size of 2
     * and waiting queue size of 2, and provide the results.
     *
     * @param numTasks The number of tasks to be executed. Tasks are TestRunnable instances.
     * @param schedulePolicy SchedulePolicy to use by the SmartExecutor
     * @param overloadPolicy OverloadPolicy to use by the SmartExecutor
     * @return The result of the task execution, containing the order of start of execution of tasks and the order of
     *         end of execution of tasks.
     */
    private TaskListExecutionResult runSequenceOfTestTasks(int numTasks, SchedulePolicy schedulePolicy, OverloadPolicy overloadPolicy) {
        SmartExecutor smartExecutor = new SmartExecutor(2,2);
        smartExecutor.setSchedulePolicy(schedulePolicy);
        smartExecutor.setOverloadPolicy(overloadPolicy);

        return executeListOfTasks(smartExecutor, numTasks);
    }

    /**
     * Execute a list of tasks using the TestRunnable class.
     * @param smartExecutor The SmartExecutor object used to schedule the execution of the tasks
     * @param numTasks The number of tasks to be executed
     * @return The results of the task execution
     */
    private TaskListExecutionResult executeListOfTasks(SmartExecutor smartExecutor, int numTasks) {

        List<Integer> tasksStartedOrder = new ArrayList<>();
        List<Integer> tasksFinishedOrder = new ArrayList<>();

        for (int idx = 0; idx < numTasks; idx++)
        {
            TestRunnable command = new TestRunnable(tasksStartedOrder, tasksFinishedOrder, idx);
            smartExecutor.execute(command);
            try {
                TimeUnit.MILLISECONDS.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        smartExecutor.awaitAll();

        return new TaskListExecutionResult(tasksStartedOrder, tasksFinishedOrder);
    }

    /**
     * TestRunnable class that will run for 250 * (1 + taskIndex^2) milliseconds.
     * Upon start and finish of the task, updates the lists taskStartedOrder and taskFinishedOrder.
     * It is meant to be used within the executeListOfTasks function only.
     */
    private static class TestRunnable implements Runnable {
        private final List<Integer> tasksFinishedOrder;
        private final List<Integer> tasksStartedOrder;
        private final int taskIndex;
        private static final int BASE_RUN_TIME = 250;

        public TestRunnable(List<Integer> tasksStartedOrder, List<Integer> tasksFinishedOrder, int taskIndex) {
            this.tasksStartedOrder = tasksStartedOrder;
            this.tasksFinishedOrder = tasksFinishedOrder;
            this.taskIndex = taskIndex;
        }

        @Override
        public void run() {
            try {
                synchronized (tasksStartedOrder){
                    tasksStartedOrder.add(taskIndex);
                }
                int sleepTime = BASE_RUN_TIME * (taskIndex * taskIndex + 1);
                TimeUnit.MILLISECONDS.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                synchronized (tasksFinishedOrder) {
                    tasksFinishedOrder.add(taskIndex);
                }
            }
        }
    }

    /**
     * Stores the results of the execution of a sequence of tasks for the scheduling and overloading test cases.
     */
    private static class TaskListExecutionResult{
        private final List<Integer> tasksStartedOrder;
        private final List<Integer> tasksFinishedOrder;

        private TaskListExecutionResult(List<Integer> tasksStartedOrder, List<Integer> tasksFinishedOrder) {
            this.tasksStartedOrder = tasksStartedOrder;
            this.tasksFinishedOrder = tasksFinishedOrder;
        }

        public List<Integer> getTasksFinishedOrder() {
            return tasksFinishedOrder;
        }

        public List<Integer> getTasksStartedOrder() {
            return tasksStartedOrder;
        }
    }
}
