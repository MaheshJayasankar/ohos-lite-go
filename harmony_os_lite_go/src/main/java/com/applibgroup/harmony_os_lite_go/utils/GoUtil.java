package com.applibgroup.harmony_os_lite_go.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

import static ohos.os.ProcessManager.getAvailableCores;

/**
 * @author MaTianyu
 * @date 2015-04-21
 */
public class GoUtil {
    private static int CPU_CORES = 0;

    public static String formatDate(long millis) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(new Date(millis));
    }

    /**
     * Get available processors.
     */
    public static int getProcessorsCount() {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * Gets the number of cores available in this device, across all processors.
     *
     * @return The number of cores, or available processors if result couldn't be obtained
     */
    public static int getCoresCount() {
        if (CPU_CORES > 0) {
            return CPU_CORES;
        }
        int coreCount = getAvailableCores().length;
        if (coreCount <= 0)
        {
            return getProcessorsCount();
        }
        CPU_CORES = coreCount;
        return CPU_CORES;
    }
}
