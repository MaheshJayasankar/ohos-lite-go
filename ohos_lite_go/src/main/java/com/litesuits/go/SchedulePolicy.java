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
 * Enumerates the different Schedule Policies available to decide which task is to be executed
 * next out of the waiting queue for a {@link SmartExecutor} object.
 *
 * @author MaTianyu
 * @date 2015-04-23
 */
public enum SchedulePolicy {
    /**
     * The last task to be added to the waiting queue will be run first.
     */
    LAST_IN_FIRST_RUN,
    /**
     * The first task to be added to the waiting queue will be run first.
     */
    FIRST_IN_FIRST_RUN
}
