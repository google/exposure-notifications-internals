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

import androidx.annotation.IntDef;

import com.google.samples.exposurenotification.ExposureNotificationEnums;

/**
 * Calibration confidence defined for an {@link ExposureWindow}.
 */
@IntDef({
        CalibrationConfidence.LOWEST,
        CalibrationConfidence.LOW,
        CalibrationConfidence.MEDIUM,
        CalibrationConfidence.HIGH,
})
public @interface CalibrationConfidence {
    /**
     * No calibration data, using fleet-wide as default options.
     */
    int LOWEST = ExposureNotificationEnums.CalibrationConfidence.LOWEST_CONFIDENCE_VALUE;

    /**
     * Using average calibration over models from manufacturer.
     */
    int LOW = ExposureNotificationEnums.CalibrationConfidence.LOW_CONFIDENCE_VALUE;

    /**
     * Using single-antenna orientation for a similar model.
     */
    int MEDIUM = ExposureNotificationEnums.CalibrationConfidence.MEDIUM_CONFIDENCE_VALUE;

    /**
     * Using significant calibration data for this model.
     */
    int HIGH = ExposureNotificationEnums.CalibrationConfidence.HIGH_CONFIDENCE_VALUE;
}