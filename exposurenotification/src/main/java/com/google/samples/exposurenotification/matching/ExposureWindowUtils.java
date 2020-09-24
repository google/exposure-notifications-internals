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

import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;

import com.google.samples.exposurenotification.ExposureKeyExportProto.TemporaryExposureKey.ReportType;
import com.google.samples.exposurenotification.ExposureNotificationEnums.Infectiousness;
import com.google.samples.exposurenotification.storage.ExposureResult;
import com.google.samples.exposurenotification.storage.ExposureResultStorage;
import com.google.samples.exposurenotification.storage.ExposureWindowProto;
import com.google.samples.exposurenotification.storage.ScanInstanceProto;
import com.google.samples.exposurenotification.storage.StorageException;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.samples.exposurenotification.nearby.DiagnosisKeysDataMapping.MAXIMUM_OFFSET_OF_DAYS_SINCE_ONSET;
import static com.google.samples.exposurenotification.nearby.DiagnosisKeysDataMapping.SIZE_OF_DAYS_SINCE_ONSET_TO_INFECTIOUSNESS;
import static com.google.samples.exposurenotification.ExposureKeyExportProto.TemporaryExposureKey.ReportType.REPORT_TYPE_UNKNOWN;
import static com.google.samples.exposurenotification.matching.ExposureMatchingTracer.TOKEN_A;
import static java.lang.Math.min;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Utilities for get exposure windows.
 */
public class ExposureWindowUtils {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @TargetApi(VERSION_CODES.KITKAT)
    public static List<ExposureWindowProto> getExposureWindows(
            String callingPackage, byte[] signatureHash, ExposureResultStorage exposureResultStorage) {
        List<ExposureWindowProto> exposureWindows = new ArrayList<>();

        try {
            List<ExposureResult> entries =
                    exposureResultStorage.getAll(callingPackage, signatureHash, TOKEN_A);
            for (ExposureResult exposureResult : entries) {
                exposureWindows.addAll(exposureResult.getExposureWindowsList());
            }
        } catch (StorageException e) {
            throw new RuntimeException("FAILED_DISK_IO", e);
        }
        Collections.shuffle(exposureWindows, SECURE_RANDOM);
        return exposureWindows;
    }

    /**
     * Returns epoch timestamp indicating the beginning of the given timezone day for the provided
     * epoch.
     */
    public static long epochToDay(int epoch, DateTimeZone dateTimeZone) {
        Instant instant = new Instant(SECONDS.toMillis(epoch));
        DateTime date = instant.toDateTime(dateTimeZone).withTime(0, 0, 0, 0);
        return date.getMillis();
    }

    public static int getTypicalAttenuation(ScanInstanceProto scanInstanceProto) {
        double sum = 0;
        for (int attenuation : scanInstanceProto.getAttenuationsList()) {
            sum += attenuation;
        }
        return (int) Math.round(sum / scanInstanceProto.getAttenuationsCount());
    }

    public static int getMinAttenuation(ScanInstanceProto scanInstanceProto) {
        int min = Integer.MAX_VALUE;
        for (int attenuation : scanInstanceProto.getAttenuationsList()) {
            min = min(min, attenuation);
        }
        return min;
    }

    public static Infectiousness getInfectiousness(
            int daysSinceOnsetOfSymptoms,
            List<Infectiousness> daysSinceOnsetToInfectiousness,
            Infectiousness defaultInfectiousness) {
        if (Math.abs(daysSinceOnsetOfSymptoms) > MAXIMUM_OFFSET_OF_DAYS_SINCE_ONSET) {
            return defaultInfectiousness;
        }
        if (daysSinceOnsetToInfectiousness.size() != SIZE_OF_DAYS_SINCE_ONSET_TO_INFECTIOUSNESS) {
            return defaultInfectiousness;
        }
        return daysSinceOnsetToInfectiousness.get(
                daysSinceOnsetOfSymptoms + MAXIMUM_OFFSET_OF_DAYS_SINCE_ONSET);
    }

    public static ReportType getReportType(
            @com.google.samples.exposurenotification.nearby.ReportType int reportTypeValue,
            ReportType defaultReportType) {
        ReportType reportType = ReportType.forNumber(reportTypeValue);
        if (reportType == null || REPORT_TYPE_UNKNOWN.equals(reportType)) {
            reportType = defaultReportType;
        }
        return reportType;
    }

    private ExposureWindowUtils() {
    }
}