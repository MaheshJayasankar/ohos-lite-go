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

import com.litesuits.go.utils.LogHelper;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LogHelperTest {
    @Test
    public void testSetEnabled() {
        LogHelper logHelper = new LogHelper(0, "TEST");
        logHelper.setEnabled(true);
        assertTrue(logHelper.isEnabled());
    }
    @Test
    public void testGetEnabled() {
        LogHelper logHelper = new LogHelper(0, "TEST");
        logHelper.setEnabled(false);
        logHelper.logInfo("Test Info Log. This will not be logged as the logger is disabled.");
        logHelper.logError("Test Error Log. This will not be logged as the logger is disabled.");
        assertFalse(logHelper.isEnabled());
    }
}
