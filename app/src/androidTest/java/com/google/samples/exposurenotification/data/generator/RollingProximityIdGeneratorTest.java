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

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.samples.exposurenotification.TemporaryExposureKey;
import com.google.samples.exposurenotification.crypto.AesEcbEncryptor;
import com.google.samples.exposurenotification.data.DayNumber;
import com.google.samples.exposurenotification.data.GeneratedRollingProximityId;
import com.google.samples.exposurenotification.data.RollingProximityId;
import com.google.samples.exposurenotification.data.TemporaryExposureKeySupport;
import com.google.samples.exposurenotification.data.generator.RollingProximityIdGeneratorBase.PaddedDataCache;
import com.google.samples.exposurenotification.features.ContactTracingFeature;
import com.google.samples.exposurenotification.testing.TestVectors;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class RollingProximityIdGeneratorTest {

    @Test
    public void generateId_usingTestVector() throws Exception {
        byte[] temporaryTracingKey = TestVectors.get_TEMPORARY_TRACING_KEY();
        TemporaryExposureKey temporaryExposureKey =
                new TemporaryExposureKey.TemporaryExposureKeyBuilder()
                        .setRollingStartIntervalNumber(0)
                        .setKeyData(temporaryTracingKey)
                        .build();
        int ctIntervalNumber = TestVectors.CTINTERVAL_NUMBER_OF_GENERATED_KEY;
        RollingProximityIdGenerator generator =
                new RollingProximityIdGenerator(
                        temporaryExposureKey.getKeyData(),
                        temporaryExposureKey.getRollingStartIntervalNumber(),
                        TemporaryExposureKeySupport.getRollingEndIntervalNumber(temporaryExposureKey),
                        ContactTracingFeature.rollingProximityIdKeySizeBytes(),
                        ContactTracingFeature.rpikHkdfInfoString(),
                        ContactTracingFeature.rpidAesPaddedString());
        for (TestVectors.AdvertisedData element : TestVectors.ADVERTISED_DATA) {
            RollingProximityId generatedId = generator.generateId(ctIntervalNumber);
            assertArrayEquals(generatedId.get(), element.get_RPI());
            ctIntervalNumber++;
        }
    }

    @Test
    public void testGenerateIds_generatesCorrectIds() throws Exception {
        TemporaryExposureKey temporaryExposureKey =
                new TemporaryExposureKey.TemporaryExposureKeyBuilder()
                        .setRollingStartIntervalNumber(TestVectors.CTINTERVAL_NUMBER_OF_GENERATED_KEY)
                        .setRollingPeriod(TestVectors.ADVERTISED_DATA.size())
                        .setKeyData(TestVectors.get_TEMPORARY_TRACING_KEY())
                        .build();

        List<GeneratedRollingProximityId> generatedIds =
                new RollingProximityIdGenerator(
                        temporaryExposureKey.getKeyData(),
                        temporaryExposureKey.getRollingStartIntervalNumber(),
                        TemporaryExposureKeySupport.getRollingEndIntervalNumber(temporaryExposureKey),
                        ContactTracingFeature.rollingProximityIdKeySizeBytes(),
                        ContactTracingFeature.rpikHkdfInfoString(),
                        ContactTracingFeature.rpidAesPaddedString())
                        .generateIds(
                                new byte
                                        [ContactTracingFeature.tkRollingPeriodMultipleOfIdRollingPeriod()
                                        * ContactTracingFeature.contactIdLength()]);

        assertEquals(generatedIds.size(), TestVectors.ADVERTISED_DATA.size());
        for (int i = 0; i < TestVectors.ADVERTISED_DATA.size(); i++) {
            assertArrayEquals(generatedIds.get(i).rollingProximityId().get(),
                    TestVectors.ADVERTISED_DATA.get(i).get_RPI());
        }
    }

    // Runs the RPIK test vector on the spec:
    @Test
    public void generateRpiKey_usingTestVector() throws Exception {
        byte[] rpiKey =
                RollingProximityIdGenerator.generateRpiKey(
                        TestVectors.get_TEMPORARY_TRACING_KEY(),
                        TestVectors.get_RPIK_HKDF_INFO_STRING(),
                        ContactTracingFeature.rollingProximityIdKeySizeBytes());
        assertArrayEquals(rpiKey, TestVectors.get_RPIK());
    }

    @Test
    public void getCachedData_sameEnIntervalNumber_sameAsDirectlyGenerated() {
        int idsPerKey =
                ContactTracingFeature.tkRollingPeriodMultipleOfIdRollingPeriod();
        int baseEnIntervalNumber =
                TestVectors.CTINTERVAL_NUMBER_OF_GENERATED_KEY
                        - (ContactTracingFeature.contactRecordDaysToKeep() * idsPerKey);
        int cacheSize = ContactTracingFeature.contactRecordDaysToKeep() + 1;
        byte[] aesPadding = ContactTracingFeature.rpidAesPaddedString().getBytes(UTF_8);

        PaddedDataCache paddedDataCache =
                PaddedDataCache.createInstance(baseEnIntervalNumber, cacheSize, aesPadding, idsPerKey);

        for (int i = 0, testEnIntervalNumber = baseEnIntervalNumber;
             i < cacheSize;
             i++, testEnIntervalNumber += idsPerKey) {
            assertArrayEquals(paddedDataCache.getCachedData(testEnIntervalNumber),
                    RollingProximityIdGeneratorBase.generateAggregatePaddedData(
                            testEnIntervalNumber, idsPerKey, aesPadding));
        }
    }

    @Test
    public void getCachedData_notAlignedEnIntervalNumber_mustReturnNull() {
        int idsPerKey =
                ContactTracingFeature.tkRollingPeriodMultipleOfIdRollingPeriod();
        int baseEnIntervalNumber =
                TestVectors.CTINTERVAL_NUMBER_OF_GENERATED_KEY
                        - (ContactTracingFeature.contactRecordDaysToKeep() * idsPerKey);
        int cacheSize = ContactTracingFeature.contactRecordDaysToKeep() + 1;
        byte[] aesPadding = ContactTracingFeature.rpidAesPaddedString().getBytes(UTF_8);

        PaddedDataCache paddedDataCache =
                PaddedDataCache.createInstance(baseEnIntervalNumber, cacheSize, aesPadding, idsPerKey);

        assertEquals(paddedDataCache.getCachedData(baseEnIntervalNumber + 1), null);
        assertEquals(paddedDataCache.getCachedData(
                TestVectors.CTINTERVAL_NUMBER_OF_GENERATED_KEY - 1), null);
    }

    @Test
    public void getCachedData_outOfRangeEnIntervalNumber_mustReturnNull() {
        int idsPerKey =
                ContactTracingFeature.tkRollingPeriodMultipleOfIdRollingPeriod();
        int baseEnIntervalNumber =
                TestVectors.CTINTERVAL_NUMBER_OF_GENERATED_KEY
                        - (ContactTracingFeature.contactRecordDaysToKeep() * idsPerKey);
        int cacheSize = ContactTracingFeature.contactRecordDaysToKeep() + 1;
        byte[] aesPadding = ContactTracingFeature.rpidAesPaddedString().getBytes(UTF_8);

        PaddedDataCache paddedDataCache =
                PaddedDataCache.createInstance(baseEnIntervalNumber, cacheSize, aesPadding, idsPerKey);

        assertEquals(paddedDataCache.getCachedData(baseEnIntervalNumber - idsPerKey), null);
        assertEquals(
                paddedDataCache.getCachedData(
                        TestVectors.CTINTERVAL_NUMBER_OF_GENERATED_KEY + idsPerKey), null);
    }

    @Test
    public void generateIds_numIdsTooLarge_shouldNotCrash() throws Exception {
        TemporaryExposureKey temporaryExposureKey =
                new TemporaryExposureKey.TemporaryExposureKeyBuilder()
                        .setRollingStartIntervalNumber(0)
                        .setRollingPeriod(145)
                        .setKeyData(
                                TemporaryExposureKeySupport.getMinKey(DayNumber.MIN_DAY_NUMBER).getKeyData())
                        .setTransmissionRiskLevel(1)
                        .build();

        List<GeneratedRollingProximityId> generatedIds =
                new RollingProximityIdGenerator.Factory()
                        .getInstance(
                                AesEcbEncryptor.create(),
                                temporaryExposureKey.getKeyData(),
                                temporaryExposureKey.getRollingStartIntervalNumber(),
                                TemporaryExposureKeySupport.getRollingEndIntervalNumber(temporaryExposureKey),
                                ContactTracingFeature.rollingProximityIdKeySizeBytes(),
                                ContactTracingFeature.rpikHkdfInfoString(),
                                ContactTracingFeature.rpidAesPaddedString())
                        .generateIds(
                                new byte[145 * ContactTracingFeature.contactIdLength()]);
        assertEquals(generatedIds.size(), 145);
    }
}