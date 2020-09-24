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

package com.google.samples.exposurenotification.storage;

import com.google.common.base.Converter;
import com.google.common.primitives.Ints;
import com.google.samples.exposurenotification.nearby.ExposureConfiguration;
import com.google.samples.exposurenotification.nearby.ExposureConfiguration.ExposureConfigurationBuilder;

/**
 * Converts between the Android Parcelable {@link ExposureConfiguration} and the proto {@link
 * ExposureConfigurationProto}.
 *
 * <p>Why this converter? https://developer.android.com/reference/android/os/Parcel.html.
 */
public class ExposureConfigurationConverter
        extends Converter<ExposureConfiguration, ExposureConfigurationProto> {

    @Override
    protected ExposureConfigurationProto doForward(ExposureConfiguration config) {
        return ExposureConfigurationProto.newBuilder()
                .setMinimumRiskScore(config.getMinimumRiskScore())
                .setAttenuationWeight(config.getAttenuationWeight())
                .addAllAttenuationScores(Ints.asList(config.getAttenuationScores()))
                .setDaysSinceLastExposureWeight(config.getDaysSinceLastExposureWeight())
                .addAllDaysSinceLastExposureScores(Ints.asList(config.getDaysSinceLastExposureScores()))
                .setDurationWeight(config.getDurationWeight())
                .addAllDurationScores(Ints.asList(config.getDurationScores()))
                .setTransmissionRiskWeight(config.getTransmissionRiskWeight())
                .addAllTransmissionRiskScores(Ints.asList(config.getTransmissionRiskScores()))
                .addAllDurationAtAttenuationThresholds(
                        Ints.asList(config.getDurationAtAttenuationThresholds()))
                .build();
    }

    @Override
    protected ExposureConfiguration doBackward(ExposureConfigurationProto proto) {
        return new ExposureConfigurationBuilder()
                .setMinimumRiskScore(proto.getMinimumRiskScore())
                .setAttenuationWeight(proto.getAttenuationWeight())
                .setAttenuationScores(Ints.toArray(proto.getAttenuationScoresList()))
                .setDaysSinceLastExposureWeight(proto.getDaysSinceLastExposureWeight())
                .setDaysSinceLastExposureScores(Ints.toArray(proto.getDaysSinceLastExposureScoresList()))
                .setDurationWeight(proto.getDurationWeight())
                .setDurationScores(Ints.toArray(proto.getDurationScoresList()))
                .setTransmissionRiskWeight(proto.getTransmissionRiskWeight())
                .setTransmissionRiskScores(Ints.toArray(proto.getTransmissionRiskScoresList()))
                .setDurationAtAttenuationThresholds(
                        Ints.toArray(proto.getDurationAtAttenuationThresholdsList()))
                .build();
    }
}