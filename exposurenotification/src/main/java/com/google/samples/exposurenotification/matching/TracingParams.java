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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.auto.value.AutoValue;
import com.google.samples.exposurenotification.features.ContactTracingFeature;
import com.google.samples.exposurenotification.storage.TracingParamsRecord;

import org.joda.time.Duration;

/** Parameters required for {@link ExposureMatchingTracer}. */
@AutoValue
public abstract class TracingParams {

    /**
     * Minimum consecutive sightings in duration required to be considered a contact, compared with a
     * bucketized duration calculation. See {@link ExposureMatchingTracer} for more details. If not
     * set, has default value {@link ContactTracingFeature#minExposureBucketizedDurationSeconds()}
     * converted to {@link Duration}.
     */
    public abstract Duration minExposureBucketizedDuration();

    /**
     * Minimum ble signal strength indicator value that must be reached by at least one sighting in a
     * series of sightings that are being considered for an exposure. If not set, has default value
     * {@link ContactTracingFeature#defaultMinExposureAttenuationValue()} converted to an int.
     */
    // TODO(wukevin): remove this field.
    public abstract int minimumAttenuationValue();

    /**
     * Maximum duration gap between two sightings to be considered continuous contact with another
     * device. If not set, has default value {@link
     * ContactTracingFeature#maxInterpolationDurationSeconds()} converted to {@link Duration}.
     */
    public abstract Duration maxInterpolationDuration();

    /**
     * Bluetooth token scanning interval. If not set, has default value {@link
     * ContactTracingFeature#scanIntervalSeconds()} converted to {@link Duration}.
     */
    public abstract Duration scanInterval();

    /**
     * Boolean controlling the use of interpolated attentuation levels when computing duration at
     * thresholds. If not set, defaults to false.
     *
     * <p>TODO(b/159218245): add ContactTracingFeature flag for a default value.
     */
    public abstract Boolean interpolationEnabled();

    public static Builder builder() {
        return new AutoValue_TracingParams.Builder()
                .setMinExposureBucketizedDuration(
                        Duration.standardSeconds(ContactTracingFeature.minExposureBucketizedDurationSeconds()))
                .setMinimumAttenuationValue(
                        (int) ContactTracingFeature.defaultMinExposureAttenuationValue())
                .setMaxInterpolationDuration(
                        Duration.standardSeconds(ContactTracingFeature.maxInterpolationDurationSeconds()))
                .setScanInterval(Duration.standardSeconds(ContactTracingFeature.scanIntervalSeconds()))
                .setInterpolationEnabled(false);
    }

    public TracingParamsRecord toTracingParamsRecord() {
        return TracingParamsRecord.newBuilder()
                .setMinAttenuationValue(minimumAttenuationValue())
                .setMaxInterpolationDurationSeconds(maxInterpolationDuration().getStandardSeconds())
                .setMinExposureBucketizedDurationSeconds(
                        minExposureBucketizedDuration().getStandardSeconds())
                .setScanIntervalSeconds((int) scanInterval().getStandardSeconds())
                .setInterpolationEnabled(interpolationEnabled())
                .build();
    }

    /** Builder clas for {@link TracingParams}. */
    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setMinExposureBucketizedDuration(
                Duration minExposureBucketizedDuration);

        public abstract Builder setMinimumAttenuationValue(int minimumAttenuationValue);

        public abstract Builder setMaxInterpolationDuration(Duration maxInterpolationDuration);

        public abstract Builder setScanInterval(Duration scanInterval);

        public abstract Builder setInterpolationEnabled(boolean interpolationEnabled);

        public abstract TracingParams autoBuild();

        public TracingParams build() {
            TracingParams tracingParams = autoBuild();
            // Use reverse logic to get >= equivalent from org.joda.time.Duration api.
            checkArgument(!tracingParams.minExposureBucketizedDuration().isShorterThan(Duration.ZERO));
            checkArgument(tracingParams.minimumAttenuationValue() >= 0);
            checkArgument(tracingParams.scanInterval().isLongerThan(Duration.ZERO));
            checkArgument(tracingParams.maxInterpolationDuration().isLongerThan(Duration.ZERO));
            return tracingParams;
        }
    }
}