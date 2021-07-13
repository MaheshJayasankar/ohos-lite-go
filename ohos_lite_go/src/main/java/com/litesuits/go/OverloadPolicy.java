/*
 * Copyright 2016 litesuits.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.litesuits.go;

/**
 * Enumerates the different Overload Policies available to handle the case when the waiting queue of tasks is full and
 * a new task is added for a {@link SmartExecutor} object.
 *
 * @author MaTianyu
 * @date 2015-04-23
 */
public enum OverloadPolicy {
    /**
     * Discard the latest added task to the queue, to make space for the new task.
     */
    DISCARD_NEW_TASK_IN_QUEUE,
    /**
     * Discard the oldest added task to the queue, to make space for the new task.
     */
    DISCARD_OLD_TASK_IN_QUEUE,
    /**
     * Discard the task to be added, and leave the queue unchanged.
     */
    DISCARD_CURRENT_TASK,
    /**
     * Run the task to be added directly on the current thread.
     */
    CALLER_RUNS,
    /**
     * Throws a TaskOverloadException which extends RuntimeException if the queue is attempted to be overloaded.
     */
    THROW_EXCEPTION
}
