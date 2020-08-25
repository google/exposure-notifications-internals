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

import androidx.annotation.IntDef;

/**
 * Report type defined for a {@link TemporaryExposureKey}.
 */
@IntDef({
        ReportType.UNKNOWN,
        ReportType.CONFIRMED_TEST,
        ReportType.CONFIRMED_CLINICAL_DIAGNOSIS,
        ReportType.SELF_REPORT,
        ReportType.RECURSIVE,
        ReportType.REVOKED,
})
public @interface ReportType {
    int UNKNOWN = 0;
    int CONFIRMED_TEST = 1;
    int CONFIRMED_CLINICAL_DIAGNOSIS = 2;
    int SELF_REPORT = 3;
    int RECURSIVE = 4;
    int REVOKED = 5;
}