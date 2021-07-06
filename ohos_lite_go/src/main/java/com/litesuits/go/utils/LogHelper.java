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

package com.litesuits.go.utils;

import ohos.hiviewdfx.HiLog;
import ohos.hiviewdfx.HiLogLabel;

/**
 * This is a convenience class that encapsulates the OHOS info and error logging functions,
 * but only accesses the {@link HiLog} library or create a {@link HiLogLabel} object when explicitly called for.
 * Logging can be toggled externally using setEnabled. By disabling, logger-dependent code can run in pure JRE for tests
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

    /**
     * Send an info log, with the given string format and arguments.
     *
     * @param messageFormat Indicates a constant format string, including the parameter type and privacy identifier.
     *                     If no privacy identifier is added, the parameter is considered to be
     *                      <strong>private</strong>.
     * @param args Indicates the parameter list corresponding to the parameter type in the format string. The number and
     *            type of parameters must be mapped onto the identifier in the format string.
     */
    public void logInfo(String messageFormat, Object... args) {
        if (enabled) {
            HiLog.info(getOrCreateLogLabel(), messageFormat, args);
        }
    }

    /**
     * Send an error log, with the given string format and arguments.
     *
     * @param messageFormat Indicates a constant format string, including the parameter type and privacy identifier.
     *                     If no privacy identifier is added, the parameter is considered to be
     *                      <strong>private</strong>.
     * @param args Indicates the parameter list corresponding to the parameter type in the format string. The number and
     *            type of parameters must be mapped onto the identifier in the format string.
     */
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

    /**
     * Set this {@link LogHelper} object to be able to or unable to write logs.
     *
     * @param enabled If {@code true}, the {@code logInfo} and {@code logError} methods will perform logging. If
     *                false, these functions will have no effect.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns whether or not this object is allowed to write logs.
     *
     * @return Whether or not this object is allowed to write logs.
     */
    public boolean isEnabled() {
        return enabled;
    }
}
