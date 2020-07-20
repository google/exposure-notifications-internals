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

package com.google.samples.exposurenotification.data.generator;

import com.google.samples.exposurenotification.crypto.AesCtrEncryptor;
import com.google.samples.exposurenotification.crypto.CryptoException;
import com.google.samples.exposurenotification.crypto.KeyDerivation;
import com.google.samples.exposurenotification.data.AssociatedEncryptedMetadata;
import com.google.samples.exposurenotification.data.BluetoothMetadata;

import javax.crypto.Mac;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Helper class for the {@link AssociatedEncryptedMetadataGenerator} .
 */
public class AssociatedEncryptedMetadataHelper {

    /**
     * Generates the Associated Encryption Metadata key by the given temporary exposure key.
     *
     * @deprecated use {@link #generateAemKey(Mac, byte[], byte[], int)} instead.
     */
    @Deprecated
    @SuppressWarnings("NewApi")
    public static byte[] generateAemKey(
            byte[] temporaryExposureKey,
            String aemkHkdfInfoString,
            int associatedMetadataEncryptionKeySizeBytes)
            throws CryptoException {
        // AEMK = HKDF(tek, NULL, UTF8("EN-AEMK"), 16).
        return KeyDerivation.hkdfSha256(
                temporaryExposureKey,
                /* inputSalt =*/ null,
                aemkHkdfInfoString.getBytes(UTF_8),
                associatedMetadataEncryptionKeySizeBytes);
    }

    /**
     * Generates the Associated Encryption Metadata key by the given temporary exposure key.
     */
    @SuppressWarnings("NewApi")
    public static byte[] generateAemKey(
            Mac mac,
            byte[] temporaryExposureKey,
            byte[] aemkHkdfInfoBytes,
            int associatedMetadataEncryptionKeySizeBytes)
            throws CryptoException {
        // AEMK = HKDF(tek, NULL, UTF8("EN-AEMK"), 16).
        return KeyDerivation.hkdfSha256(
                mac,
                temporaryExposureKey,
                /* inputSalt =*/ null,
                aemkHkdfInfoBytes,
                associatedMetadataEncryptionKeySizeBytes);
    }

    /**
     * Encrypt BLE metadata using AES-CTR(AEMK, RPI, metadata).
     */
    public static byte[] encryptOrDecrypt(
            byte[] aemKey, byte[] rollingProximityId, byte[] bluetoothMetadataAsBytes)
            throws CryptoException {
        return AesCtrEncryptor.aesCtr(aemKey, rollingProximityId, bluetoothMetadataAsBytes);
    }

    /**
     * Create an {@link AssociatedEncryptedMetadata} encrypted via {@code aemKey}.
     */
    public static AssociatedEncryptedMetadata encrypt(
            byte[] rollingProximityIdAsBytes, BluetoothMetadata metadata, byte[] aemKey)
            throws CryptoException {
        return AssociatedEncryptedMetadata.create(
                AssociatedEncryptedMetadataHelper.encryptOrDecrypt(
                        aemKey, rollingProximityIdAsBytes, metadata.toBytes()));
    }

    private AssociatedEncryptedMetadataHelper() {
    }
}