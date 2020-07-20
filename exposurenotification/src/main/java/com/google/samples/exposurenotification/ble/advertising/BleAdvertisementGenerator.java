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

package com.google.samples.exposurenotification.ble.advertising;

import androidx.annotation.VisibleForTesting;

import com.google.samples.exposurenotification.Log;
import com.google.samples.exposurenotification.ble.data.AdvertisementPacket;
import com.google.samples.exposurenotification.ble.interfaces.BleKeyGenerator;
import com.google.samples.exposurenotification.crypto.CryptoException;
import com.google.samples.exposurenotification.data.AssociatedEncryptedMetadata;
import com.google.samples.exposurenotification.data.BluetoothMetadata;
import com.google.samples.exposurenotification.data.RollingProximityId;
import com.google.samples.exposurenotification.data.generator.AssociatedEncryptedMetadataGenerator;
import com.google.samples.exposurenotification.features.ContactTracingFeature;

import static com.google.samples.exposurenotification.data.BluetoothMetadata.CALIBRATION_CONFIDENCE_BITS_MASK;
import static com.google.samples.exposurenotification.data.BluetoothMetadata.CALIBRATION_CONFIDENCE_BITS_OFFSET;
import static com.google.samples.exposurenotification.data.BluetoothMetadata.VERSION_1_0;
import static com.google.samples.exposurenotification.data.BluetoothMetadata.VERSION_1_1;

/**
 * Generates advertisement packet following v1.3 of the BLE spec.
 */
public class BleAdvertisementGenerator implements BleKeyGenerator {
    private final RollingProximityIdManager proxIdManager;

    public BleAdvertisementGenerator(RollingProximityIdManager proxIdManager) {
        this.proxIdManager = proxIdManager;
    }

    @Override
    public AdvertisementPacket generatePacket() throws CryptoException {
        // byte 0 - version
        // byte 1 - transmit power level
        // byte 2 - reserved
        // byte 3 - reserved
        return generatePacket(
                BluetoothMetadata.create(
                        createVersion(),
                        (byte)
                                (ContactTracingFeature.txCalibrationPower()
                                        + ContactTracingFeature.txCalibrationPowerToTxPower())));
    }

    @VisibleForTesting
    AdvertisementPacket generatePacket(BluetoothMetadata metadata) throws CryptoException {
        RollingProximityId rollingProximityId = proxIdManager.getCurrentRollingProximityId();
        Log.log.atInfo().log("Generating advertisement with version=%s, tx=%s",
                metadata.version(), metadata.txPower());
        // TODO: Consider store AEM generator to avoid recomputation of AEM key for 143 more times.
        AssociatedEncryptedMetadataGenerator aemGenerator =
                new AssociatedEncryptedMetadataGenerator(proxIdManager.getTemporaryExposureKey());
        AssociatedEncryptedMetadata encryptedMetadata =
                aemGenerator.encrypt(rollingProximityId, metadata);

        return new AdvertisementPacket.Builder()
                .setProxId(rollingProximityId.getDirect())
                .setRawMetadata(metadata.toBytes())
                .setEncryptedMetadata(encryptedMetadata.getDirect())
                .build();
    }

    @VisibleForTesting
    static byte createVersion() {
        if (ContactTracingFeature.advertisementMetadataV11()) {
            // bit 7:6 - major version
            // bit 5:4 - minor version
            // bit 3:2 - transmit power calibration confidence
            // bit 1:0 - reserved
            return (byte)
                    (VERSION_1_1
                            | ((ContactTracingFeature.calibrationConfidence()
                            << CALIBRATION_CONFIDENCE_BITS_OFFSET)
                            & CALIBRATION_CONFIDENCE_BITS_MASK));
        } else {
            // Bits 7:6 = major version (01)
            // Bits 5:4 = minor version (00)
            // Bits 3:0 = reserved (0000)
            return VERSION_1_0;
        }
    }
}