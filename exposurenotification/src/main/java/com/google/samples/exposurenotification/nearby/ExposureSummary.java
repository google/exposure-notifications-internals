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

package com.google.samples.exposurenotification.nearby;

import androidx.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.samples.exposurenotification.safeparcel.AbstractSafeParcelable;
import com.google.samples.exposurenotification.safeparcel.ReflectedParcelable;
import com.google.samples.exposurenotification.safeparcel.SafeParcelable;

import java.util.Arrays;
import java.util.Locale;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Summary information about recent exposures.
 *
 * <p>The client can get this information via {@link ExposureNotificationClient#getExposureSummary}.
 *
 * @deprecated no longer used with Exposure Window API.
 */
@SafeParcelable.Class(creator = "ExposureSummaryCreator")
@Deprecated
public final class ExposureSummary extends AbstractSafeParcelable implements ReflectedParcelable {

    @Field(id = 1, getter = "getDaysSinceLastExposure")
    int daysSinceLastExposure;

    @Field(id = 2, getter = "getMatchedKeyCount")
    int matchedKeyCount;

    @Field(id = 3, getter = "getMaximumRiskScore")
    int maximumRiskScore;

    @Field(id = 4, getter = "getAttenuationDurationsInMinutes")
    int[] attenuationDurations;

    @Field(id = 5, getter = "getSummationRiskScore")
    int summationRiskScore;

    @Constructor
    ExposureSummary(
            @Param(id = 1) int daysSinceLastExposure,
            @Param(id = 2) int matchedKeyCount,
            @Param(id = 3) int maximumRiskScore,
            @Param(id = 4) int[] attenuationDurations,
            @Param(id = 5) int summationRiskScore) {
        this.daysSinceLastExposure = daysSinceLastExposure;
        this.matchedKeyCount = matchedKeyCount;
        this.maximumRiskScore = maximumRiskScore;
        this.attenuationDurations = attenuationDurations;
        this.summationRiskScore = summationRiskScore;
    }

    /**
     * Days since last match to a diagnosis key from the server. 0 is today, 1 is yesterday, etc. Only
     * valid if {@link #getMatchedKeyCount} > 0.
     */
    public int getDaysSinceLastExposure() {
        return daysSinceLastExposure;
    }

    /** Number of matched diagnosis keys. */
    public int getMatchedKeyCount() {
        return matchedKeyCount;
    }

    /** The highest risk score of all exposure incidents, it will be a value 0-4096. */
    public int getMaximumRiskScore() {
        return maximumRiskScore;
    }

    /**
     * Array of durations in minutes at certain radio signal attenuations. The thresholds are defined
     * in {@link ExposureConfiguration#getDurationAtAttenuationThresholds}.
     *
     * <p>Array index 0: Sum of durations for all exposures when attenuation < low threshold.
     *
     * <p>Array index 1: Sum of durations for all exposures when low threshold <= attenuation < high
     * threshold.
     *
     * <p>Array index 2: Sum of durations for all exposures when attenuation >= high threshold.
     *
     * <p>These durations are aggregated across all exposures and capped at 30 minutes.
     */
    public int[] getAttenuationDurationsInMinutes() {
        return Arrays.copyOf(attenuationDurations, attenuationDurations.length);
    }

    /** The summation of risk scores of all exposure incidents. */
    public int getSummationRiskScore() {
        return summationRiskScore;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof ExposureSummary) {
            ExposureSummary that = (ExposureSummary) obj;
            return Objects.equal(daysSinceLastExposure, that.getDaysSinceLastExposure())
                    && Objects.equal(matchedKeyCount, that.getMatchedKeyCount())
                    && Objects.equal(maximumRiskScore, that.getMaximumRiskScore())
                    && Arrays.equals(attenuationDurations, that.getAttenuationDurationsInMinutes())
                    && Objects.equal(summationRiskScore, that.getSummationRiskScore());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
                daysSinceLastExposure,
                matchedKeyCount,
                maximumRiskScore,
                attenuationDurations,
                summationRiskScore);
    }

    @Override
    public String toString() {
        return String.format(
                Locale.US,
                "ExposureSummary<"
                        + "daysSinceLastExposure: %d, "
                        + "matchedKeyCount: %d, "
                        + "maximumRiskScore: %d, "
                        + "attenuationDurations: %s, "
                        + "summationRiskScore: %d"
                        + ">",
                daysSinceLastExposure,
                matchedKeyCount,
                maximumRiskScore,
                Arrays.toString(attenuationDurations),
                summationRiskScore);
    }

    /** A builder for {@link ExposureSummary}. */
    public static final class ExposureSummaryBuilder {

        private int daysSinceLastExposure = 0;
        private int matchedKeyCount = 0;
        private int maximumRiskScore = 0;
        private int[] attenuationDurations = new int[] {0, 0, 0};
        private int summationRiskScore = 0;

        public ExposureSummaryBuilder setDaysSinceLastExposure(int daysSinceLastExposure) {
            checkArgument(
                    daysSinceLastExposure >= 0,
                    "daysSinceLastExposure (%s) must be >= 0",
                    daysSinceLastExposure);
            this.daysSinceLastExposure = daysSinceLastExposure;
            return this;
        }

        public ExposureSummaryBuilder setMatchedKeyCount(int matchedKeyCount) {
            checkArgument(matchedKeyCount >= 0, "matchedKeyCount (%s) must be >= 0", matchedKeyCount);
            this.matchedKeyCount = matchedKeyCount;
            return this;
        }

        public ExposureSummaryBuilder setMaximumRiskScore(int maximumRiskScore) {
            checkArgument(
                    maximumRiskScore >= 0 && maximumRiskScore <= 4096,
                    "maximumRiskScore (%s) must be >= 0 and <= 4096",
                    maximumRiskScore);
            this.maximumRiskScore = maximumRiskScore;
            return this;
        }

        public ExposureSummaryBuilder setAttenuationDurations(int[] attenuationDurations) {
            checkArgument(attenuationDurations.length == 3);
            for (int attenuationDuration : attenuationDurations) {
                checkArgument(
                        attenuationDuration >= 0, "attenuationDuration (%s) must be >= 0", attenuationDuration);
            }
            this.attenuationDurations = Arrays.copyOf(attenuationDurations, attenuationDurations.length);
            return this;
        }

        public ExposureSummaryBuilder setSummationRiskScore(int summationRiskScore) {
            checkArgument(
                    summationRiskScore >= 0, "summationRiskScore (%s) must be >= 0", summationRiskScore);
            this.summationRiskScore = summationRiskScore;
            return this;
        }

        public ExposureSummary build() {
            return new ExposureSummary(
                    daysSinceLastExposure,
                    matchedKeyCount,
                    maximumRiskScore,
                    attenuationDurations,
                    summationRiskScore);
        }
    }
}