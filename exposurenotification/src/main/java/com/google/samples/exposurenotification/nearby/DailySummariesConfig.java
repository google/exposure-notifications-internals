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

import androidx.annotation.FloatRange;
import androidx.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.samples.exposurenotification.ExposureNotificationEnums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Configuration of per-day summary of exposures.
 *
 * <p>During summarization the following are computed for each {@link ExposureWindow}s:
 *
 * <ul>
 *   <li>a weighted duration, computed as
 *       <p><code>
 *          ( immediateDurationSeconds * immediateDurationWeight ) +
 *          ( nearDurationSeconds      * nearDurationWeight ) +
 *          ( mediumDurationSeconds    * mediumDurationWeight ) +
 *          ( otherDurationSeconds     * otherDurationWeight )
 *       </code>
 *   <li>a score, computed as
 *       <p><code>
 *          reportTypeWeights[Tek.reportType] *
 *          infectiousnessWeights[infectiousness] *
 *          weightedDuration
 *       </code>
 *       <p>where infectiousness and reportType are set based on the ExposureWindow's diagnosis key
 *       and the DiagnosisKeysDataMapping
 * </ul>
 *
 * <p>The {@link ExposureWindow}s are then filtered, removing those with score lower than {@link
 * #getMinimumWindowScore()}.
 *
 * <p>Scores and weighted durations of the {@link ExposureWindow}s that pass the {@link
 * #getMinimumWindowScore()} are then aggregated over a day to compute the maximum and cumulative
 * scores and duration:
 *
 * <ul>
 *   <li>sumScore = sum(score of ExposureWindows)
 *   <li>maxScore = max(score of ExposureWindows)
 *   <li>weightedDurationSum = sum(weighted duration of ExposureWindow)
 * </ul>
 *
 * <p>Note that when the weights are typically around 100% (1.0), both the scores and the
 * weightedDurationSum can be considered as being expressed in seconds. For example, 15 minutes of
 * exposure with all weights equal to 1.0 would be 60 * 15 = 900 (seconds).
 */
public class DailySummariesConfig {

    List<Double> reportTypeWeights;
    List<Double> infectiousnessWeights;
    List<Integer> attenuationBucketThresholdDb;
    List<Double> attenuationBucketWeights;
    int daysSinceExposureThreshold;
    double minimumWindowScore;

    DailySummariesConfig(
            List<Double> reportTypeWeights,
            List<Double> infectiousnessWeights,
            List<Integer> attenuationBucketThresholdDb,
            List<Double> attenuationBucketWeights,
            int daysSinceExposureThreshold,
            double minimumWindowScore) {
        this.reportTypeWeights = reportTypeWeights;
        this.infectiousnessWeights = infectiousnessWeights;
        this.attenuationBucketThresholdDb = attenuationBucketThresholdDb;
        this.attenuationBucketWeights = attenuationBucketWeights;
        this.daysSinceExposureThreshold = daysSinceExposureThreshold;
        this.minimumWindowScore = minimumWindowScore;
    }

    /**
     * Scoring weights to associate with exposures with different {@link ReportType}s.
     *
     * <p>This map can include weights for the following {@link ReportType}s:
     *
     * <ul>
     *   <li>CONFIRMED_TEST
     *   <li>CONFIRMED_CLINICAL_DIAGNOSIS
     *   <li>SELF_REPORT
     *   <li>RECURSIVE (reserved for future use)
     * </ul>
     * <p>
     * Each element must be between 0 and 2.5.
     */
    public Map<Integer, Double> getReportTypeWeights() {
        HashMap<Integer, Double> result = new HashMap<>();
        for (int i = 0; i < reportTypeWeights.size(); i++) {
            result.put(i, reportTypeWeights.get(i));
        }
        return result;
    }

    /**
     * Scoring weights to associate with exposures with different {@link Infectiousness}.
     *
     * <p>This map can include weights for the following {@link Infectiousness} values:
     *
     * <ul>
     *   <li>STANDARD
     *   <li>HIGH
     * </ul>
     * <p>
     * Each element must be between 0 and 2.5.
     */
    public Map<Integer, Double> getInfectiousnessWeights() {
        HashMap<Integer, Double> result = new HashMap<>();
        for (int i = 0; i < infectiousnessWeights.size(); i++) {
            result.put(i, infectiousnessWeights.get(i));
        }
        return result;
    }

    /**
     * Thresholds defining the BLE attenuation buckets edges.
     *
     * <p>This list must have 3 elements: the immediate, near, and medium thresholds. See
     * attenuationBucketWeights for more information.
     *
     * <p>These elements must be between 0 and 255 and come in increasing order.
     */
    public List<Integer> getAttenuationBucketThresholdDb() {
        return new ArrayList<>(attenuationBucketThresholdDb);
    }

    /**
     * Scoring weights to associate with ScanInstances depending on the attenuation bucket in which
     * their typicalAttenuationDb falls.
     *
     * <p>This list must have 4 elements, corresponding to the weights for the 4 buckets.
     *
     * <ul>
     *   <li>immediate bucket: -infinity < attenuation <= immediate threshold
     *   <li>near bucket: immediate threshold < attenuation <= near threshold
     *   <li>medium bucket: near threshold < attenuation <= medium threshold
     *   <li>other bucket: medium threshold < attenuation < +infinity
     * </ul>
     * <p>
     * Each element must be between 0 and 2.5.
     */
    public List<Double> getAttenuationBucketWeights() {
        return new ArrayList<>(attenuationBucketWeights);
    }

    /**
     * Day summaries older than this are not returned.
     *
     * <p>To return all available day summaries, set to 0, which is treated specially.
     */
    public int getDaysSinceExposureThreshold() {
        return daysSinceExposureThreshold;
    }

    /**
     * Minimum score that ExposureWindows must reach in order to be included in the {@link
     * DailySummary.ExposureSummaryData}.
     *
     * <p>Use 0 to consider all {@link ExposureWindow}s (recommended).
     */
    public double getMinimumWindowScore() {
        return minimumWindowScore;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof DailySummariesConfig) {
            DailySummariesConfig that = (DailySummariesConfig) obj;
            return reportTypeWeights.equals(that.reportTypeWeights)
                    && infectiousnessWeights.equals(that.infectiousnessWeights)
                    && attenuationBucketThresholdDb.equals(that.attenuationBucketThresholdDb)
                    && attenuationBucketWeights.equals(that.attenuationBucketWeights)
                    && daysSinceExposureThreshold == that.daysSinceExposureThreshold
                    && minimumWindowScore == that.minimumWindowScore;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
                reportTypeWeights,
                infectiousnessWeights,
                attenuationBucketThresholdDb,
                attenuationBucketWeights,
                daysSinceExposureThreshold,
                minimumWindowScore);
    }

    @Override
    public String toString() {
        return String.format(
                Locale.US,
                "DailySummariesConfig<"
                        + "reportTypeWeights: %s, "
                        + "infectiousnessWeights: %s, "
                        + "attenuationBucketThresholdDb: %s, "
                        + "attenuationBucketWeights: %s"
                        + "daysSinceExposureThreshold: %d,"
                        + "minimumWindowScore: %.3f>",
                reportTypeWeights,
                infectiousnessWeights,
                attenuationBucketThresholdDb,
                attenuationBucketWeights,
                daysSinceExposureThreshold,
                minimumWindowScore);
    }

    /**
     * Definition of attenuation bucket group.
     *
     * @hide
     */
    public enum DistanceEstimate {
        IMMEDIATE,
        NEAR,
        MEDIUM,
        OTHER;

        public static DistanceEstimate getDistanceEstimate(
                int typicalAttenuation, DailySummariesConfig config) {
            if (typicalAttenuation <= config.getAttenuationBucketThresholdDb().get(0)) {
                return IMMEDIATE;
            } else if (typicalAttenuation <= config.getAttenuationBucketThresholdDb().get(1)) {
                return NEAR;
            } else if (typicalAttenuation <= config.getAttenuationBucketThresholdDb().get(2)) {
                return MEDIUM;
            } else {
                return OTHER;
            }
        }
    }

    /**
     * A builder for {@link DailySummariesConfig}.
     */
    public static final class DailySummariesConfigBuilder {
        private Double[] reportTypeWeights = new Double[6]; // 6 = number of ReportType values.
        private Double[] infectiousnessWeights =
                new Double[ExposureNotificationEnums.Infectiousness.values().length];
        private List<Integer> attenuationBucketThresholdDb;
        private List<Double> attenuationBucketWeights;
        int daysSinceExposureThreshold = 0;
        double minimumWindowScore = 0;

        public DailySummariesConfigBuilder() {
            Arrays.fill(reportTypeWeights, 0.0);
            Arrays.fill(infectiousnessWeights, 0.0);
        }

        /**
         * See {@link DailySummariesConfig#getReportTypeWeights()}.
         */
        public DailySummariesConfigBuilder setReportTypeWeight(
                @ReportType int reportType, @FloatRange(from = 0.0, to = 2.5) double weight) {
            checkArgument(
                    reportType >= ReportType.UNKNOWN && reportType <= ReportType.REVOKED,
                    "Incorrect value of reportType");
            checkSingleWeightRange(weight, "reportTypeWeights");
            this.reportTypeWeights[reportType] = weight;
            return this;
        }

        /**
         * See {@link DailySummariesConfig#getInfectiousnessWeights()}
         */
        public DailySummariesConfigBuilder setInfectiousnessWeight(
                @Infectiousness int infectiousness, @FloatRange(from = 0.0, to = 2.5) double weight) {
            checkArgument(
                    ExposureNotificationEnums.Infectiousness.forNumber(infectiousness) != null,
                    "Incorrect value of infectiousness");
            checkSingleWeightRange(weight, "infectiousnessWeights");
            this.infectiousnessWeights[infectiousness] = weight;
            return this;
        }

        /**
         * See {@link DailySummariesConfig#getAttenuationBucketThresholdDb()} and {@link
         * DailySummariesConfig#getAttenuationBucketWeights()} ()}
         */
        public DailySummariesConfigBuilder setAttenuationBuckets(
                List<Integer> thresholds, List<Double> weights) {
            setAttenuationBucketThreshold(thresholds);
            setAttenuationBucketWeights(weights);
            return this;
        }

        private void setAttenuationBucketThreshold(List<Integer> attenuationBucketThresholdDb) {
            checkListSize(
                    attenuationBucketThresholdDb,
                    DistanceEstimate.values().length - 1,
                    "attenuationBucketThresholdDb");
            for (int i = 0; i < attenuationBucketThresholdDb.size(); i++) {
                checkArgument(
                        attenuationBucketThresholdDb.get(i) >= 0 && attenuationBucketThresholdDb.get(i) <= 255,
                        "Element of attenuationBucketThreshold must between 0 ~ 255");
                if (i == 0) {
                    continue;
                }
                checkArgument(
                        attenuationBucketThresholdDb.get(i - 1) < attenuationBucketThresholdDb.get(i),
                        String.format(
                                Locale.ENGLISH,
                                "attenuationBucketThresholdDb of index %d must be larger than index %d",
                                i,
                                i - 1));
            }
            this.attenuationBucketThresholdDb = new ArrayList<>(attenuationBucketThresholdDb);
        }

        private void setAttenuationBucketWeights(List<Double> attenuationBucketWeights) {
            checkListSize(
                    attenuationBucketWeights, DistanceEstimate.values().length, "attenuationBucketWeights");
            checkWeightRange(attenuationBucketWeights, "attenuationBucketWeights");
            this.attenuationBucketWeights = new ArrayList<>(attenuationBucketWeights);
        }

        /**
         * See {@link DailySummariesConfig#getDaysSinceExposureThreshold()}
         */
        public DailySummariesConfigBuilder setDaysSinceExposureThreshold(
                int daysSinceExposureThreshold) {
            checkArgument(
                    daysSinceExposureThreshold >= 0, "daysSinceExposureThreshold must not be negative");
            this.daysSinceExposureThreshold = daysSinceExposureThreshold;
            return this;
        }

        /**
         * See {@link DailySummariesConfig#getMinimumWindowScore()}
         */
        public DailySummariesConfigBuilder setMinimumWindowScore(double minimumWindowScore) {
            checkArgument(minimumWindowScore >= 0, "minimumWindowScore must not be negative");
            this.minimumWindowScore = minimumWindowScore;
            return this;
        }

        public DailySummariesConfig build() {
            checkArgument(attenuationBucketThresholdDb != null, "Must set attenuationBucketThresholdDb");
            checkArgument(attenuationBucketWeights != null, "Must set attenuationBucketWeights");
            return new DailySummariesConfig(
                    Arrays.asList(reportTypeWeights),
                    Arrays.asList(infectiousnessWeights),
                    attenuationBucketThresholdDb,
                    attenuationBucketWeights,
                    daysSinceExposureThreshold,
                    minimumWindowScore);
        }

        @SuppressWarnings("rawtypes")
        private static void checkListSize(List list, int expectSize, String name) {
            checkArgument(list != null, String.format(Locale.ENGLISH, "%s must not be null", name));
            checkArgument(
                    list.size() == expectSize,
                    String.format(Locale.ENGLISH, "%s must must contains %d elements", name, expectSize));
        }

        private static void checkWeightRange(List<Double> values, String name) {
            for (double value : values) {
                checkSingleWeightRange(value, name);
            }
        }

        private static void checkSingleWeightRange(double value, String name) {
            checkArgument(
                    value >= 0 && value <= 2.5,
                    String.format(Locale.ENGLISH, "Element value of %s must between 0 ~ 2.5", name));
        }
    }
}