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

import com.google.samples.exposurenotification.Log;
import com.google.samples.exposurenotification.crypto.AesEcbEncryptor;
import com.google.samples.exposurenotification.crypto.CryptoException;
import com.google.samples.exposurenotification.crypto.KeyDerivation;
import com.google.samples.exposurenotification.features.ContactTracingFeature;

import java.security.NoSuchAlgorithmException;
import java.time.Instant;

import javax.crypto.Mac;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * A class to generate {@code RollingProximityId}(s).
 */
public class RollingProximityIdGenerator extends RollingProximityIdGeneratorBase {

    /**
     * Used primarily to avoid re-creating {@link AesEcbEncryptor} for performance.
     *
     * @deprecated Use {@link Factory} to create {@link RollingProximityIdGenerator} instances.
     */
    @Deprecated
    public RollingProximityIdGenerator(
            AesEcbEncryptor aesEcbEncryptor,
            byte[] keyData,
            int rollingStartIntervalNumber,
            int rollingEndIntervalNumber,
            int rollingProximityIdKeySizeBytes,
            String rpikHkdfInfoString,
            String rpidAesPaddedString)
            throws CryptoException {
        super(
                aesEcbEncryptor,
                keyData,
                rollingStartIntervalNumber,
                rollingEndIntervalNumber,
                rollingProximityIdKeySizeBytes,
                rpikHkdfInfoString,
                rpidAesPaddedString);
    }

    /**
     * Used primarily to avoid re-creating {@link Mac}, and {@link AesEcbEncryptor} for performance.
     */
    private RollingProximityIdGenerator(
            Mac mac,
            AesEcbEncryptor aesEcbEncryptor,
            byte[] keyData,
            int rollingStartIntervalNumber,
            int rollingEndIntervalNumber,
            int rollingProximityIdKeySizeBytes,
            byte[] rpikHkdfInfoBytes,
            byte[] rpidAesPaddedBytes,
            PaddedDataCache paddedDataCache)
            throws CryptoException {
        super(
                mac,
                aesEcbEncryptor,
                keyData,
                rollingStartIntervalNumber,
                rollingEndIntervalNumber,
                rollingProximityIdKeySizeBytes,
                rpikHkdfInfoBytes,
                rpidAesPaddedBytes,
                paddedDataCache);
    }

    /**
     * @deprecated Use {@link Factory} to create {@link RollingProximityIdGenerator} instances.
     */
    @Deprecated
    public RollingProximityIdGenerator(
            byte[] keyData,
            int rollingStartIntervalNumber,
            int rollingEndIntervalNumber,
            int rollingProximityIdKeySizeBytes,
            String rpikHkdfInfoString,
            String rpidAesPaddedString)
            throws CryptoException {
        this(
                AesEcbEncryptor.create(),
                keyData,
                rollingStartIntervalNumber,
                rollingEndIntervalNumber,
                rollingProximityIdKeySizeBytes,
                rpikHkdfInfoString,
                rpidAesPaddedString);
    }

    /**
     * Factory to create {@link RollingProximityIdGenerator}.
     */
    public static class Factory {
        private final Mac mac;
        private final PaddedDataCache paddedDataCache;

        public Factory() throws CryptoException {
            int startRollingEnIntervalNumber = getStartRollingEnIntervalNumber(Instant.now());
            int baseEnIntervalNumber =
                    startRollingEnIntervalNumber
                            - (int)
                            (ContactTracingFeature.contactRecordDaysToKeep()
                                    * ContactTracingFeature.tkRollingPeriodMultipleOfIdRollingPeriod());
            paddedDataCache =
                    PaddedDataCache.createInstance(
                            baseEnIntervalNumber,
                            (int) (ContactTracingFeature.contactRecordDaysToKeep() + 1),
                            ContactTracingFeature.rpidAesPaddedString().getBytes(UTF_8),
                            (int) ContactTracingFeature.tkRollingPeriodMultipleOfIdRollingPeriod());

            try {
                mac = Mac.getInstance(KeyDerivation.ALGORITHM_NAME);
            } catch (NoSuchAlgorithmException e) {
                throw new CryptoException(e);
            }
        }

        /**
         * Gets an instance of {@link RollingProximityIdGenerator}.
         */
        public RollingProximityIdGenerator getInstance(
                AesEcbEncryptor aesEcbEncryptor,
                byte[] keyData,
                int rollingStartIntervalNumber,
                int rollingEndIntervalNumber,
                int rollingProximityIdKeySizeBytes,
                String rpikHkdfInfoString,
                String rpidAesPaddedString)
                throws CryptoException {
            if (rollingEndIntervalNumber - rollingStartIntervalNumber > 144) {
                Log.log
                        .atWarning()
                        .log("RPI generator window is greater than 144. startIntervalNumber=%d,"
                                        + " endIntervalNumber=%d",
                                rollingStartIntervalNumber, rollingEndIntervalNumber);
            }
            return new RollingProximityIdGenerator(
                    mac,
                    aesEcbEncryptor,
                    keyData,
                    rollingStartIntervalNumber,
                    rollingEndIntervalNumber,
                    rollingProximityIdKeySizeBytes,
                    rpikHkdfInfoString.getBytes(UTF_8),
                    rpidAesPaddedString.getBytes(UTF_8),
                    paddedDataCache);
        }

        private static int getStartRollingEnIntervalNumber(Instant instant) {
            int tkRollingPeriod =
                    (int) ContactTracingFeature.tkRollingPeriodMultipleOfIdRollingPeriod();
            return tkRollingPeriod * ((int) (instant.toEpochMilli()
                    / MINUTES.toMillis(ContactTracingFeature.idRollingPeriodMinutes()))
                    / tkRollingPeriod);
        }
    }
}