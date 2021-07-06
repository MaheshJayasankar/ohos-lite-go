package com.litesuits.go;

import com.litesuits.go.utils.GoUtil;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GoUtilTest {
    @BeforeClass
    public static void setUpTestEnvironment()
    {
        GoUtil.setTestingEnv(true);
        int coresCount = GoUtil.getCoresCount();
        assertTrue(coresCount > 0);
    }

    @Test
    public void testGoUtil(){
        SmartExecutor smartExecutor = new SmartExecutor();
        int goUtilCoresCount = GoUtil.getCoresCount();
        int smartExecutorCoreSize = smartExecutor.getCoreSize();
        assertTrue(goUtilCoresCount > 0);
        assertEquals(goUtilCoresCount, smartExecutorCoreSize);
    }
}
