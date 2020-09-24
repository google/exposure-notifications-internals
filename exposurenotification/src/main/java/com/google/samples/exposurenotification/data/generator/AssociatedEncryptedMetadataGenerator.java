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

import android.os.Build.VERSION_CODES;

import androidx.annotation.RequiresApi;

import com.google.samples.exposurenotification.nearby.TemporaryExposureKey;
import com.google.samples.exposurenotification.crypto.CryptoException;
import com.google.samples.exposurenotification.crypto.KeyDerivation;
import com.google.samples.exposurenotification.data.AssociatedEncryptedMetadata;
import com.google.samples.exposurenotification.data.BluetoothMetadata;
import com.google.samples.exposurenotification.data.RollingProximityId;
import com.google.samples.exposurenotification.features.ContactTracingFeature;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;

/**
 * The class to encrypt (from {@link BluetoothMetadata} to {@link AssociatedEncryptedMetadata}) and
 * decrypt (opposite) metadata.
 */
@RequiresApi(api = VERSION_CODES.KITKAT)
public class AssociatedEncryptedMetadataGenerator {
    private final byte[] aemKey;

    /**
     * Construct a {@link AssociatedEncryptedMetadataGenerator} with a {@link TemporaryExposureKey}
     * which is required to encrypt and decrypt metadata.
     *
     * @deprecated Use {@link Factory} to create {@link AssociatedEncryptedMetadataGenerator}
     * instances.
     */
    @Deprecated
    public AssociatedEncryptedMetadataGenerator(TemporaryExposureKey temporaryExposureKey)
            throws CryptoException {
        aemKey =
                AssociatedEncryptedMetadataHelper.generateAemKey(
                        temporaryExposureKey.getKeyData(),
                        ContactTracingFeature.aemkHkdfInfoString(),
                        (int) ContactTracingFeature.associatedMetadataEncryptionKeySizeBytes());
    }

    /**
     * Construct a {@link AssociatedEncryptedMetadataGenerator} with a {@link TemporaryExposureKey}
     * which is required to encrypt and decrypt metadata.
     */
    private AssociatedEncryptedMetadataGenerator(
            Mac mac, TemporaryExposureKey temporaryExposureKey, byte[] aemkHkdfInfoBytes)
            throws CryptoException {
        aemKey =
                AssociatedEncryptedMetadataHelper.generateAemKey(
                        mac,
                        temporaryExposureKey.getKeyData(),
                        aemkHkdfInfoBytes,
                        (int) ContactTracingFeature.associatedMetadataEncryptionKeySizeBytes());
    }

    /**
     * Encrypts BLE metadata using AES-CTR(AEMK, RPI, metadata).
     */
    public AssociatedEncryptedMetadata encrypt(
            RollingProximityId rollingProximityId, BluetoothMetadata metadata) throws CryptoException {
        return AssociatedEncryptedMetadataHelper.encrypt(rollingProximityId.get(), metadata, aemKey);
    }

    /**
     * Decrypts using AES-CTR(AEMK, RPI, encryptedData).
     */
    public BluetoothMetadata decrypt(
            RollingProximityId rollingProximityId, AssociatedEncryptedMetadata aem)
            throws CryptoException {
        return BluetoothMetadata.fromBytes(
                AssociatedEncryptedMetadataHelper.encryptOrDecrypt(
                        aemKey, rollingProximityId.getDirect(), aem.getDirect()));
    }

    /**
     * Factory to create {@link AssociatedEncryptedMetadataGenerator}.
     */
    public static class Factory {
        private final Mac mac;

        public Factory() throws CryptoException {
            try {
                mac = Mac.getInstance(KeyDerivation.ALGORITHM_NAME);
            } catch (NoSuchAlgorithmException e) {
                throw new CryptoException(e);
            }
        }

        /**
         * Gets an instance of {@link AssociatedEncryptedMetadataGenerator}.
         */
        public AssociatedEncryptedMetadataGenerator getInstance(
                TemporaryExposureKey temporaryExposureKey) throws CryptoException {
            return new AssociatedEncryptedMetadataGenerator(
                    mac,
                    temporaryExposureKey,
                    ContactTracingFeature.aemkHkdfInfoString().getBytes(StandardCharsets.UTF_8));
        }
    }
}