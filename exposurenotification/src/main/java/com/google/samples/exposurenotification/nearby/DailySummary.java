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
import com.google.samples.exposurenotification.nearby.DailySummary.ExposureSummaryData.ExposureSummaryDataBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Daily exposure summary to pass to client side.
 */
public class DailySummary {

    int daysSinceEpoch;
    List<ExposureSummaryData> reportSummaries;
    ExposureSummaryData summaryData;

    DailySummary(
            int daysSinceEpoch,
            List<ExposureSummaryData> reportSummaries,
            ExposureSummaryData summaryData) {
        this.daysSinceEpoch = daysSinceEpoch;
        this.reportSummaries = reportSummaries;
        this.summaryData = summaryData;
    }

    /**
     * Returns days since epoch of the {@link ExposureWindow}s that went into this summary.
     */
    public int getDaysSinceEpoch() {
        return daysSinceEpoch;
    }

    /**
     * Summary of all exposures on this day of a specific diagnosis {@link ReportType}.
     */
    public ExposureSummaryData getSummaryDataForReportType(@ReportType int reportType) {
        return reportSummaries.get(reportType);
    }

    /**
     * Summary of all exposures on this day.
     */
    public ExposureSummaryData getSummaryData() {
        return summaryData;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof DailySummary) {
            DailySummary that = (DailySummary) obj;
            return daysSinceEpoch == that.daysSinceEpoch
                    && reportSummaries.equals(that.reportSummaries)
                    && Objects.equal(summaryData, that.summaryData);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(daysSinceEpoch, reportSummaries, summaryData);
    }

    @Override
    public String toString() {
        return String.format(
                Locale.US,
                "DailySummary<" + "daysSinceEpoch: %d, " + "reportSummaries: %s, " + "daySummary: %s>",
                daysSinceEpoch,
                reportSummaries,
                summaryData);
    }

    /**
     * A builder for {@link DailySummary}.
     *
     * @hide
     */
    public static final class DailySummaryBuilder {
        private int daysSinceEpoch = 0;
        private ExposureSummaryData[] reportSummaries =
                new ExposureSummaryData[6]; // 6 = number of possible ReportTypes.
        private ExposureSummaryData summaryData;

        public DailySummaryBuilder() {
            Arrays.fill(reportSummaries, new ExposureSummaryDataBuilder().build());
        }

        public DailySummaryBuilder setDaysSinceEpoch(int daysSinceEpoch) {
            this.daysSinceEpoch = daysSinceEpoch;
            return this;
        }

        public DailySummaryBuilder setReportSummary(
                @ReportType int reportType, ExposureSummaryData reportTypeSummaryData) {
            this.reportSummaries[reportType] = reportTypeSummaryData;
            return this;
        }

        public DailySummaryBuilder setSummaryData(ExposureSummaryData summaryData) {
            this.summaryData = summaryData;
            return this;
        }

        public DailySummary build() {
            return new DailySummary(daysSinceEpoch, Arrays.asList(reportSummaries), summaryData);
        }
    }

    /**
     * Stores different scores for specific {@link ReportType}.
     */
    public static class ExposureSummaryData {

        double maximumScore;
        double scoreSum;
        double weightedDurationSum;

        ExposureSummaryData(
                double maximumScore,
                double scoreSum,
                double weightedDurationSum) {
            this.maximumScore = maximumScore;
            this.scoreSum = scoreSum;
            this.weightedDurationSum = weightedDurationSum;
        }

        /**
         * Highest score of all {@link ExposureWindow}s aggregated into this summary.
         *
         * <p>See {@link DailySummariesConfig} for more information about how the per-ExposureWindow
         * score is computed.
         */
        public double getMaximumScore() {
            return maximumScore;
        }

        /**
         * Sum of scores for all {@link ExposureWindow}s aggregated into this summary.
         *
         * <p>See {@link DailySummariesConfig} for more information about how the per-ExposureWindow
         * score is computed.
         */
        public double getScoreSum() {
            return scoreSum;
        }

        /**
         * Sum of weighted durations for all {@link ExposureWindow}s aggregated into this summary.
         *
         * <p>See {@link DailySummariesConfig} for more information about how the per-{@link
         * ExposureWindow} score and weightedDurationSum are computed.
         */
        public double getWeightedDurationSum() {
            return weightedDurationSum;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof ExposureSummaryData) {
                ExposureSummaryData that = (ExposureSummaryData) obj;
                return maximumScore == that.maximumScore
                        && scoreSum == that.scoreSum
                        && weightedDurationSum == that.weightedDurationSum;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(maximumScore, scoreSum, weightedDurationSum);
        }

        @Override
        public String toString() {
            return String.format(
                    Locale.US,
                    "ExposureSummaryData<"
                            + "maximumScore: %.3f, "
                            + "scoreSum: %.3f, "
                            + "weightedDurationSum: %.3f>",
                    maximumScore,
                    scoreSum,
                    weightedDurationSum);
        }

        /**
         * A builder for {@link ExposureSummaryData}. @hide
         */
        public static final class ExposureSummaryDataBuilder {
            private double maximumScore = 0;
            private double scoreSum = 0;
            private double weightedDurationSum = 0;

            public ExposureSummaryDataBuilder setMaximumScore(double maximumScore) {
                this.maximumScore = maximumScore;
                return this;
            }

            public ExposureSummaryDataBuilder setScoreSum(double scoreSum) {
                this.scoreSum = scoreSum;
                return this;
            }

            public ExposureSummaryDataBuilder setWeightedDurationSum(double weightedDurationSum) {
                this.weightedDurationSum = weightedDurationSum;
                return this;
            }

            public ExposureSummaryData build() {
                return new ExposureSummaryData(maximumScore, scoreSum, weightedDurationSum);
            }
        }
    }
}