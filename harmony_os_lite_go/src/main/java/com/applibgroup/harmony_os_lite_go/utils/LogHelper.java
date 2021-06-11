package com.applibgroup.harmony_os_lite_go.utils;

import ohos.hiviewdfx.HiLog;
import ohos.hiviewdfx.HiLogLabel;

/**
 * Encapsulates the HiLog.info and HiLog.error functions,
 * but doesn't use HiLog or HiLogLabel until explicitly called for,
 * so that logging may be disabled in Java environments.
 */
public class LogHelper {

    private final int DOMAIN;
    private final String TAG;
    private HiLogLabel logLabel;

    public LogHelper(int domain, String tag) {
        DOMAIN = domain;
        TAG = tag;
    }

    public void logInfo(String messageFormat, Object... args){
        HiLogLabel logLabel = getOrCreateLogLabel();
        HiLog.info(logLabel, messageFormat, args);
    }

    public void logError(String messageFormat, Object... args){
        HiLogLabel logLabel = getOrCreateLogLabel();
        HiLog.error(logLabel, messageFormat, args);
    }

    private HiLogLabel getOrCreateLogLabel(){
        if (logLabel == null)
        {
            int TYPE = HiLog.LOG_APP;
            logLabel = new HiLogLabel(TYPE, DOMAIN, TAG);
        }
        return logLabel;
    }

}
