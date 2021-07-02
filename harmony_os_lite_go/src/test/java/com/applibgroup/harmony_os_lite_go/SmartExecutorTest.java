package com.applibgroup.harmony_os_lite_go;

import com.applibgroup.harmony_os_lite_go.utils.GoUtil;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SmartExecutorTest {
    @BeforeClass
    public static void setUpTestEnvironment()
    {
        GoUtil.setTestingEnv(true);
        int coresCount = GoUtil.getCoresCount();
        assert(coresCount > 0);
    }

    @Test
    public void testGoUtil(){
        SmartExecutor smartExecutor = new SmartExecutor();
        int goUtilCoresCount = GoUtil.getCoresCount();
        int smartExecutorCoreSize = smartExecutor.getCoreSize();
        assertTrue(goUtilCoresCount > 0);
        assertEquals(goUtilCoresCount, smartExecutorCoreSize);
    }
    @Test
    public void testSmartExecutorInit()
    {
        SmartExecutor smartExecutor1 = new SmartExecutor();
        assertTrue(smartExecutor1.getCoreSize() > 0);
        SmartExecutor smartExecutor2 = new SmartExecutor(2,2);
        assertTrue(smartExecutor2.getCoreSize() > 0);
        SmartExecutor smartExecutor3 = new SmartExecutor(2,2);
        smartExecutor3.setSchedulePolicy(SchedulePolicy.FirstInFirstRun);
        smartExecutor3.setOverloadPolicy(OverloadPolicy.DiscardNewTaskInQueue);
        assertTrue(smartExecutor3.getCoreSize() > 0);
    }
    @Test
    public void testOverloadPolicy(){
        List<Integer> discardOldPolicyExpectedTaskFinishedOrder = Arrays.asList(0,1,4,5);
        TaskListExecutionResult discardOldPolicyResult =  runSequenceOfTestTasks(6, SchedulePolicy.FirstInFirstRun, OverloadPolicy.DiscardOldTaskInQueue);
        List<Integer> discardOldPolicyTaskFinishedOrder = discardOldPolicyResult.getTasksFinishedOrder();

        assertEquals(discardOldPolicyExpectedTaskFinishedOrder, discardOldPolicyTaskFinishedOrder);

        List<Integer> discardNewPolicyExpectedTaskFinishedOrder = Arrays.asList(0,1,2,5);
        TaskListExecutionResult discardNewPolicyResult =  runSequenceOfTestTasks(6, SchedulePolicy.FirstInFirstRun, OverloadPolicy.DiscardNewTaskInQueue);
        List<Integer> discardNewPolicyTaskFinishedOrder = discardNewPolicyResult.getTasksFinishedOrder();

        assertEquals(discardNewPolicyExpectedTaskFinishedOrder, discardNewPolicyTaskFinishedOrder);
    }
    @Test
    public void testSchedulePolicy(){
        List<Integer> firstInFirstRunExpectedTaskStartedOrder = Arrays.asList(0,1,4,5);
        TaskListExecutionResult firstInFirstRunResult =  runSequenceOfTestTasks(6, SchedulePolicy.FirstInFirstRun, OverloadPolicy.DiscardOldTaskInQueue);
        List<Integer> firstInFirstRunTaskStartedOrder = firstInFirstRunResult.getTasksStartedOrder();

        assertEquals(firstInFirstRunExpectedTaskStartedOrder, firstInFirstRunTaskStartedOrder);

        List<Integer> lastInFirstRunExpectedTaskStartedOrder = Arrays.asList(0,1,5,4);
        TaskListExecutionResult lastInFirstRunResult =  runSequenceOfTestTasks(6, SchedulePolicy.LastInFirstRun, OverloadPolicy.DiscardOldTaskInQueue);
        List<Integer> lastInFirstRunTaskStartedOrder = lastInFirstRunResult.getTasksStartedOrder();

        assertEquals(lastInFirstRunExpectedTaskStartedOrder, lastInFirstRunTaskStartedOrder);
    }

    private TaskListExecutionResult runSequenceOfTestTasks(int numTasks, SchedulePolicy schedulePolicy, OverloadPolicy overloadPolicy) {
        SmartExecutor smartExecutor = new SmartExecutor(2,2);
        smartExecutor.setSchedulePolicy(schedulePolicy);
        smartExecutor.setOverloadPolicy(overloadPolicy);

        return executeListOfTasks(smartExecutor, numTasks);
    }

    private TaskListExecutionResult executeListOfTasks(SmartExecutor smartExecutor, int numTasks) {

        List<Integer> tasksStartedOrder = new ArrayList<>();
        List<Integer> tasksFinishedOrder = new ArrayList<>();
        List<TestRunnable> testRunnableList = new ArrayList<>();

        for (int idx = 0; idx < numTasks; idx++)
        {
            TestRunnable command = new TestRunnable(tasksStartedOrder, tasksFinishedOrder, idx);
            smartExecutor.execute(command);
            testRunnableList.add(command);
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        smartExecutor.awaitAll();

        return new TaskListExecutionResult(tasksStartedOrder, tasksFinishedOrder);
    }

    /**
     * TestRunnable class that will run for 100 * (1 + taskIndex^2) milliseconds.
     * Upon start and finish of the task, updates the lists taskStartedOrder and taskFinishedOrder.
     * It is meant to be used within the executeListOfTasks function only.
     */
    private class TestRunnable implements Runnable {
        private final List<Integer> tasksFinishedOrder;
        private final List<Integer> tasksStartedOrder;
        private final int taskIndex;
        private final int baseRunTime = 100;

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
                Thread.sleep(baseRunTime * (taskIndex*taskIndex + 1));
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                synchronized (tasksFinishedOrder) {
                    tasksFinishedOrder.add(taskIndex);
                }
            }
        }
    }

    private class TaskListExecutionResult{
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
