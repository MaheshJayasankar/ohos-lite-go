# ohos-lite-go
LiteGo is a Java-based asynchronous concurrency library. It has a smart executor, which can be freely set the maximum number of concurrent tasks, and the number of threads in waiting queue. It can also set waiting policies and overload strategies. LiteGo can directly utilise Runnable, Callable, FutureTask and other types of implementations to run a task. Its core component is the "SmartExecutor", which can be used as the sole component in the Application that supports asynchronous concurrency. There can be multiple instances of SmartExecutor in an Application, and each instance has complete "independence", in the sense of independent core concurrency, queuing and waiting indicators, independent task scheduling and waiting list overloading strategy. However, all instances share a thread pool. This mechanism not only meets the independent needs of different modules for thread control and task scheduling, but also shares a pool resource to save overhead, saves resources and reuses threads to the greatest extent, and helps improve performance.

# Source
Inspired by the Android library android-lite-go (v1.0) by [litesuits](http://litesuits.com):

https://github.com/litesuits/android-lite-go

## Features

>  The number of core concurrent threads can be defined, that is, the number of concurrent requests at the same time.
>
> The number of threads waiting to be queued can be defined, that is, the number of requests that can be queued after exceeding the number of concurrent cores.
>
> The strategy for waiting for the queue to enter the execution state can be defined: first come first, execute first, then execute first.

You can define a strategy for processing new requests after the waiting queue is full:

- Discard the latest task in the queue
- Discard the oldest task in the queue
- Discard the current new task
- Direct execution (blocking the current thread)
- Throw an exception (interrupt the current thread)

## Dependency
****How to add the dependency

## Usage
initialization:

```java
// put in 4 tasks at once
for (int i = 0; i <4; i++) {
     final int j = i;
     smallExecutor.execute(new Runnable() {
         @Override
         public void run() {
             Hilog.info(LABEL, "TASK" + j + "is running now ----------->");
             Thread.sleep(j * 200);
         }
     });
}

// Put in another task that may need to be cancelled
Future future = smallExecutor.submit(new Runnable() {
     @Override
     public void run() {
         Hilog.info(LABEL, "TASK 4 will be canceled... ------------>");
         Thread.sleep(1000);
     }
});

// Cancel this task at the right time
future.cancel(false);
```



The above code is designed to be able to concurrently "2" threads at the same time. After the concurrency is fully loaded, the waiting queue can accommodate "2" threads. The late tasks in the queue are executed first, and the new tasks will be discarded when the waiting queue is full. The oldest task.

Test the situation of multiple threads concurrency:

```java
// put in 4 tasks at once
for (int i = 0; i <4; i++) {
    final int j = i;
    smallExecutor.execute(new Runnable() {
        @Override
        public void run() {
            HttpLog.i(TAG, "TASK" + j + "is running now ----------->");
            SystemClock.sleep(j * 200);
        }
    });
}

// Put in another task that may need to be cancelled
Future future = smallExecutor.submit(new Runnable() {
    @Override
    public void run() {
        HttpLog.i(TAG, "TASK 4 will be canceled... ------------>");
        SystemClock.sleep(1000);
    }
});

// Cancel this task at the right time
future.cancel(false);
```



In the above code, five tasks of 0, 1, 2, 3, 4 are inserted in sequence at a time. Note that task 4 is the last to be inserted and returns a Future object.

According to the settings, 0 and 1 will be executed immediately. While they are being executed, 2 and 3 will enter the queue. This results in the queue being full, at which point the independently input task 4 will enter. Based on the current overload policy, the oldest task 2 in the queue will be removed, and the queue will consist of only 3 and 4.

Because 4 was subsequently cancelled, it will not completely execute. The final output:

```java
TASK 0 is running now ----------->
TASK 1 is running now ----------->
TASK 3 is running now ----------->
```
