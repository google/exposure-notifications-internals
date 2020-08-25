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

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.auto.value.AutoValue;
import com.google.samples.exposurenotification.data.BluetoothMetadata;
import com.google.samples.exposurenotification.storage.SightingRecord;

/** A pair of {@link SightingRecord} and {@link BluetoothMetadata}. */
@AutoValue
abstract class SightingRecordWithMetadata {
    static SightingRecordWithMetadata create(
            SightingRecord sightingRecord, BluetoothMetadata metadata) {
        return new AutoValue_SightingRecordWithMetadata(sightingRecord, metadata);
    }

    abstract SightingRecord sightingRecord();

    /**
     * A decrypted version of {@link SightingRecord#getAssociatedEncryptedMetadata()} of {@link
     * #sightingRecord()}.
     */
    abstract BluetoothMetadata metadata();

    public int attenuationValue() {
        return Math.max(metadata().txPower() - sightingRecord().getRssi(), 0);
    }

    public int secondsSinceLastScan(int maxMinutesSinceLastScan, int defaultMinutesSinceLastScan) {
        // Default value, indicating we do not know when was the previous scan.
        if (sightingRecord().getPreviousScanEpochSeconds() == 0) {
            return (int) MINUTES.toSeconds(defaultMinutesSinceLastScan);
        }

        return (int)
                MINUTES.toSeconds(
                        Math.min(
                                maxMinutesSinceLastScan,
                                SECONDS.toMinutes(sightingRecord().getEpochSeconds())
                                        - SECONDS.toMinutes(sightingRecord().getPreviousScanEpochSeconds())));
    }
}