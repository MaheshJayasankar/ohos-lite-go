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
