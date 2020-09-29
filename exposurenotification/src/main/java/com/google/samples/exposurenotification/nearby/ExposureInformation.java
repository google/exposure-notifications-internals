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
import com.google.samples.exposurenotification.safeparcel.SafeParcelable;

import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import static com.google.common.base.Preconditions.checkArgument;


/**
 * Information about an exposure, meaning a single diagnosis key over a contiguous period of time
 * specified by durationMinutes.
 *
 * <p>The client can get the exposure information via {@link
 * ExposureNotificationClient#getExposureInformation}.
 *
 * @deprecated no longer used with Exposure Window API.
 */
@SafeParcelable.Class(creator = "ExposureInformationCreator")
@Deprecated
public final class ExposureInformation extends AbstractSafeParcelable {

    @Field(id = 1, getter = "getDateMillisSinceEpoch")
    long dateMillisSinceEpoch;

    @Field(id = 2, getter = "getDurationMinutes")
    int durationMinutes;

    @Field(id = 3, getter = "getAttenuationValue")
    int attenuationValue;

    @Field(id = 4, getter = "getTransmissionRiskLevel")
    @RiskLevel
    int transmissionRiskLevel;

    @Field(id = 5, getter = "getTotalRiskScore")
    int totalRiskScore;

    @Field(id = 6, getter = "getAttenuationDurationsInMinutes")
    int[] attenuationDurations;

    @Constructor
    ExposureInformation(
            @Param(id = 1) long dateMillisSinceEpoch,
            @Param(id = 2) int durationMinutes,
            @Param(id = 3) int attenuationValue,
            @Param(id = 4) int transmissionRiskLevel,
            @Param(id = 5) int totalRiskScore,
            @Param(id = 6) int[] attenuationDurations) {
        this.dateMillisSinceEpoch = dateMillisSinceEpoch;
        this.durationMinutes = durationMinutes;
        this.attenuationValue = attenuationValue;
        this.transmissionRiskLevel = transmissionRiskLevel;
        this.totalRiskScore = totalRiskScore;
        this.attenuationDurations = attenuationDurations;
    }

    /** Day-level resolution that the exposure occurred, in milliseconds since epoch. */
    public long getDateMillisSinceEpoch() {
        return dateMillisSinceEpoch;
    }

    /** Length of exposure in 5 minute increments, with a 30 minute maximum. */
    public int getDurationMinutes() {
        return durationMinutes;
    }

    /**
     * The time-weighted signal strength attenuation value which goes into {@link #getTotalRiskScore}.
     * Value will be between 0 and 255.
     */
    public int getAttenuationValue() {
        return attenuationValue;
    }

    /** The transmission risk associated with the matched diagnosis key. */
    @RiskLevel
    public int getTransmissionRiskLevel() {
        return transmissionRiskLevel;
    }

    /**
     * The total risk calculated for the exposure. See {@link ExposureConfiguration} for more
     * information about what is represented by the risk score, it will be a value 0-4096.
     */
    public int getTotalRiskScore() {
        return totalRiskScore;
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
     */
    public int[] getAttenuationDurationsInMinutes() {
        return Arrays.copyOf(attenuationDurations, attenuationDurations.length);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof ExposureInformation) {
            ExposureInformation that = (ExposureInformation) obj;
            return Objects.equal(dateMillisSinceEpoch, that.getDateMillisSinceEpoch())
                    && Objects.equal(durationMinutes, that.getDurationMinutes())
                    && Objects.equal(attenuationValue, that.getAttenuationValue())
                    && Objects.equal(transmissionRiskLevel, that.getTransmissionRiskLevel())
                    && Objects.equal(totalRiskScore, that.getTotalRiskScore())
                    && Arrays.equals(attenuationDurations, that.getAttenuationDurationsInMinutes());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
                dateMillisSinceEpoch,
                durationMinutes,
                attenuationValue,
                transmissionRiskLevel,
                totalRiskScore,
                attenuationDurations);
    }

    @Override
    public String toString() {
        return String.format(
                Locale.US,
                "ExposureInformation<"
                        + "date: %s, "
                        + "dateMillisSinceEpoch: %d, "
                        + "durationMinutes: %d, "
                        + "attenuationValue: %d, "
                        + "transmissionRiskLevel: %d, "
                        + "totalRiskScore: %d, "
                        + "attenuationDurations: %s"
                        + ">",
                new Date(dateMillisSinceEpoch),
                dateMillisSinceEpoch,
                durationMinutes,
                attenuationValue,
                transmissionRiskLevel,
                totalRiskScore,
                Arrays.toString(attenuationDurations));
    }

    /** A builder for {@link ExposureInformation}. */
    public static final class ExposureInformationBuilder {

        private long dateMillisSinceEpoch = 0;
        private int durationMinutes = 0;
        private int attenuationValue = 0;
        @RiskLevel private int transmissionRiskLevel = RiskLevel.RISK_LEVEL_INVALID;
        private int totalRiskScore = 0;
        private int[] attenuationDurations = new int[] {0, 0};

        public ExposureInformationBuilder setDateMillisSinceEpoch(long dateMillisSinceEpoch) {
            // This check only allows dates >= 1970-01-01, which is fairly arbitrary. Maybe we want to
            // make these bounds a bit tighter?
            checkArgument(
                    dateMillisSinceEpoch >= 0,
                    "dateMillisSinceEpoch (%s) must be >= 0",
                    dateMillisSinceEpoch);
            this.dateMillisSinceEpoch = dateMillisSinceEpoch;
            return this;
        }

        public ExposureInformationBuilder setDurationMinutes(int durationMinutes) {
            checkArgument(
                    durationMinutes % 5 == 0,
                    "durationMinutes (%s) must be an increment of 5",
                    durationMinutes);
            checkArgument(durationMinutes <= 30, "durationMinutes (%s) must be <= 30", durationMinutes);
            this.durationMinutes = durationMinutes;
            return this;
        }

        public ExposureInformationBuilder setAttenuationValue(int attenuationValue) {
            checkArgument(
                    attenuationValue >= 0 && attenuationValue <= 255,
                    "attenuationValue (%s) must be >= 0 and <= 255",
                    attenuationValue);
            this.attenuationValue = attenuationValue;
            return this;
        }

        public ExposureInformationBuilder setTransmissionRiskLevel(
                @RiskLevel int transmissionRiskLevel) {
            checkArgument(
                    transmissionRiskLevel >= 0 && transmissionRiskLevel <= 8,
                    "transmissionRiskLevel (%s) must be >= 0 and <= 8",
                    transmissionRiskLevel);
            this.transmissionRiskLevel = transmissionRiskLevel;
            return this;
        }

        public ExposureInformationBuilder setTotalRiskScore(int totalRiskScore) {
            checkArgument(
                    totalRiskScore >= 0 && totalRiskScore <= 4096,
                    "totalRiskScore (%s) must be >= 0 and <= 4096",
                    totalRiskScore);
            this.totalRiskScore = totalRiskScore;
            return this;
        }

        public ExposureInformationBuilder setAttenuationDurations(int[] attenuationDurations) {
            for (int attenuationDuration : attenuationDurations) {
                checkArgument(
                        attenuationDuration >= 0, "attenuationDuration (%s) must be >= 0", attenuationDuration);
            }
            this.attenuationDurations = Arrays.copyOf(attenuationDurations, attenuationDurations.length);
            return this;
        }

        public ExposureInformation build() {
            return new ExposureInformation(
                    dateMillisSinceEpoch,
                    durationMinutes,
                    attenuationValue,
                    transmissionRiskLevel,
                    totalRiskScore,
                    attenuationDurations);
        }
    }
}