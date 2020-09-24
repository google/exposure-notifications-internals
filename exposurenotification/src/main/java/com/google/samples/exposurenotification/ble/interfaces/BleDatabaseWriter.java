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

package com.google.samples.exposurenotification.ble.interfaces;

import com.google.samples.exposurenotification.nearby.TemporaryExposureKey;

/**
 * Stores BLE scanning and advertising history to database.
 */
public interface BleDatabaseWriter {
    /**
     * Writes sighting contact tracing ID into database.
     */
    void writeBleSighting(
            byte[] rollingProximityId,
            int rssi,
            byte[] associatedEncryptedMetadata,
            int previousScanEpochSeconds);

    /**
     * Writes new generated contact tracing ID into database.
     */
    void writeNewTemporaryExposureKey(TemporaryExposureKey temporaryExposureKey);
}