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

package com.google.samples.exposurenotification.testing;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.samples.exposurenotification.nearby.TemporaryExposureKey;
import com.google.samples.exposurenotification.data.AssociatedEncryptedMetadata;
import com.google.samples.exposurenotification.data.BluetoothMetadata;
import com.google.samples.exposurenotification.data.RollingProximityId;
import com.google.samples.exposurenotification.data.generator.AssociatedEncryptedMetadataGenerator;
import com.google.samples.exposurenotification.data.generator.AssociatedEncryptedMetadataHelper;
import com.google.samples.exposurenotification.features.ContactTracingFeature;

import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.io.BaseEncoding.base16;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class AssociatedEncryptedMetadataGeneratorTest {

    @Test
    public void decryptEncryptedBleMetadata_mustGetOriginalValue() throws Exception {
        RollingProximityId rollingProximityId =
                new RollingProximityId(base16().decode("AABBCCDDEEFF00112233445566778899"));
        TemporaryExposureKey temporaryExposureKey =
                new TemporaryExposureKey.TemporaryExposureKeyBuilder()
                        .setKeyData(base16().decode("FEDCBA9876543210FEDCBA9876543210"))
                        .setRollingStartIntervalNumber(0)
                        .build();
        BluetoothMetadata metadata = BluetoothMetadata.fromBytes(new byte[]{0x01, 0x02, 0x03, 0x04});

        AssociatedEncryptedMetadataGenerator generator =
                new AssociatedEncryptedMetadataGenerator(temporaryExposureKey);
        AssociatedEncryptedMetadata encryptedMetadata =
                generator.encrypt(rollingProximityId, metadata);

        assertEquals(generator.decrypt(rollingProximityId, encryptedMetadata), metadata);
    }

    @Test
    public void encrypt_usingTestVector() throws Exception {
        byte[] aemKey = TestVectors.get_AEMK();
        byte[] bleMetadata = TestVectors.get_BLE_METADATA();

        for (TestVectors.AdvertisedData element : TestVectors.ADVERTISED_DATA) {
            byte[] encryptedMetadata =
                    AssociatedEncryptedMetadataHelper.encryptOrDecrypt(
                            aemKey, element.get_RPI(), bleMetadata);
            assertArrayEquals(encryptedMetadata, element.get_AEM());
        }
    }

    @Test
    public void decrypt_usingTestVector() throws Exception {
        byte[] aemKey = TestVectors.get_AEMK();
        byte[] bleMetadata = TestVectors.get_BLE_METADATA();

        for (TestVectors.AdvertisedData element : TestVectors.ADVERTISED_DATA) {
            byte[] decryptedMetadata =
                    AssociatedEncryptedMetadataHelper.encryptOrDecrypt(
                            aemKey, element.get_RPI(), element.get_AEM());
            assertArrayEquals(decryptedMetadata, bleMetadata);
        }
    }

    // Runs the AEMK test vector on the spec:
    @Test
    public void generateAemKey_usingTestVector() throws Exception {
        byte[] aemKey =
                AssociatedEncryptedMetadataHelper.generateAemKey(
                        TestVectors.get_TEMPORARY_TRACING_KEY(),
                        ContactTracingFeature.aemkHkdfInfoString(),
                        ContactTracingFeature.associatedMetadataEncryptionKeySizeBytes());
        assertArrayEquals(aemKey, TestVectors.get_AEMK());
    }
}