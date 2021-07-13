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
