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

package com.google.samples.exposurenotification.data;

import com.google.auto.value.AutoValue;
import com.google.samples.exposurenotification.ExposureNotificationEnums.CalibrationConfidence;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * The structured metadata sent and received via Bluetooth protocol. The actual payload that get
 * transmitted over Bluetooth is an encrypted copy of this class. See {@link
 * AssociatedEncryptedMetadata} and {@link
 * com.google.samples.exposurenotification.data.generator.AssociatedEncryptedMetadataGenerator}.
 */
@AutoValue
public abstract class BluetoothMetadata {
    public static final byte VERSION_1_0 = 0x40;
    public static final byte VERSION_1_1 = 0x50;

    public static final int CALIBRATION_CONFIDENCE_BITS_OFFSET = 2;
    public static final byte CALIBRATION_CONFIDENCE_BITS_MASK = (byte) 0b00001100;

    public static BluetoothMetadata create(byte version, byte txPower) {
        return new AutoValue_BluetoothMetadata(version, txPower);
    }

    // Bits 7:6 = major version
    // Bits 5:4 = minor version
    // Bits 3:2 = transmit power calibration confidence (added in v1.1)
    // Bits 1:0 = reserved
    public abstract byte version();

    public abstract byte txPower();

    public byte[] toBytes() {
        // Byte 0 = version
        // Byte 1 = TX power
        // Byte 2/3 = reserved
        return new byte[]{version(), txPower(), 0, 0};
    }

    public CalibrationConfidence calibrationConfidence() {
        CalibrationConfidence confidence =
                CalibrationConfidence.forNumber(
                        (version() & CALIBRATION_CONFIDENCE_BITS_MASK) >> CALIBRATION_CONFIDENCE_BITS_OFFSET);
        if (confidence == null) {
            confidence = CalibrationConfidence.LOWEST_CONFIDENCE;
        }
        return confidence;
    }

    /**
     * Parse the {@link BluetoothMetadata} from a raw byte array.
     *
     * @param rawMetadata the decrypted raw metadata from {@link AssociatedEncryptedMetadata}.
     */
    public static BluetoothMetadata fromBytes(byte[] rawMetadata) {
        checkArgument(rawMetadata.length == 4);
        return BluetoothMetadata.create(/*version=*/ rawMetadata[0], /*txPower=*/ rawMetadata[1]);
    }
}
