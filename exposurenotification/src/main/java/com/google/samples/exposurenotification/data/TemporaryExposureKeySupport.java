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

package com.google.samples.exposurenotification.data;

import com.google.samples.exposurenotification.TemporaryExposureKey;
import com.google.samples.exposurenotification.features.ContactTracingFeature;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * A utility class providing helper functions for {@link TemporaryExposureKey}.
 */
public final class TemporaryExposureKeySupport {
    private static final byte[] MIN_VALUE = {
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };
    private static final byte[] MAX_VALUE = {
            (byte) 0xFF,
            (byte) 0xFF,
            (byte) 0xFF,
            (byte) 0xFF,
            (byte) 0xFF,
            (byte) 0xFF,
            (byte) 0xFF,
            (byte) 0xFF,
            (byte) 0xFF,
            (byte) 0xFF,
            (byte) 0xFF,
            (byte) 0xFF,
            (byte) 0xFF,
            (byte) 0xFF,
            (byte) 0xFF,
            (byte) 0xFF
    };

    private TemporaryExposureKeySupport() {
    }

    public static DayNumber getDayNumber(TemporaryExposureKey temporaryExposureKey) {
        return new DayNumber(
                (int)
                        (temporaryExposureKey.getRollingStartIntervalNumber()
                                / ContactTracingFeature.tkRollingPeriodMultipleOfIdRollingPeriod()));
    }

    /**
     * Gets the time which a key is released from embargo. This time is the key's expiration date (ie,
     * start period + number of rolling periods valid for) + an additional number of rolling periods
     * to protect against clock drift.
     */
    public static long getEmbargoEndTimeInMillis(TemporaryExposureKey temporaryExposureKey) {
        long endIntervalNumber =
                temporaryExposureKey.getRollingStartIntervalNumber()
                        + temporaryExposureKey.getRollingPeriod()
                        + ContactTracingFeature.tkMatchingClockDriftRollingPeriods();
        return endIntervalNumber * MINUTES.toMillis(ContactTracingFeature.idRollingPeriodMinutes());
    }

    public static int getRollingStartIntervalNumber(DayNumber dayNumber) {
        return getRollingStartIntervalNumber(dayNumber.getValue());
    }

    public static int getRollingStartIntervalNumber(int dayNumber) {
        return (int) (dayNumber * ContactTracingFeature.tkRollingPeriodMultipleOfIdRollingPeriod());
    }

    /**
     * Get the rolling end interval number (exclusive). In another word, this number is the first
     * interval number that does not belong to this {@link TemporaryExposureKey} following the {@link
     * TemporaryExposureKey#getRollingStartIntervalNumber()}.
     */
    public static int getRollingEndIntervalNumber(TemporaryExposureKey temporaryExposureKey) {
        return temporaryExposureKey.getRollingStartIntervalNumber()
                + temporaryExposureKey.getRollingPeriod();
    }

    /**
     * Get the end time of the validation window of {@code temporaryExposureKey} without considering
     * embargo. See {@link #getEmbargoEndTimeInMillis(TemporaryExposureKey)} for embargo end time.
     */
    public static long getExpirationTimeMillis(TemporaryExposureKey temporaryExposureKey) {
        return getRollingEndIntervalNumber(temporaryExposureKey)
                * MINUTES.toMillis(ContactTracingFeature.idRollingPeriodMinutes());
    }

    public static int getKeySizeBytes() {
        return (int) ContactTracingFeature.temporaryTracingKeySizeBytes();
    }

    public static TemporaryExposureKey getMinKey(DayNumber dayNumber) {
        return new TemporaryExposureKey.TemporaryExposureKeyBuilder()
                .setRollingStartIntervalNumber(getRollingStartIntervalNumber(dayNumber))
                .setKeyData(MIN_VALUE)
                .build();
    }

    public static TemporaryExposureKey getMaxKey(DayNumber dayNumber) {
        return new TemporaryExposureKey.TemporaryExposureKeyBuilder()
                .setRollingStartIntervalNumber(getRollingStartIntervalNumber(dayNumber))
                .setKeyData(MAX_VALUE)
                .build();
    }
}