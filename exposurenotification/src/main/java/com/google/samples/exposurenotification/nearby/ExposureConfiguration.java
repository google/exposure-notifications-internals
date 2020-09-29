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
import java.util.Locale;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Exposure configuration parameters that can be provided when initializing the service.
 *
 * <p>These parameters are used to calculate risk for each exposure incident using the following
 * formula:
 *
 * <p><code>
 *   RiskScore = attenuationScore
 *       * daysSinceLastExposureScore
 *       * durationScore
 *       * transmissionRiskScore
 * </code>
 *
 * <p>Scores are in the range [0-8]. Weights are in the range [0-100].
 *
 * @deprecated no longer used with Exposure Windows API.
 */
@SafeParcelable.Class(creator = "ExposureConfigurationCreator")
@Deprecated
public final class ExposureConfiguration extends AbstractSafeParcelable {

    @Field(id = 1, getter = "getMinimumRiskScore")
    int minimumRiskScore;

    @Field(id = 2, getter = "getAttenuationScores")
    int[] attenuationScores;

    @Field(id = 3, getter = "getAttenuationWeight")
    int attenuationWeight;

    @Field(id = 4, getter = "getDaysSinceLastExposureScores")
    int[] daysSinceLastExposureScores;

    @Field(id = 5, getter = "getDaysSinceLastExposureWeight")
    int daysSinceLastExposureWeight;

    @Field(id = 6, getter = "getDurationScores")
    int[] durationScores;

    @Field(id = 7, getter = "getDurationWeight")
    int durationWeight;

    @Field(id = 8, getter = "getTransmissionRiskScores")
    int[] transmissionRiskScores;

    @Field(id = 9, getter = "getTransmissionRiskWeight")
    int transmissionRiskWeight;

    @Field(id = 10, getter = "getDurationAtAttenuationThresholds")
    int[] durationAtAttenuationThresholds;

    @Constructor
    ExposureConfiguration(
            @Param(id = 1) int minimumRiskScore,
            @Param(id = 2) int[] attenuationScores,
            @Param(id = 3) int attenuationWeight,
            @Param(id = 4) int[] daysSinceLastExposureScores,
            @Param(id = 5) int daysSinceLastExposureWeight,
            @Param(id = 6) int[] durationScores,
            @Param(id = 7) int durationWeight,
            @Param(id = 8) int[] transmissionRiskScores,
            @Param(id = 9) int transmissionRiskWeight,
            @Param(id = 10) int[] durationAtAttenuationThresholds) {
        this.minimumRiskScore = minimumRiskScore;
        this.attenuationScores = attenuationScores;
        this.attenuationWeight = attenuationWeight;
        this.daysSinceLastExposureScores = daysSinceLastExposureScores;
        this.daysSinceLastExposureWeight = daysSinceLastExposureWeight;
        this.durationScores = durationScores;
        this.durationWeight = durationWeight;
        this.transmissionRiskScores = transmissionRiskScores;
        this.transmissionRiskWeight = transmissionRiskWeight;
        this.durationAtAttenuationThresholds = durationAtAttenuationThresholds;
    }

    /**
     * Minimum risk score. Excludes exposure incidents with scores lower than this from calculation of
     * {@link ExposureSummary#getMaximumRiskScore}, {@link ExposureSummary#getSummationRiskScore}, and
     * {@link ExposureInformation#getTotalRiskScore}. Other returned fields are unaffected by this
     * setting.
     *
     * <p>Defaults to no minimum.
     */
    public int getMinimumRiskScore() {
        return minimumRiskScore;
    }

    /**
     * Scores for attenuation buckets. Must contain 8 scores, one for each bucket as defined below:
     *
     * <p><code>{@code
     * attenuationScores[0] when Attenuation > 73
     * attenuationScores[1] when 73 >= Attenuation > 63
     * attenuationScores[2] when 63 >= Attenuation > 51
     * attenuationScores[3] when 51 >= Attenuation > 33
     * attenuationScores[4] when 33 >= Attenuation > 27
     * attenuationScores[5] when 27 >= Attenuation > 15
     * attenuationScores[6] when 15 >= Attenuation > 10
     * attenuationScores[7] when 10 >= Attenuation
     * }</code>
     */
    public int[] getAttenuationScores() {
        return Arrays.copyOf(attenuationScores, attenuationScores.length);
    }

    /**
     * Weight to apply to the attenuation score. Must be in the range 0-100.
     *
     * <p>Reserved for future use.
     */
    public int getAttenuationWeight() {
        return attenuationWeight;
    }

    /**
     * Scores for days since last exposure buckets. Must contain 8 scores, one for each bucket as
     * defined below:
     *
     * <p><code>{@code
     * daysSinceLastExposureScores[0] when Days >= 14
     * daysSinceLastExposureScores[1] when Days >= 12
     * daysSinceLastExposureScores[2] when Days >= 10
     * daysSinceLastExposureScores[3] when Days >= 8
     * daysSinceLastExposureScores[4] when Days >= 6
     * daysSinceLastExposureScores[5] when Days >= 4
     * daysSinceLastExposureScores[6] when Days >= 2
     * daysSinceLastExposureScores[7] when Days >= 0
     * }</code>
     */
    public int[] getDaysSinceLastExposureScores() {
        return Arrays.copyOf(daysSinceLastExposureScores, daysSinceLastExposureScores.length);
    }

    /**
     * Weight to apply to the days since last exposure score. Must be in the range 0-100.
     *
     * <p>Reserved for future use.
     */
    public int getDaysSinceLastExposureWeight() {
        return daysSinceLastExposureWeight;
    }

    /**
     * Scores for duration buckets. Must contain 8 scores, one for each bucket as defined below:
     *
     * <p><code>{@code
     * durationScores[0] when Duration == 0
     * durationScores[1] when Duration <= 5
     * durationScores[2] when Duration <= 10
     * durationScores[3] when Duration <= 15
     * durationScores[4] when Duration <= 20
     * durationScores[5] when Duration <= 25
     * durationScores[6] when Duration <= 30
     * durationScores[7] when Duration  > 30
     * }</code>
     */
    public int[] getDurationScores() {
        return Arrays.copyOf(durationScores, durationScores.length);
    }

    /**
     * Weight to apply to the duration score. Must be in the range 0-100.
     *
     * <p>Reserved for future use.
     */
    public int getDurationWeight() {
        return durationWeight;
    }

    /**
     * Scores for transmission risk buckets. Must contain 8 scores, one for each bucket as defined
     * below:
     *
     * <p><code>{@code
     * transmissionRiskScores[0] when RISK_SCORE_LOWEST
     * transmissionRiskScores[1] when RISK_SCORE_LOW
     * transmissionRiskScores[2] when RISK_SCORE_LOW_MEDIUM
     * transmissionRiskScores[3] when RISK_SCORE_MEDIUM
     * transmissionRiskScores[4] when RISK_SCORE_MEDIUM_HIGH
     * transmissionRiskScores[5] when RISK_SCORE_HIGH
     * transmissionRiskScores[6] when RISK_SCORE_VERY_HIGH
     * transmissionRiskScores[7] when RISK_SCORE_HIGHEST
     * }</code>
     */
    public int[] getTransmissionRiskScores() {
        return Arrays.copyOf(transmissionRiskScores, transmissionRiskScores.length);
    }

    /**
     * Weight to apply to the transmission risk score. Must be in the range 0-100.
     *
     * <p>Reserved for future use.
     */
    public int getTransmissionRiskWeight() {
        return transmissionRiskWeight;
    }

    /**
     * Attenuation thresholds to apply when calculating duration at attenuation. Must contain two
     * thresholds, each in range of 0 - 255. durationAtAttenuationThresholds[0] has to be <=
     * durationAtAttenuationThresholds[1]. These are used used to populate {@link
     * ExposureSummary#getAttenuationDurationsInMinutes} and {@link
     * ExposureInformation#getAttenuationDurationsInMinutes}.
     */
    public int[] getDurationAtAttenuationThresholds() {
        return Arrays.copyOf(durationAtAttenuationThresholds, durationAtAttenuationThresholds.length);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof ExposureConfiguration) {
            ExposureConfiguration that = (ExposureConfiguration) obj;
            return Objects.equal(minimumRiskScore, that.getMinimumRiskScore())
                    && Arrays.equals(attenuationScores, that.getAttenuationScores())
                    && Objects.equal(attenuationWeight, that.getAttenuationWeight())
                    && Arrays.equals(daysSinceLastExposureScores, that.getDaysSinceLastExposureScores())
                    && Objects.equal(daysSinceLastExposureWeight, that.getDaysSinceLastExposureWeight())
                    && Arrays.equals(durationScores, that.getDurationScores())
                    && Objects.equal(durationWeight, that.getDurationWeight())
                    && Arrays.equals(transmissionRiskScores, that.getTransmissionRiskScores())
                    && Objects.equal(transmissionRiskWeight, that.getTransmissionRiskWeight())
                    && Arrays.equals(
                    durationAtAttenuationThresholds, that.getDurationAtAttenuationThresholds());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
                minimumRiskScore,
                attenuationScores,
                attenuationWeight,
                daysSinceLastExposureScores,
                daysSinceLastExposureWeight,
                durationScores,
                durationWeight,
                transmissionRiskScores,
                transmissionRiskWeight,
                durationAtAttenuationThresholds);
    }

    @Override
    public String toString() {
        return String.format(
                Locale.US,
                "ExposureConfiguration<"
                        + "minimumRiskScore: %d, "
                        + "attenuationScores: %s, "
                        + "attenuationWeight: %d, "
                        + "daysSinceLastExposureScores: %s, "
                        + "daysSinceLastExposureWeight: %d, "
                        + "durationScores: %s, "
                        + "durationWeight: %d, "
                        + "transmissionRiskScores: %s, "
                        + "transmissionRiskWeight: %d, "
                        + "durationAtAttenuationThresholds: %s"
                        + ">",
                minimumRiskScore,
                Arrays.toString(attenuationScores),
                attenuationWeight,
                Arrays.toString(daysSinceLastExposureScores),
                daysSinceLastExposureWeight,
                Arrays.toString(durationScores),
                durationWeight,
                Arrays.toString(transmissionRiskScores),
                transmissionRiskWeight,
                Arrays.toString(durationAtAttenuationThresholds));
    }

    /** A builder for {@link ExposureConfiguration}. */
    public static final class ExposureConfigurationBuilder {

        private int minimumRiskScore = 4;
        private int[] attenuationScores = new int[] {4, 4, 4, 4, 4, 4, 4, 4};
        private int attenuationWeight = 50;
        private int[] daysSinceLastExposureScores = new int[] {4, 4, 4, 4, 4, 4, 4, 4};
        private int daysSinceLastExposureWeight = 50;
        private int[] durationScores = new int[] {4, 4, 4, 4, 4, 4, 4, 4};
        private int durationWeight = 50;
        private int[] transmissionRiskScores = new int[] {4, 4, 4, 4, 4, 4, 4, 4};
        private int transmissionRiskWeight = 50;
        private int[] durationAtAttenuationThresholds = new int[] {50, 74};

        public ExposureConfigurationBuilder setMinimumRiskScore(int minimumRiskScore) {
            checkArgument(
                    minimumRiskScore >= 1 && minimumRiskScore <= 4096,
                    "minimumRiskScore (%s) must be >= 1 and <= 4096",
                    minimumRiskScore);
            this.minimumRiskScore = minimumRiskScore;
            return this;
        }

        public ExposureConfigurationBuilder setAttenuationScores(int... attenuationScores) {
            checkArgument(
                    attenuationScores.length == 8,
                    "attenuationScores (%s) must have exactly 8 elements",
                    Arrays.toString(attenuationScores));
            for (int attenuationScore : attenuationScores) {
                checkArgument(
                        attenuationScore >= 0 && attenuationScore <= 8,
                        "attenuationScore (%s) must be >= 0 and <= 8",
                        attenuationScore);
            }
            this.attenuationScores = Arrays.copyOf(attenuationScores, attenuationScores.length);
            return this;
        }

        public ExposureConfigurationBuilder setAttenuationWeight(int attenuationWeight) {
            checkArgument(
                    attenuationWeight >= 0 && attenuationWeight <= 100,
                    "attenuationWeight (%s) must be >= 0 and <= 100",
                    attenuationWeight);
            this.attenuationWeight = attenuationWeight;
            return this;
        }

        public ExposureConfigurationBuilder setDaysSinceLastExposureScores(
                int... daysSinceLastExposureScores) {
            checkArgument(
                    daysSinceLastExposureScores.length == 8,
                    "daysSinceLastExposureScores (%s) must have exactly 8 elements",
                    Arrays.toString(daysSinceLastExposureScores));
            for (int daysSinceLastExposureScore : daysSinceLastExposureScores) {
                checkArgument(
                        daysSinceLastExposureScore >= 0 && daysSinceLastExposureScore <= 8,
                        "daysSinceLastExposureScore (%s) must be >= 0 and <= 8",
                        daysSinceLastExposureScore);
            }
            this.daysSinceLastExposureScores =
                    Arrays.copyOf(daysSinceLastExposureScores, daysSinceLastExposureScores.length);
            return this;
        }

        public ExposureConfigurationBuilder setDaysSinceLastExposureWeight(
                int daysSinceLastExposureWeight) {
            checkArgument(
                    daysSinceLastExposureWeight >= 0 && daysSinceLastExposureWeight <= 100,
                    "daysSinceLastExposureWeight (%s) must be >= 0 and <= 100",
                    daysSinceLastExposureWeight);
            this.daysSinceLastExposureWeight = daysSinceLastExposureWeight;
            return this;
        }

        public ExposureConfigurationBuilder setDurationScores(int... durationScores) {
            checkArgument(
                    durationScores.length == 8,
                    "durationScores (%s) must have exactly 8 elements",
                    Arrays.toString(durationScores));
            for (int durationScore : durationScores) {
                checkArgument(
                        durationScore >= 0 && durationScore <= 8,
                        "durationScore (%s) must be >= 0 and <= 8",
                        durationScore);
            }
            this.durationScores = Arrays.copyOf(durationScores, durationScores.length);
            return this;
        }

        public ExposureConfigurationBuilder setDurationWeight(int durationWeight) {
            checkArgument(
                    durationWeight >= 0 && durationWeight <= 100,
                    "durationWeight (%s) must be >= 0 and <= 100",
                    durationWeight);
            this.durationWeight = durationWeight;
            return this;
        }

        public ExposureConfigurationBuilder setTransmissionRiskScores(int... transmissionRiskScores) {
            checkArgument(
                    transmissionRiskScores.length == 8,
                    "transmissionRiskScores (%s) must have exactly 8 elements",
                    Arrays.toString(transmissionRiskScores));
            for (int transmissionRiskScore : transmissionRiskScores) {
                checkArgument(
                        transmissionRiskScore >= 0 && transmissionRiskScore <= 8,
                        "transmissionRiskScore (%s) must be >= 0 and <= 8",
                        transmissionRiskScore);
            }
            this.transmissionRiskScores =
                    Arrays.copyOf(transmissionRiskScores, transmissionRiskScores.length);
            return this;
        }

        public ExposureConfigurationBuilder setTransmissionRiskWeight(int transmissionRiskWeight) {
            checkArgument(
                    transmissionRiskWeight >= 0 && transmissionRiskWeight <= 100,
                    "transmissionRiskWeight (%s) must be >= 0 and <= 100",
                    transmissionRiskWeight);
            this.transmissionRiskWeight = transmissionRiskWeight;
            return this;
        }

        public ExposureConfigurationBuilder setDurationAtAttenuationThresholds(
                int... durationAtAttenuationThresholds) {
            checkArgument(
                    durationAtAttenuationThresholds.length == 2,
                    "durationAtAttenuationThresholds (%s) must have exactly 2 elements",
                    Arrays.toString(durationAtAttenuationThresholds));
            for (int durationAtAttenuationThreshold : durationAtAttenuationThresholds) {
                checkArgument(
                        durationAtAttenuationThreshold >= 0 && durationAtAttenuationThreshold <= 255,
                        "durationAtAttenuationThreshold (%s) must be >= 0 and <= 255",
                        durationAtAttenuationThreshold);
            }
            checkArgument(
                    durationAtAttenuationThresholds[0] <= durationAtAttenuationThresholds[1],
                    "durationAtAttenuationThresholds[0] (%s) must be <= than"
                            + " durationAtAttenuationThresholds[1] (%s)",
                    durationAtAttenuationThresholds[0],
                    durationAtAttenuationThresholds[1]);
            this.durationAtAttenuationThresholds = durationAtAttenuationThresholds;
            return this;
        }

        public ExposureConfiguration build() {
            return new ExposureConfiguration(
                    minimumRiskScore,
                    attenuationScores,
                    attenuationWeight,
                    daysSinceLastExposureScores,
                    daysSinceLastExposureWeight,
                    durationScores,
                    durationWeight,
                    transmissionRiskScores,
                    transmissionRiskWeight,
                    durationAtAttenuationThresholds);
        }
    }
}