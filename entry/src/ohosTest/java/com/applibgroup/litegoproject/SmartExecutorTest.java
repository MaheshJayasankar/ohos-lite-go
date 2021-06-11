package com.applibgroup.litegoproject;

import com.applibgroup.harmony_os_lite_go.OverloadPolicy;
import com.applibgroup.harmony_os_lite_go.SchedulePolicy;
import com.applibgroup.harmony_os_lite_go.SmartExecutor;
import com.applibgroup.harmony_os_lite_go.utils.GoUtil;
import ohos.miscservices.timeutility.Time;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SmartExecutorTest {
    @Test
    public void testGoUtil(){
        SmartExecutor smartExecutor = new SmartExecutor();
        int goUtilCoresCount = GoUtil.getCoresCount();
        int smartExecutorCoreSize = smartExecutor.getCoreSize();
        assert(goUtilCoresCount > 0);
        assert(goUtilCoresCount == smartExecutorCoreSize);
    }
    @Test
    public void testSmartExecutorInit()
    {
        SmartExecutor smartExecutor1 = new SmartExecutor();
        assert(smartExecutor1.getClass().equals(SmartExecutor.class));
        SmartExecutor smartExecutor2 = new SmartExecutor(2,2);
        assert(smartExecutor2.getClass().equals(SmartExecutor.class));
        SmartExecutor smartExecutor3 = new SmartExecutor(2,2, SchedulePolicy.FirstInFirstRun, OverloadPolicy.DiscardNewTaskInQueue);
        assert(smartExecutor3.getClass().equals(SmartExecutor.class));
    }
    @Test
    public void testOverloadPolicy(){
        testOverloadPolicyDiscardOldTaskInQueue();
        testOverloadPolicyDiscardNewTaskInQueue();
    }
    @Test
    public void testSchedulePolicy(){
        testSchedulePolicyFirstInFirstRun();
        testSchedulePolicyLastInFirstRun();
    }

    private void testOverloadPolicyDiscardNewTaskInQueue() {
        SmartExecutor smartExecutor = new SmartExecutor(2,2,SchedulePolicy.FirstInFirstRun, OverloadPolicy.DiscardNewTaskInQueue);
        List<Boolean> tasksFinished = Arrays.asList(false, false, false, false);
        for (int idx = 0; idx <= 4; idx++)
        {

            int finalIdx = idx;
            smartExecutor.execute(() -> {
                Time.sleep(2000);
                tasksFinished.set(finalIdx, true);
            });
        }

        List<Boolean> expectedResult = Arrays.asList(true, true, false, false);
        assert(expectedResult.equals(tasksFinished));
    }
    private void testOverloadPolicyDiscardOldTaskInQueue() {
        SmartExecutor smartExecutor = new SmartExecutor(2,2,SchedulePolicy.FirstInFirstRun, OverloadPolicy.DiscardOldTaskInQueue);
        List<Boolean> tasksFinished = Arrays.asList(false, false, false, false);
        for (int idx = 0; idx <= 4; idx++)
        {

            int finalIdx = idx;
            smartExecutor.execute(() -> {
                Time.sleep(2000);
                tasksFinished.set(finalIdx, true);
            });
        }

        List<Boolean> expectedResult = Arrays.asList(false, false, true, true);
        assert(expectedResult.equals(tasksFinished));
    }
    private void testSchedulePolicyFirstInFirstRun(){
        SmartExecutor smartExecutor = new SmartExecutor(1,2, SchedulePolicy.FirstInFirstRun, OverloadPolicy.DiscardOldTaskInQueue);
        List<Integer> tasksFinishedOrder = new ArrayList<>();
        for (int idx = 0; idx <= 3; idx++)
        {

            int finalIdx = idx;
            smartExecutor.execute(() -> {
                Time.sleep(2000);
                tasksFinishedOrder.add(finalIdx);
            });
        }

        List<Integer> expectedResult = Arrays.asList(0,1,2);
        assert(expectedResult.equals(tasksFinishedOrder));
    }
    private void testSchedulePolicyLastInFirstRun(){
        SmartExecutor smartExecutor = new SmartExecutor(1,2, SchedulePolicy.LastInFirstRun, OverloadPolicy.DiscardOldTaskInQueue);
        List<Integer> tasksFinishedOrder = new ArrayList<>();
        for (int idx = 0; idx <= 3; idx++)
        {

            int finalIdx = idx;
            smartExecutor.execute(() -> {
                Time.sleep(2000);
                tasksFinishedOrder.add(finalIdx);
            });
        }

        List<Integer> expectedResult = Arrays.asList(0,2,1);
        assert(expectedResult.equals(tasksFinishedOrder));
    }
}
