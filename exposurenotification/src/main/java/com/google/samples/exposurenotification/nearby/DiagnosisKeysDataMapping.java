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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.samples.exposurenotification.ExposureNotificationEnums;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

/** Mappings from diagnosis keys data to concepts returned by the API. */
public class DiagnosisKeysDataMapping {
    /**
     * The maximum days since onset for checking the valid range of {@link
     * #getDaysSinceOnsetToInfectiousness}.
     *
     * @hide
     */
    public static final int MAXIMUM_OFFSET_OF_DAYS_SINCE_ONSET = 14;

    /**
     * The maximum size of {@link #getDaysSinceOnsetToInfectiousness}.
     *
     * @hide
     */
    public static final int SIZE_OF_DAYS_SINCE_ONSET_TO_INFECTIOUSNESS =
            MAXIMUM_OFFSET_OF_DAYS_SINCE_ONSET * 2 + 1;

    List<Integer> daysSinceOnsetToInfectiousness;

    @ReportType
    int reportTypeWhenMissing;

    @Infectiousness
    int infectiousnessWhenDaysSinceOnsetMissing;

    DiagnosisKeysDataMapping(
            List<Integer> daysSinceOnsetToInfectiousness,
            @ReportType int reportTypeWhenMissing,
            @Infectiousness int infectiousnessWhenDaysSinceOnsetMissing) {
        this.daysSinceOnsetToInfectiousness = daysSinceOnsetToInfectiousness;
        this.reportTypeWhenMissing = reportTypeWhenMissing;
        this.infectiousnessWhenDaysSinceOnsetMissing = infectiousnessWhenDaysSinceOnsetMissing;
    }

    /**
     * Mapping from diagnosisKey.daysSinceOnsetOfSymptoms to {@link Infectiousness}.
     *
     * <p>Infectiousness is computed from this mapping and the tek metadata as -
     * daysSinceOnsetToInfectiousness[{@link TemporaryExposureKey#getDaysSinceOnsetOfSymptoms}], or -
     * {@link #getInfectiousnessWhenDaysSinceOnsetMissing} if {@link
     * TemporaryExposureKey#getDaysSinceOnsetOfSymptoms} is {@link
     * TemporaryExposureKey#DAYS_SINCE_ONSET_OF_SYMPTOMS_UNKNOWN}.
     *
     * <p>Values of DaysSinceOnsetOfSymptoms that aren't represented in this map are given {@link
     * Infectiousness#NONE} as infectiousness. Exposures with infectiousness equal to {@link
     * Infectiousness#NONE} are dropped.
     */
    public Map<Integer, Integer> getDaysSinceOnsetToInfectiousness() {
        HashMap<Integer, Integer> map = Maps.newHashMapWithExpectedSize(29);
        for (int i = 0; i < daysSinceOnsetToInfectiousness.size(); i++) {
            map.put(i - 14, daysSinceOnsetToInfectiousness.get(i));
        }
        return map;
    }

    /**
     * Report type to default to when a TEK has no report type set.
     *
     * <p>This report type gets used when creating the {@link ExposureWindow}s and the {@link
     * DailySummary}s. The system will treat TEKs with missing report types as if they had this
     * provided report type.
     */
    @ReportType
    public int getReportTypeWhenMissing() {
        return reportTypeWhenMissing;
    }

    /**
     * Infectiousness of TEKs for which onset of symptoms is not set.
     *
     * <p>See {@link #getDaysSinceOnsetToInfectiousness} for more info.
     */
    @Infectiousness
    public int getInfectiousnessWhenDaysSinceOnsetMissing() {
        return infectiousnessWhenDaysSinceOnsetMissing;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof DiagnosisKeysDataMapping) {
            DiagnosisKeysDataMapping that = (DiagnosisKeysDataMapping) obj;
            return daysSinceOnsetToInfectiousness.equals(that.daysSinceOnsetToInfectiousness)
                    && reportTypeWhenMissing == that.reportTypeWhenMissing
                    && infectiousnessWhenDaysSinceOnsetMissing
                    == that.infectiousnessWhenDaysSinceOnsetMissing;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
                daysSinceOnsetToInfectiousness,
                reportTypeWhenMissing,
                infectiousnessWhenDaysSinceOnsetMissing);
    }

    @Override
    public String toString() {
        return String.format(
                Locale.US,
                "DiagnosisKeysDataMapping<"
                        + "daysSinceOnsetToInfectiousness: %s, "
                        + "reportTypeWhenMissing: %d, "
                        + "infectiousnessWhenDaysSinceOnsetMissing: %d>",
                getDaysSinceOnsetToInfectiousness(),
                reportTypeWhenMissing,
                infectiousnessWhenDaysSinceOnsetMissing);
    }

    /** A builder for {@link DiagnosisKeysDataMapping}. */
    public static final class DiagnosisKeysDataMappingBuilder {
        private List<Integer> daysSinceOnsetToInfectiousness = null;

        @ReportType private int reportTypeWhenMissing = ReportType.UNKNOWN;

        @Infectiousness private Integer infectiousnessWhenDaysSinceOnsetMissing = null;

        /**
         * For internal converting the data back to API surface data structure.
         *
         * @hide
         */
        public DiagnosisKeysDataMappingBuilder internalSetDaysSinceOnsetToInfectiousness(
                List<ExposureNotificationEnums.Infectiousness> daysSinceOnsetToInfectiousness) {
            checkArgument(
                    daysSinceOnsetToInfectiousness.size() == SIZE_OF_DAYS_SINCE_ONSET_TO_INFECTIOUSNESS,
                    "The size of daysSinceOnsetToInfectiousness must be %d.",
                    SIZE_OF_DAYS_SINCE_ONSET_TO_INFECTIOUSNESS);

            this.daysSinceOnsetToInfectiousness =
                    Lists.newArrayListWithExpectedSize(SIZE_OF_DAYS_SINCE_ONSET_TO_INFECTIOUSNESS);
            for (ExposureNotificationEnums.Infectiousness infectiousness :
                    daysSinceOnsetToInfectiousness) {
                this.daysSinceOnsetToInfectiousness.add(infectiousness.getNumber());
            }
            return this;
        }

        public DiagnosisKeysDataMappingBuilder setDaysSinceOnsetToInfectiousness(
                Map<Integer, Integer> daysSinceOnsetToInfectiousness) {
            checkArgument(
                    daysSinceOnsetToInfectiousness != null,
                    "daysSinceOnsetToInfectiousness must not be null.");
            checkArgument(
                    daysSinceOnsetToInfectiousness.size() <= SIZE_OF_DAYS_SINCE_ONSET_TO_INFECTIOUSNESS,
                    "the size of daysSinceOnsetToInfectiousness exceeds maximum size %d.",
                    SIZE_OF_DAYS_SINCE_ONSET_TO_INFECTIOUSNESS);

            Integer[] infectiousnessArray = new Integer[SIZE_OF_DAYS_SINCE_ONSET_TO_INFECTIOUSNESS];
            Arrays.fill(infectiousnessArray, Infectiousness.NONE);
            for (Map.Entry<Integer, Integer> entry : daysSinceOnsetToInfectiousness.entrySet()) {
                int daysSinceOnset = entry.getKey();
                int infectiousness = entry.getValue();
                checkArgument(
                        Math.abs(daysSinceOnset) <= MAXIMUM_OFFSET_OF_DAYS_SINCE_ONSET,
                        "Invalid day since onset %d",
                        daysSinceOnset);
                checkArgument(
                        ExposureNotificationEnums.Infectiousness.forNumber(infectiousness) != null,
                        "Invalid value of Infectiousness %d",
                        infectiousness);
                infectiousnessArray[MAXIMUM_OFFSET_OF_DAYS_SINCE_ONSET + daysSinceOnset] = infectiousness;
            }
            this.daysSinceOnsetToInfectiousness = Arrays.asList(infectiousnessArray);
            return this;
        }

        public DiagnosisKeysDataMappingBuilder setReportTypeWhenMissing(
                @ReportType int reportTypeWhenMissing) {
            checkArgument(
                    reportTypeWhenMissing != ReportType.UNKNOWN, "Invalid reportTypeWhenMissing value");
            checkArgument(
                    reportTypeWhenMissing >= ReportType.UNKNOWN
                            && reportTypeWhenMissing <= ReportType.REVOKED,
                    "Invalid value of ReportType %d",
                    reportTypeWhenMissing);
            this.reportTypeWhenMissing = reportTypeWhenMissing;
            return this;
        }

        public DiagnosisKeysDataMappingBuilder setInfectiousnessWhenDaysSinceOnsetMissing(
                @Infectiousness int infectiousnessWhenDaysSinceOnsetMissing) {
            checkArgument(
                    ExposureNotificationEnums.Infectiousness.forNumber(
                            infectiousnessWhenDaysSinceOnsetMissing)
                            != null,
                    "Invalid value of Infectiousness %d",
                    infectiousnessWhenDaysSinceOnsetMissing);
            this.infectiousnessWhenDaysSinceOnsetMissing = infectiousnessWhenDaysSinceOnsetMissing;
            return this;
        }

        public DiagnosisKeysDataMapping build() {
            checkArgument(
                    daysSinceOnsetToInfectiousness != null, "Must set daysSinceOnsetToInfectiousness");
            checkArgument(reportTypeWhenMissing != ReportType.UNKNOWN, "Must set reportTypeWhenMissing");
            checkArgument(
                    infectiousnessWhenDaysSinceOnsetMissing != null,
                    "Must set infectiousnessWhenDaysSinceOnsetMissing");
            return new DiagnosisKeysDataMapping(
                    daysSinceOnsetToInfectiousness,
                    reportTypeWhenMissing,
                    infectiousnessWhenDaysSinceOnsetMissing);
        }
    }
}