/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.exposurenotification.ble.interfaces;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Provides executor for BLE scanning and advertising.
 */
public interface BleExecutors {
    /**
     * Schedules a {@code command} with specific delay.
     *
     * @return the canceller of this scheduled task
     */
    ScheduledTask schedule(Runnable command, long delay, TimeUnit unit);

    /**
     * Schedules a {@code command} with specific delay with {@link ScheduledExecutorService}.
     *
     * @return the canceller of this scheduled task
     */
    ScheduledTask scheduleWithScheduledExecutor(Runnable command, long delay, TimeUnit unit);

    /**
     * Schedules a {@code command} with specific delay. OpportunisticSchedule will not be executed
     * exactly on time, but will be checked everytime when {@link BleExecutors#schedule(Runnable,
     * long, TimeUnit)} called or its scheduled task fired. If current time is larger than target time
     * when checking, the opportunisticSchedule will be executed.
     */
    ScheduledTask opportunisticSchedule(Runnable runnable, long delay, TimeUnit unit);

    void start();

    void stop();

    /**
     * Allows scheduled runnable to be cancelled, and also provides status check for cancellation.
     */
    interface ScheduledTask {
        boolean isCancelable();

        boolean cancel();
    }
}