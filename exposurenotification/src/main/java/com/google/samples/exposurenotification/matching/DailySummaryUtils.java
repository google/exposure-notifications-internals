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

import com.google.samples.exposurenotification.DailySummariesConfig;
import com.google.samples.exposurenotification.DailySummariesConfig.DistanceEstimate;
import com.google.samples.exposurenotification.DailySummary;
import com.google.samples.exposurenotification.DailySummary.DailySummaryBuilder;
import com.google.samples.exposurenotification.DailySummary.ExposureSummaryData;
import com.google.samples.exposurenotification.DailySummary.ExposureSummaryData.ExposureSummaryDataBuilder;
import com.google.samples.exposurenotification.ExposureKeyExportProto;
import com.google.samples.exposurenotification.Log;
import com.google.samples.exposurenotification.TekMetadataUtils;
import com.google.samples.exposurenotification.storage.ExposureWindowProto;
import com.google.samples.exposurenotification.storage.ScanInstanceProto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.samples.exposurenotification.matching.ExposureWindowUtils.getTypicalAttenuation;
import static java.lang.Math.max;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Utilities for creating daily summaries.
 */
public class DailySummaryUtils {
    public static List<DailySummary> getDailySummaries(
            DailySummariesConfig config, List<ExposureWindowProto> exposureWindows) {
        Map<Long, List<ExposureWindowProto>> windows = new HashMap<>();
        for (ExposureWindowProto window : exposureWindows) {
            if (!TekMetadataUtils.isTekReportTypeReturnableToClients(window.getTekMetadata())) {
                Log.log
                        .atInfo()
                        .log(
                                "ExposureWindow ignored due to invalid report type %s",
                                window.getTekMetadata().getReportType());
                continue;
            }
            long windowEpochDays = SECONDS.toDays(window.getEpochSeconds());
            if (config.getDaysSinceExposureThreshold() > 0) {
                long todayEpochDays = MILLISECONDS.toDays(System.currentTimeMillis());
                if (todayEpochDays - windowEpochDays > config.getDaysSinceExposureThreshold()) {
                    Log.log
                            .atInfo()
                            .log(
                                    "ExposureWindow ignored due to too old, days since Window.Days=%d,"
                                            + " Config.daysSinceExposureThreshold=%d",
                                    todayEpochDays - windowEpochDays, config.getDaysSinceExposureThreshold());
                    continue;
                }
            }

            List<ExposureWindowProto> windowProtoList = windows.get(windowEpochDays);
            if (windowProtoList == null) {
                windowProtoList = new ArrayList<>();
                windows.put(windowEpochDays, windowProtoList);
            }
            windowProtoList.add(window);
        }

        if (windows.isEmpty()) {
            return new ArrayList<>();
        }

        ArrayList<DailySummary> result = new ArrayList<>(windows.size());
        for (long epochDay : windows.keySet()) {
            List<ExposureWindowProto> windowProtoList = windows.get(epochDay);
            SummaryItemScores[] scoresOfAllReports =
                    new SummaryItemScores
                            [ExposureKeyExportProto.TemporaryExposureKey.ReportType.values().length];
            for (int i = 0; i < scoresOfAllReports.length; i++) {
                scoresOfAllReports[i] = new SummaryItemScores();
            }

            for (ExposureWindowProto exposureWindowProto : windowProtoList) {
                SummaryItemScores scores =
                        scoresOfAllReports[exposureWindowProto.getTekMetadata().getReportType().getNumber()];
                double windowWeightedDuration =
                        calculateWindowWeightedDuration(exposureWindowProto, config);
                double windowScore =
                        calculateWindowScore(exposureWindowProto, windowWeightedDuration, config);
                if (windowScore < config.getMinimumWindowScore()) {
                    Log.log
                            .atInfo()
                            .log(
                                    "ExposureWindow(%d) ignored due to score(%.3f) is lower than"
                                            + " MinimumWindowScore(%.3f)",
                                    exposureWindowProto.getEpochSeconds(),
                                    windowScore,
                                    config.getMinimumWindowScore());
                    continue;
                }
                scores.sum += windowScore;
                scores.maximum = max(scores.maximum, windowScore);
                scores.weightedDurationSum += windowWeightedDuration;
            }

            result.add(createDailySummary((int) epochDay, scoresOfAllReports));
        }
        return result;
    }

    private static double calculateWindowWeightedDuration(
            ExposureWindowProto window, DailySummariesConfig config) {
        int[] durations = new int[DailySummariesConfig.DistanceEstimate.values().length];

        for (ScanInstanceProto scan : window.getScanInstancesList()) {
            durations[
                    DistanceEstimate.getDistanceEstimate(getTypicalAttenuation(scan), config)
                            .ordinal()] +=
                    scan.getSecondsSinceLastScan();
        }
        double result = 0;
        for (DistanceEstimate distanceEstimate : DistanceEstimate.values()) {
            result +=
                    durations[distanceEstimate.ordinal()]
                            * config.getAttenuationBucketWeights().get(distanceEstimate.ordinal());
        }
        return result;
    }

    private static class SummaryItemScores {
        private double maximum = 0;
        private double sum = 0;
        private double weightedDurationSum = 0;
    }

    private static DailySummary createDailySummary(
            int epochDays, SummaryItemScores[] scoresOfAllReports) {
        DailySummaryBuilder dailySummaryBuilder = new DailySummaryBuilder();
        dailySummaryBuilder.setDaysSinceEpoch(epochDays);
        for (ExposureKeyExportProto.TemporaryExposureKey.ReportType reportType :
                ExposureKeyExportProto.TemporaryExposureKey.ReportType.values()) {
            dailySummaryBuilder.setReportSummary(
                    reportType.getNumber(),
                    createExposureSummaryData(scoresOfAllReports[reportType.getNumber()]));
        }

        SummaryItemScores scoresOfDaySummary = new SummaryItemScores();
        for (ExposureKeyExportProto.TemporaryExposureKey.ReportType reportType :
                ExposureKeyExportProto.TemporaryExposureKey.ReportType.values()) {
            SummaryItemScores scoreOfReport = scoresOfAllReports[reportType.getNumber()];
            scoresOfDaySummary.maximum = max(scoresOfDaySummary.maximum, scoreOfReport.maximum);
            scoresOfDaySummary.sum += scoreOfReport.sum;
            scoresOfDaySummary.weightedDurationSum += scoreOfReport.weightedDurationSum;
        }
        dailySummaryBuilder.setSummaryData(createExposureSummaryData(scoresOfDaySummary));
        return dailySummaryBuilder.build();
    }

    private static ExposureSummaryData createExposureSummaryData(SummaryItemScores scores) {
        return new ExposureSummaryDataBuilder()
                .setScoreSum(scores.sum)
                .setMaximumScore(scores.maximum)
                .setWeightedDurationSum(scores.weightedDurationSum)
                .build();
    }

    private static double calculateWindowScore(
            ExposureWindowProto window, double windowWeightedDuration, DailySummariesConfig config) {
        return windowWeightedDuration
                * config.getReportTypeWeights().get(window.getTekMetadata().getReportType().getNumber())
                * config
                .getInfectiousnessWeights()
                .get(window.getTekMetadata().getInfectiousness().getNumber());
    }

    private DailySummaryUtils() {
    }
}