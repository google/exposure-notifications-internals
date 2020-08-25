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

package com.google.samples.exposurenotification.matching;

import com.google.common.annotations.VisibleForTesting;
import com.google.samples.Clock;
import com.google.samples.Clock.DefaultClock;
import com.google.samples.exposurenotification.ExposureConfiguration;
import com.google.samples.exposurenotification.RiskLevel;
import com.google.samples.exposurenotification.TemporaryExposureKey;
import com.google.samples.exposurenotification.features.ContactTracingFeature;
import com.google.samples.exposurenotification.storage.ExposureRecord;

import java.util.List;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Used to calculate risk score based on client configuration and exposures.
 */
public class RiskScoreCalculator {
    private final Clock clock;

    public static RiskScoreCalculator create() {
        return new RiskScoreCalculator(DefaultClock.getInstance());
    }

    public static RiskScoreCalculator create(Clock clock) {
        return new RiskScoreCalculator(clock);
    }

    @VisibleForTesting
    RiskScoreCalculator(Clock clock) {
        this.clock = clock;
    }

    /**
     * Returns a V1 risk score. Returns 0 if calculated risk score is below {@link
     * ExposureConfiguration#getMinimumRiskScore()}. Throws IllegalArgumentException on invalid input.
     */
    public int calculateRiskScore(
            TemporaryExposureKey diagnosisKey,
            ExposureRecord exposureRecord,
            long dateMillisSinceEpoch,
            ExposureConfiguration configuration) {

        int attenuationScore =
                configuration
                        .getAttenuationScores()[bucketAttenuationValue(exposureRecord.getAttenuationValue())];
        int daysSinceLastExposureScore =
                configuration.getDaysSinceLastExposureScores()[bucketLatencyDays(dateMillisSinceEpoch)];
        int durationScore =
                configuration.getDurationScores()[bucketDuration(exposureRecord.getDurationSeconds())];
        int transmissionRiskScore =
                diagnosisKey.getTransmissionRiskLevel() == RiskLevel.RISK_LEVEL_INVALID
                        ? 1
                        : configuration.getTransmissionRiskScores()[bucketRiskLevel(diagnosisKey)];
        int riskScore =
                attenuationScore * daysSinceLastExposureScore * durationScore * transmissionRiskScore;

        if (riskScore >= configuration.getMinimumRiskScore()) {
            return riskScore;
        } else {
            return 0;
        }
    }

    public static int getAttenuationScore(
            int weightedAttenuation, ExposureConfiguration configuration) {
        return configuration.getAttenuationScores()[bucketAttenuationValue(weightedAttenuation)];
    }

    private int bucketRiskLevel(TemporaryExposureKey diagnosisKey) {
        // Enum of risk levels starts at 1, with 0 reserved for RISK_LEVEL_INVALID.
        return diagnosisKey.getTransmissionRiskLevel() - 1;
    }

    private static int bucketAttenuationValue(int attenuationValue) {
        List<Integer> bucketBounds =
                ContactTracingFeature.riskScoreAttenuationValueBuckets();
        for (int bucket = 0; bucket < bucketBounds.size(); bucket++) {
            if (attenuationValue > bucketBounds.get(bucket)) {
                return bucket;
            }
        }
        return bucketBounds.size();
    }

    private int bucketLatencyDays(long dateMillisSinceEpoch) {
        List<Integer> bucketBounds =
                ContactTracingFeature.riskScoreLatencyDaysBuckets();
        long timeNow = clock.currentTimeMillis();
        long latencyDays = MILLISECONDS.toDays(timeNow - dateMillisSinceEpoch);
        for (int bucket = 0; bucket < bucketBounds.size(); bucket++) {
            if (latencyDays >= bucketBounds.get(bucket)) {
                return bucket;
            }
        }
        return bucketBounds.size();
    }

    private int bucketDuration(long durationSeconds) {
        List<Integer> bucketBounds = ContactTracingFeature.riskScoreDurationBuckets();
        long durationMinutes = SECONDS.toMinutes(durationSeconds);
        for (int bucket = 0; bucket < bucketBounds.size(); bucket++) {
            if (durationMinutes <= bucketBounds.get(bucket)) {
                return bucket;
            }
        }
        return bucketBounds.size();
    }
}