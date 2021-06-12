package com.applibgroup.litegoproject;

import com.applibgroup.harmony_os_lite_go.OverloadPolicy;
import com.applibgroup.harmony_os_lite_go.SchedulePolicy;
import com.applibgroup.harmony_os_lite_go.SmartExecutor;
import com.applibgroup.harmony_os_lite_go.utils.GoUtil;
import org.junit.Test;

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
        assert(smartExecutor1.getCoreSize() > 0);
        SmartExecutor smartExecutor2 = new SmartExecutor(2,2);
        assert(smartExecutor2.getCoreSize() > 0);
        SmartExecutor smartExecutor3 = new SmartExecutor(2,2, SchedulePolicy.FirstInFirstRun, OverloadPolicy.DiscardNewTaskInQueue);
        assert(smartExecutor3.getCoreSize() > 0);
    }
    @Test
    public void testSmartExecutorDebugMode()
    {
        SmartExecutor smartExecutor = new SmartExecutor();
        smartExecutor.setDebug(true);

        smartExecutor.execute(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        smartExecutor.awaitAll();
    }
}
