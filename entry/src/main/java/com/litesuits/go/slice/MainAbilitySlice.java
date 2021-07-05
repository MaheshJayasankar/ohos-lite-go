package com.litesuits.go.slice;

import com.litesuits.go.ListItemProvider;
import com.litesuits.go.OverloadPolicy;
import com.litesuits.go.ResourceTable;
import com.litesuits.go.SchedulePolicy;
import com.litesuits.go.SmartExecutor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.agp.components.ListContainer;
import ohos.hiviewdfx.HiLog;
import ohos.hiviewdfx.HiLogLabel;

public class MainAbilitySlice extends AbilitySlice {

    // OHOS Log parameters
    private static final String TAG = MainAbilitySlice.class.getSimpleName();
    private static final int DOMAIN = 0x00101;
    private final HiLogLabel infoLabel = new HiLogLabel(HiLog.LOG_APP, DOMAIN, TAG);
    private static final int LIST_ITEM_COUNT = 4;
    protected SmartExecutor mainExecutor;

    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);
        super.setUIContent(ResourceTable.Layout_ability_main);
        initViews();
        initSmartExecutor();
    }

    @Override
    public void onActive() {
        super.onActive();
    }

    @Override
    public void onForeground(Intent intent) {
        super.onForeground(intent);
    }

    private void initViews() {
        ListContainer listContainer = (ListContainer) findComponentById(ResourceTable.Id_list_container);
        List<Integer> list = getData();
        ListItemProvider listItemProvider = new ListItemProvider(list, this);
        listContainer.setItemProvider(listItemProvider);

        listContainer.setItemClickedListener((listContainer1, component, i, l) -> clickTestItem(i));
    }

    private List<Integer> getData() {
        List<Integer> list = new ArrayList<>();
        for (int idx = 0; idx < LIST_ITEM_COUNT; idx++) {
            list.add(idx);
        }
        return list;
    }

    private void initSmartExecutor() {
        if (mainExecutor == null) {
            // set this temporary parameter, just for test
            // Intelligent Concurrent Scheduling Controller: Set the [Maximum Concurrency Number]
            // and [Waiting Queue] size for testing purposes only, according to actual scenarios
            mainExecutor = new SmartExecutor();

            // To turn on Debugging and Logging features. It is recommended to turn off in production.
            mainExecutor.setLoggerEnabled(true);

            // number of concurrent threads at the same time, recommended core size is CPU count
            mainExecutor.setCoreSize(2);

            // adjust maximum number of waiting queue size by yourself or based on device performance
            mainExecutor.setQueueSize(100);

            // If number of tasks exceeds number of cores,
            // decide how to deal with the tasks when they exit the waiting queue
            mainExecutor.setSchedulePolicy(SchedulePolicy.LastInFirstRun);

            // If number of tasks exceed the length of the waiting queue, decide to discard oldest task in queue
            mainExecutor.setOverloadPolicy(OverloadPolicy.DiscardOldTaskInQueue);
            HiLog.info(infoLabel, "Smart Executor has been Initialized.");
        }
    }

    /**
     * <item>0. Submit Runnable</item>
     * <item>1. Submit FutureTask</item>
     * <item>2. Submit Callable</item>
     * <item>3. Strategy Test</item>
     */
    private void clickTestItem(final int which) {
        switch (which) {
            case 0:
                HiLog.info(infoLabel, "Trying to submit runnable.");
                // 0. Submit Runnable
                mainExecutor.submit(() -> {
                    HiLog.info(infoLabel, " Runnable start!  thread id: %{public}d",
                            Thread.currentThread().getId());
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    HiLog.info(infoLabel, " Runnable end!  thread id: %{public}d",
                            Thread.currentThread().getId());
                });
                break;

            case 1:
                // 1. Submit FutureTask
                FutureTask<String> futureTask = new FutureTask<>(() -> {
                    HiLog.info(infoLabel, " FutureTask thread id: %{public}d",
                            Thread.currentThread().getId());
                    return "FutureTask";
                });
                mainExecutor.submit(futureTask);
                break;

            case 2:
                // 2. Submit Callable
                mainExecutor.submit(() -> {
                    HiLog.info(infoLabel, " Callable thread id: %{public}d",
                            Thread.currentThread().getId());
                    return "Callable";
                });

                break;

            case 3:
                // 3. Strategy Test

                // set this temporary parameter, just for test
                SmartExecutor smallExecutor = new SmartExecutor();


                // number of concurrent threads at the same time, recommended core size is CPU count
                smallExecutor.setCoreSize(2);

                // adjust maximum number of waiting queue size by yourself or based on phone performance
                smallExecutor.setQueueSize(2);

                // After the number of tasks exceeds [Maximum Concurrent Number], it will
                // automatically enter the [Waiting Queue], wait for the completion of the current
                // execution task and enter the execution state according to the strategy: last-in first
                smallExecutor.setSchedulePolicy(SchedulePolicy.LastInFirstRun);

                // When the number of new tasks added subsequently exceeds the size of the
                // [waiting queue], the overload strategy is executed: the oldest task in the queue is discarded.
                smallExecutor.setOverloadPolicy(OverloadPolicy.DiscardOldTaskInQueue);

                smallExecutor.setLoggerEnabled(true);

                // put in 4 tasks at once
                for (int i = 0; i < 4; i++) {
                    final int j = i;
                    smallExecutor.execute(() -> {
                        HiLog.info(infoLabel, " TASK %{public}d is running now ----------->", j);
                        try {
                            Thread.sleep(j * 200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    });
                }

                // A new task is added to the list, but it is immediately canceled.
                Future<?> future = smallExecutor.submit(() -> {
                    HiLog.info(infoLabel, " TASK 4 should have been cancelled ... ------------>");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                boolean cancelResult = future.cancel(false);
                HiLog.info(infoLabel, "TASK 4 was cancelled successfully? %{public}s", cancelResult);
                break;
            default: break;
        }
    }

}
