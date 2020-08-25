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

package com.google.samples.exposurenotification.ble.data;

import com.google.samples.Clock.DefaultClock;
import com.google.samples.exposurenotification.TemporaryExposureKey;
import com.google.samples.exposurenotification.features.ContactTracingFeature;

import org.joda.time.Duration;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * A utility class for methods used to convert time interval numbers to time and vice versa.
 */
public class TimeIntervalNumberUtility {
    /**
     * Converts the current time to a time interval number. Using DefaultClock for testability
     */
    public static int getCurrentIntervalNumber() {
        return (int)
                (DefaultClock.getInstance().currentTimeMillis()
                        / MINUTES.toMillis(ContactTracingFeature.idRollingPeriodMinutes()));
    }

    /**
     * Converts the passed in time since epoch into a time interval number, based on the feature flag
     * values that are provided.
     */
    public static int getTimeIntervalNumber(Duration timeSinceEpoch) {
        return (int)
                (timeSinceEpoch.getStandardMinutes() / ContactTracingFeature.idRollingPeriodMinutes());
    }

    /**
     * Gets the latest {@link TemporaryExposureKey#getRollingStartIntervalNumber()}} from before the
     * provided time.
     */
    public static int getLatestStartIntervalNumber(Duration timeSinceEpoch) {
        int numKeysSinceEpoch =
                (int)
                        (numIntervalsPer(timeSinceEpoch)
                                / ContactTracingFeature.tkRollingPeriodMultipleOfIdRollingPeriod());
        return numKeysSinceEpoch
                * (int) ContactTracingFeature.tkRollingPeriodMultipleOfIdRollingPeriod();
    }

    /**
     * Returns the number of intervals passed wtihin a given {@code duration}.
     */
    public static int numIntervalsPer(Duration duration) {
        return (int) (duration.getStandardMinutes() / ContactTracingFeature.idRollingPeriodMinutes());
    }

    /**
     * Calculates milliseconds since epoch from {@code intervalNumber}.
     */
    public static long getMillisSinceEpoch(int intervalNumber) {
        return Duration.standardMinutes(intervalNumber * ContactTracingFeature.idRollingPeriodMinutes())
                .getMillis();
    }

    private TimeIntervalNumberUtility() {
    }
}