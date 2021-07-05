package com.litesuits.go.utils;

import ohos.hiviewdfx.HiLog;
import ohos.hiviewdfx.HiLogLabel;

/**
 * Encapsulates the HiLog.info and HiLog.error functions,
 * but doesn't use the HiLog library or a HiLogLabel object until explicitly called for,
 * so that by disabling logging facility, the code may run on pure Java environments.
 */
public class LogHelper {

    private final int logDomain;
    private final String logTag;
    private HiLogLabel logLabel;
    // By default, the logger is disabled. It must be enabled by the calling class.
    private boolean enabled = false;

    public LogHelper(int domain, String tag) {
        logDomain = domain;
        logTag = tag;
    }

    public void logInfo(String messageFormat, Object... args) {
        if (enabled) {
            HiLog.info(getOrCreateLogLabel(), messageFormat, args);
        }
    }

    public void logError(String messageFormat, Object... args) {
        if (enabled) {
            HiLog.error(getOrCreateLogLabel(), messageFormat, args);
        }
    }

    private HiLogLabel getOrCreateLogLabel() {
        if (logLabel == null) {
            int logType = HiLog.LOG_APP;
            logLabel = new HiLogLabel(logType, logDomain, logTag);
        }
        return logLabel;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
