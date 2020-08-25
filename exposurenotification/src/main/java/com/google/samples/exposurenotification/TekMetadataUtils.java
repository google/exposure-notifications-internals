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

package com.google.samples.exposurenotification;

import com.google.samples.exposurenotification.ExposureKeyExportProto.TemporaryExposureKey.ReportType;
import com.google.samples.exposurenotification.storage.TekMetadata;

/**
 * Helpers for working with TEK metadata.
 */
public final class TekMetadataUtils {

    /**
     * Returns true if the metadata's report type is supported by clients. This excludes revoked, for
     * example.
     */
    public static boolean isTekReportTypeReturnableToClients(TekMetadata metadata) {
        return metadata.getReportType() == ReportType.CONFIRMED_TEST
                || metadata.getReportType() == ReportType.CONFIRMED_CLINICAL_DIAGNOSIS
                || metadata.getReportType() == ReportType.SELF_REPORT
                || metadata.getReportType() == ReportType.RECURSIVE;
    }

    private TekMetadataUtils() {
    }
}