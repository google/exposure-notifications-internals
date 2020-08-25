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

import android.util.Pair;

import androidx.annotation.Nullable;

import com.google.common.collect.Iterables;
import com.google.samples.exposurenotification.ExposureConfiguration;
import com.google.samples.exposurenotification.ExposureKeyExportProto.TemporaryExposureKey.ReportType;
import com.google.samples.exposurenotification.ExposureNotificationEnums.Infectiousness;
import com.google.samples.exposurenotification.Log;
import com.google.samples.exposurenotification.TemporaryExposureKey;
import com.google.samples.exposurenotification.features.ContactTracingFeature;
import com.google.samples.exposurenotification.storage.ExposureRecord;
import com.google.samples.exposurenotification.storage.ExposureResult;
import com.google.samples.exposurenotification.storage.ExposureWindowProto;
import com.google.samples.exposurenotification.storage.ScanInstanceProto;
import com.google.samples.exposurenotification.storage.SightingRecord;
import com.google.samples.exposurenotification.storage.TekMetadata;
import com.google.samples.exposurenotification.storage.TekMetadataRecord;

import org.joda.time.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * Handles the evaluation of exposure for a single diagnosis key and its sightings.
 *
 * <p>See {@link #findExposures(TemporaryExposureKey, List)}.
 */
final class KeyExposureEvaluator {
    private final ExposureConfiguration exposureConfiguration;
    private final RiskScoreCalculator riskScoreCalculator;
    private final TracingParams tracingParams;
    private final String instanceLogTag;
    private final TekMetadataRecord tekMetadataRecord;

    /**
     * @param exposureConfiguration exposure configuration used to run evaluations with.
     * @param riskScoreCalculator   risk score calculator to generate scores with
     * @param tracingParams         tracing parameters for filtering exposures based on sightings
     * @param instanceLogTag        log tag associated with matching job. See {@link ExposureMatchingTracer}.
     * @param tekMetadataRecord     tek data mapping for creating {@link ExposureWindowProto}.
     */
    KeyExposureEvaluator(
            ExposureConfiguration exposureConfiguration,
            RiskScoreCalculator riskScoreCalculator,
            TracingParams tracingParams,
            String instanceLogTag,
            TekMetadataRecord tekMetadataRecord) {
        this.exposureConfiguration = exposureConfiguration;
        this.riskScoreCalculator = riskScoreCalculator;
        this.tracingParams = tracingParams;
        this.instanceLogTag = instanceLogTag;
        this.tekMetadataRecord = tekMetadataRecord;
    }

    /**
     * Finds exposure windows given the time-sorted sightings seen from the provided {@link
     * TemporaryExposureKey}.
     *
     * <p>Exposures to one TEK are split in windows of 30 minutes, so a given TEK may lead to several
     * exposure windows if beacon sightings for it spanned more than 30 minutes.
     *
     * @param diagnosisKey    The diagnosis key whose rolling proximity identifiers were sighted in the
     *                        provided list of sightings.
     * @param sightingRecords a list of rolling proximity id sightings.
     * @return list of exposure windows for the diagnosisKey.
     */
    List<ExposureWindowProto> findExposureWindows(
            TemporaryExposureKey diagnosisKey, List<SightingRecordWithMetadata> sightingRecords) {
        List<ExposureWindowProto> exposureWindows = new ArrayList<>();

        ExposureWindowProto.Builder currentExposureWindowBuilder = null;
        int previousSightingEpochSeconds = 0;
        Infectiousness infectiousness =
                ExposureWindowUtils.getInfectiousness(
                        diagnosisKey.getDaysSinceOnsetOfSymptoms(),
                        tekMetadataRecord.getDaysSinceOnsetToInfectiousnessList(),
                        tekMetadataRecord.getInfectiousnessWhenDaysSinceOnsetMissing());
        ReportType reportType =
                ExposureWindowUtils.getReportType(
                        diagnosisKey.getReportType(), tekMetadataRecord.getReportTypeWhenMissing());
        for (SightingRecordWithMetadata sightingRecordWithMetadata : sightingRecords) {
            if (shouldStartNewExposureWindow(sightingRecordWithMetadata, currentExposureWindowBuilder)) {
                if (currentExposureWindowBuilder != null) {
                    exposureWindows.add(currentExposureWindowBuilder.build());
                }
                currentExposureWindowBuilder =
                        ExposureWindowProto.newBuilder()
                                .setEpochSeconds(sightingRecordWithMetadata.sightingRecord().getEpochSeconds())
                                .setTekMetadata(
                                        TekMetadata.newBuilder()
                                                .setInfectiousness(infectiousness)
                                                .setReportType(reportType)
                                                .setCalibrationConfidence(
                                                        sightingRecordWithMetadata.metadata().calibrationConfidence()));
            }
            if (sightingInTheSameScanInstance(sightingRecordWithMetadata, previousSightingEpochSeconds)
                    && currentExposureWindowBuilder.getScanInstancesCount() > 0) {
                // Add to the last existing scan instance.
                currentExposureWindowBuilder.setScanInstances(
                        currentExposureWindowBuilder.getScanInstancesCount() - 1,
                        currentExposureWindowBuilder
                                .getScanInstances(currentExposureWindowBuilder.getScanInstancesCount() - 1)
                                .toBuilder()
                                .addAttenuations(sightingRecordWithMetadata.attenuationValue()));
            } else {
                // Create a new ScanInstance.
                currentExposureWindowBuilder.addScanInstances(
                        ScanInstanceProto.newBuilder()
                                .addAttenuations(sightingRecordWithMetadata.attenuationValue())
                                .setSecondsSinceLastScan(
                                        sightingRecordWithMetadata.secondsSinceLastScan(
                                                (int) ContactTracingFeature.maxMinutesSinceLastScan(),
                                                (int) ContactTracingFeature.defaultMinutesSinceLastScan())));
            }
            previousSightingEpochSeconds = sightingRecordWithMetadata.sightingRecord().getEpochSeconds();
        }

        if (currentExposureWindowBuilder != null) {
            exposureWindows.add(currentExposureWindowBuilder.build());
        }
        return exposureWindows;
    }

    /**
     * Finds exposures given the time-sorted sightings seen from the provided {@link
     * TemporaryExposureKey}.
     *
     * <p>Exposures are defined as consecutive sightings where the time between each sighting is not
     * longer than {@link TracingParams#maxInterpolationDuration()} and the exposure duration is not
     * shorter than {@link TracingParams#minExposureBucketizedDuration()}.
     *
     * <p>Exposure durations are rounded into five minute intervals before being evaluated against
     * {@link TracingParams#minExposureBucketizedDuration()}.
     *
     * @param diagnosisKey    The diagnosis key whose rolling proximity identifiers were sighted in the
     *                        provided list of sightings.
     * @param sightingRecords a list of rolling proximity id sightings.
     * @return exposure result containing list of exposures.
     */
    @Nullable
    ExposureResult findExposures(
            TemporaryExposureKey diagnosisKey, List<SightingRecordWithMetadata> sightingRecords) {
        if (sightingRecords.isEmpty()) {
            Log.log.atInfo().log("%s No sighting records.", instanceLogTag);
            return null;
        }
        List<ExposureRecord> exposureRecords = new ArrayList<>();
        // The average is computed using time spent at
        // attenuation levels as weights. The attenuation level between two scans is
        // the attenuation level of the first scan. The first and last scans get extended by half of
        // scanInterval. Consider:
        //
        // scanInterval = 6
        // t=00, attenuation=1
        // t=10, attenuation=4
        // t=50, attenuation=5
        //
        // The resulting attenuation value is:
        //
        // (6/2 * 1 + (10 - 0) * 1 + (50 - 10) * 4 + 6/2 * 5) / (3 + 10 + 40 +3) = 188 / 56 = 3.36
        //
        // The result is rounded to 3.

        int exposureFirstSightingSeconds = sightingRecords.get(0).sightingRecord().getEpochSeconds();
        List<TimeAndAttenuation> timeAndAttenuations = new ArrayList<>();

        int maxRiskScore = 0;
        int totalRiskScore = 0;
        List<Integer> totalTimesBelowBetweenAndAbove = Arrays.asList(0, 0, 0);

        for (int i = 0; i < sightingRecords.size(); ++i) {
            SightingRecord currentScan = sightingRecords.get(i).sightingRecord();
            timeAndAttenuations.add(
                    TimeAndAttenuation.create(
                            currentScan.getEpochSeconds(), sightingRecords.get(i).attenuationValue()));

            if (isEndOfPossibleExposureSightings(
                    i, sightingRecords, tracingParams.maxInterpolationDuration())) {
                Log.log.atVerbose().log("%s Found a series of sightings.", instanceLogTag);
                // 5 minutes bucketization, as defined in the API.
                Duration bucketizedDuration =
                        bucketizeDuration(
                                Duration.standardSeconds(
                                        currentScan.getEpochSeconds() - exposureFirstSightingSeconds),
                                Duration.standardMinutes(5));

                List<Period> periods =
                        computePeriods(timeAndAttenuations, tracingParams.scanInterval().getStandardSeconds());
                int weightedAttenuation = weightAttenuationOfPeriods(periods);
                int[] durationAtAttenuationThresholds =
                        exposureConfiguration.getDurationAtAttenuationThresholds();
                List<Integer> timesBelowBetweenAndAbove =
                        getTimeBelowBetweenAndAbove(
                                periods,
                                durationAtAttenuationThresholds[0],
                                durationAtAttenuationThresholds[1],
                                tracingParams.interpolationEnabled());

                Log.log
                        .atInfo()
                        .log(
                                "%s Bucketed duration=%dm >= min_duration=%dm ? %b.",
                                instanceLogTag,
                                bucketizedDuration.getStandardMinutes(),
                                tracingParams.minExposureBucketizedDuration().getStandardMinutes(),
                                !bucketizedDuration.isShorterThan(tracingParams.minExposureBucketizedDuration()));
                if (!bucketizedDuration.isShorterThan(tracingParams.minExposureBucketizedDuration())) {
                    Log.log.atVerbose().log("%s Found exposure.", instanceLogTag);
                    ExposureRecord exposureRecord =
                            ExposureRecord.newBuilder()
                                    .setAttenuationValue(weightedAttenuation)
                                    .setDurationSeconds((int) bucketizedDuration.getStandardSeconds())
                                    .addAllAttenuationDurations(timesBelowBetweenAndAbove)
                                    .setTransmissionRiskLevel(diagnosisKey.getTransmissionRiskLevel())
                                    .build();

                    long dateMillisSinceEpoch =
                            Duration.standardMinutes(
                                    diagnosisKey.getRollingStartIntervalNumber()
                                            * ContactTracingFeature.idRollingPeriodMinutes())
                                    .getMillis();
                    int riskScore =
                            riskScoreCalculator.calculateRiskScore(
                                    diagnosisKey, exposureRecord, dateMillisSinceEpoch, exposureConfiguration);
                    maxRiskScore = Math.max(riskScore, maxRiskScore);
                    totalRiskScore += riskScore;
                    for (int bin = 0; bin < totalTimesBelowBetweenAndAbove.size(); bin++) {
                        totalTimesBelowBetweenAndAbove.set(
                                bin, totalTimesBelowBetweenAndAbove.get(bin) + timesBelowBetweenAndAbove.get(bin));
                    }

                    exposureRecords.add(exposureRecord.toBuilder().setRiskScore(riskScore).build());
                }
                if (i + 1 == sightingRecords.size()) {
                    break;
                }
                exposureFirstSightingSeconds =
                        sightingRecords.get(i + 1).sightingRecord().getEpochSeconds();
                timeAndAttenuations.clear();
            }
        }
        if (exposureRecords.isEmpty()) {
            return null;
        }
        ReportType reportType =
                ExposureWindowUtils.getReportType(
                        diagnosisKey.getReportType(), tekMetadataRecord.getReportTypeWhenMissing());
        return ExposureResult.newBuilder()
                .addAllExposureRecords(exposureRecords)
                .setMaxRiskScore(maxRiskScore)
                .setDateMillisSinceEpoch(
                        Duration.standardMinutes(
                                diagnosisKey.getRollingStartIntervalNumber()
                                        * ContactTracingFeature.idRollingPeriodMinutes())
                                .getMillis())
                .setTracingParamsRecord(tracingParams.toTracingParamsRecord())
                .setTotalRiskScore(totalRiskScore)
                .addAllAttenuationDurations(totalTimesBelowBetweenAndAbove)
                .setTekMetadata(TekMetadata.newBuilder().setReportType(reportType))
                .setReportTypeTransitionCount(0)
                .build();
    }

    private static boolean shouldStartNewExposureWindow(
            SightingRecordWithMetadata sightingRecordWithMetadata,
            ExposureWindowProto.Builder currentExposureWindowBuilder) {
        return currentExposureWindowBuilder == null
                || sightingRecordWithMetadata.sightingRecord().getEpochSeconds()
                - currentExposureWindowBuilder.getEpochSeconds()
                >= MINUTES.toSeconds(ContactTracingFeature.maxExposureWindowDurationMinutes());
    }

    private static boolean sightingInTheSameScanInstance(
            SightingRecordWithMetadata sightingRecordWithMetadata, int previousSightingEpochSeconds) {
        // Sightings are considered to be in the same scan if they are separate by a relatively short
        // duration. The exact value here is not important, as scanning durations are at the order of
        // seconds vs. time between scans are at the order of minutes. We use 1.5 * max scan duration as
        // a simple heuristic.
        return sightingRecordWithMetadata.sightingRecord().getEpochSeconds()
                - previousSightingEpochSeconds
                <= 1.5
                * (ContactTracingFeature.scanTimeSeconds()
                + ContactTracingFeature.scanTimeExtendForProfileInUseSeconds());
    }

    /**
     * Returns a list of {@link Period}s with duration > 0.
     */
    private static List<Period> computePeriods(
            List<TimeAndAttenuation> timeAndAttenuations, long scanInterval) {
        // Add fake start and end boundary scans with the same attenuation as first and last ones.
        int timeMargin = (int) (scanInterval / 2);
        timeAndAttenuations.add(
                0,
                TimeAndAttenuation.create(
                        timeAndAttenuations.get(0).time() - timeMargin,
                        timeAndAttenuations.get(0).attenuation()));
        timeAndAttenuations.add(
                TimeAndAttenuation.create(
                        Iterables.getLast(timeAndAttenuations).time() + timeMargin,
                        Iterables.getLast(timeAndAttenuations).attenuation()));

        List<Period> periods = new ArrayList<>();

        TimeAndAttenuation previous = timeAndAttenuations.get(0);
        for (TimeAndAttenuation timeAndAttenuation : timeAndAttenuations) {
            // Nothing to do for 0-length segments.
            if (timeAndAttenuation.time().equals(previous.time())) {
                continue;
            }
            periods.add(Period.create(previous, timeAndAttenuation));
            previous = timeAndAttenuation;
        }

        return periods;
    }

    /**
     * Calculates a time-weighted attenuation value.
     */
    private static int weightAttenuationOfPeriods(List<Period> periods) {
        double attenuationSum = 0;
        double durationSum = 0;
        for (Period period : periods) {
            attenuationSum += period.scan1().attenuation() * period.duration();
            durationSum += period.duration();
        }
        return (int) Math.round(attenuationSum / durationSum);
    }

    private static List<Integer> getTimeBelowBetweenAndAbove(
            List<Period> periods, int thresholdLow, int thresholdHigh, boolean interpolate) {
        Pair<Integer, Integer> timeBelowAndAboveOfLow =
                getTimeBelowAndAbove(periods, thresholdLow, interpolate);
        Pair<Integer, Integer> timeBelowAndAboveOfHigh =
                getTimeBelowAndAbove(periods, thresholdHigh, interpolate);

        int timeBelowLow = timeBelowAndAboveOfLow.first;
        int timeAboveHigh = timeBelowAndAboveOfHigh.second;
        int totalTime = timeBelowAndAboveOfLow.first + timeBelowAndAboveOfLow.second;
        // Written explicitly to avoid confusion.
        int timeBetween = totalTime - timeBelowLow - timeAboveHigh;

        return Arrays.asList(timeBelowLow, timeBetween, timeAboveHigh);
    }

    /**
     * Calculates duration below and above the given threshold.
     *
     * <p>Set {@code interpolate} to {@code true} to use linear interpolation of the attenuation
     * values.
     */
    private static Pair<Integer, Integer> getTimeBelowAndAbove(
            List<Period> periods, int threshold, boolean interpolate) {
        int timeAboveThreshold = 0;
        int timeBelowThreshold = 0;
        for (Period period : periods) {

            int timeCross = period.calculateTimeCross(threshold, interpolate);

            // No cross in the interval.
            if (timeCross <= period.scan1().time() || timeCross >= period.scan2().time()) {
                if (period.scan1().attenuation() >= threshold) {
                    // The entire interval is above the threshold.
                    timeAboveThreshold += period.duration();
                } else {
                    // The entire interval is below threshold.
                    timeBelowThreshold += period.duration();
                }
            } else {
                // Cross in the interval, see how much is above and below.
                int leftTime = timeCross - period.scan1().time();
                int rightTime = period.scan2().time() - timeCross;
                if (period.scan1().attenuation() >= threshold) {
                    // The left hand side is above threshold
                    timeAboveThreshold += leftTime;
                    timeBelowThreshold += rightTime;
                } else {
                    // The right hand side is above the threshold
                    timeAboveThreshold += rightTime;
                    timeBelowThreshold += leftTime;
                }
            }
        }
        return new Pair<>(timeBelowThreshold, timeAboveThreshold);
    }

    /**
     * Returns true if the next {@link SightingRecord} is too far away in time from current {@link
     * SightingRecord}.
     *
     * @param index                    current index in question for end of sightings series or not.
     * @param sightingRecords          scan records in ascending order of scan time.
     * @param maxInterpolationDuration duration threshold for two {@link SightingRecord}s being too
     *                                 far away to be a part of the same exposure.
     */
    private static boolean isEndOfPossibleExposureSightings(
            int index,
            List<SightingRecordWithMetadata> sightingRecords,
            Duration maxInterpolationDuration) {
        // Last item always marks end of exposure sightings.
        if (index + 1 == sightingRecords.size()) {
            return true;
        }
        return Duration.standardSeconds(
                sightingRecords.get(index + 1).sightingRecord().getEpochSeconds()
                        - sightingRecords.get(index).sightingRecord().getEpochSeconds())
                .isLongerThan(maxInterpolationDuration);
    }

    /**
     * For every observation of a sighting (which occurs at {@code scanInterval}, our best estimate
     * for the duration of exposure is {@code scanInterval}. Therefore, to calculate exposure
     * duration, {@code scanInterval} must be added and the estimate must be rounded to the nearest
     * {@code scanInterval} increment.
     */
    private static Duration bucketizeDuration(Duration sightedScanDuration, Duration scanInterval) {
        Duration estimatedExposureDuration = sightedScanDuration.plus(scanInterval);
        long mod = estimatedExposureDuration.getStandardSeconds() % scanInterval.getStandardSeconds();
        if (mod <= scanInterval.getStandardSeconds() / 2) {
            return estimatedExposureDuration.minus(Duration.standardSeconds(mod));
        }
        return estimatedExposureDuration.plus(scanInterval.minus(Duration.standardSeconds(mod)));
    }
}