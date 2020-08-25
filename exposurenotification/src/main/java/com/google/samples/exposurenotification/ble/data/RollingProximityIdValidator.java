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

import com.google.samples.exposurenotification.TemporaryExposureKey;
import com.google.samples.exposurenotification.data.GeneratedRollingProximityId;
import com.google.samples.exposurenotification.data.TemporaryExposureKeySupport;
import com.google.samples.exposurenotification.features.ContactTracingFeature;

import org.joda.time.Duration;

/**
 * Utility method to validate whether a sighting falls within a valid time window.
 *
 * <p>A valid sighting falls within {@link
 * ContactTracingFeature#tkMatchingClockDriftRollingPeriods()} of the start and end of a {@link
 * GeneratedRollingProximityId#intervalNumber()}
 */
public class RollingProximityIdValidator {

    /** Returns true if the sighting is within the expected time bounds. */
    public static boolean isSightingValid(
            GeneratedRollingProximityId generatedId,
            Duration sightingTimeSinceEpoch,
            TemporaryExposureKey diagnosisKey) {
        int sightingTimeIntervalNumber =
                TimeIntervalNumberUtility.getTimeIntervalNumber(sightingTimeSinceEpoch);
        return sightingTimeIntervalNumber >= getValidWindowStartIntervalNumber(generatedId)
                && sightingTimeIntervalNumber < getValidWindowEndIntervalNumber(generatedId, diagnosisKey);
    }

    /**
     * Gets the start of the valid time window based on {@link
     * GeneratedRollingProximityId#intervalNumber()}.
     */
    public static int getValidWindowStartIntervalNumber(GeneratedRollingProximityId generatedId) {
        return (int)
                (generatedId.intervalNumber() - ContactTracingFeature.tkMatchingClockDriftRollingPeriods());
    }

    /**
     * Gets the exclusive end of the valid time window based on {@link
     * GeneratedRollingProximityId#intervalNumber()}. Clock drift will only be considered if the
     * resulting validEndIntervalNumber falls within the provided {@code diagnosisKey}'s validity
     * period. If the computed validStartIntervalNumber results in an ending interval later than the
     * provided {@code diagnosisKey}, then the {@code diagnosisKey}'s end interval is returned
     * instead.
     */
    public static int getValidWindowEndIntervalNumber(
            GeneratedRollingProximityId generatedId, TemporaryExposureKey diagnosisKey) {
        int windowEndIntervalNumber =
                (int)
                        (generatedId.intervalNumber()
                                + ContactTracingFeature.tkMatchingClockDriftRollingPeriods()
                                + 1);
        int keyEndIntervalNumber =
                TemporaryExposureKeySupport.getRollingEndIntervalNumber(diagnosisKey);
        if (ContactTracingFeature.ignoreEmbargoPeriodWhenMatchingNearKeyEdges()
                && windowEndIntervalNumber > keyEndIntervalNumber) {
            windowEndIntervalNumber = keyEndIntervalNumber;
        }
        return windowEndIntervalNumber;
    }
    private RollingProximityIdValidator() {}
}