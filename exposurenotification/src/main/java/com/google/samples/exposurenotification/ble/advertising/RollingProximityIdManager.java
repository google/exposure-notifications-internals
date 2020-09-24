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

import androidx.annotation.Nullable;
import androidx.core.util.Supplier;

import com.google.common.base.Preconditions;
import com.google.samples.exposurenotification.Log;
import com.google.samples.exposurenotification.nearby.TemporaryExposureKey;
import com.google.samples.exposurenotification.ble.interfaces.BleDatabaseWriter;
import com.google.samples.exposurenotification.ble.utils.Constants;
import com.google.samples.exposurenotification.crypto.CryptoException;
import com.google.samples.exposurenotification.data.RollingProximityId;
import com.google.samples.exposurenotification.data.TemporaryExposureKeySupport;
import com.google.samples.exposurenotification.data.generator.RollingProximityIdGenerator;
import com.google.samples.exposurenotification.data.generator.TemporaryExposureKeyGenerator;
import com.google.samples.exposurenotification.features.ContactTracingFeature;
import com.google.samples.exposurenotification.storage.SelfTemporaryExposureKeyDataStore;

import java.time.Instant;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * Generate rolling proximity identifiers for BLE advertising, using v1.3.
 */
public class RollingProximityIdManager {
    private final BleDatabaseWriter bleDatabaseWriter;
    private final Supplier<Long> currentTimeMsSupplier;
    @Nullable
    private final SelfTemporaryExposureKeyDataStore selfTemporaryExposureKeyDataStore;
    private int latestEnIntervalNumber;
    @Nullable
    private TemporaryExposureKey temporaryExposureKey;
    @Nullable
    private RollingProximityId latestRollingProximityId;

    public RollingProximityIdManager(
            @Nullable SelfTemporaryExposureKeyDataStore selfTemporaryExposureKeyDataStore,
            BleDatabaseWriter bleDatabaseWriter,
            Supplier<Long> currentTimeMsSupplier) {
        this.selfTemporaryExposureKeyDataStore = selfTemporaryExposureKeyDataStore;
        this.bleDatabaseWriter = bleDatabaseWriter;
        this.currentTimeMsSupplier = currentTimeMsSupplier;
    }

    public RollingProximityIdManager(
            @Nullable SelfTemporaryExposureKeyDataStore selfTemporaryExposureKeyDataStore,
            BleDatabaseWriter bleDatabaseWriter,
            Supplier<Long> currentTimeMsSupplier,
            TemporaryExposureKey temporaryExposureKey) {
        this.selfTemporaryExposureKeyDataStore = selfTemporaryExposureKeyDataStore;
        this.bleDatabaseWriter = bleDatabaseWriter;
        this.currentTimeMsSupplier = currentTimeMsSupplier;
        this.temporaryExposureKey = temporaryExposureKey;
    }

    /**
     * Gets the rolling proximity id for current time (given instant).
     */
    public synchronized RollingProximityId getCurrentRollingProximityId() throws CryptoException {
        Instant instant = Instant.ofEpochMilli(currentTimeMsSupplier.get());
        int currentEnIntervalNumber = getCurrentEnIntervalNumber(instant);
        if (latestRollingProximityId != null && currentEnIntervalNumber == latestEnIntervalNumber) {
            Log.log
                    .atInfo()
                    .log(
                            "getCurrentRollingProximityId: same latestEnIntervalNumber=%d",
                            latestEnIntervalNumber);
            return verifyNotNull(latestRollingProximityId);
        }

        Log.log
                .atInfo()
                .log(
                        "getCurrentRollingProximityId: current/latestEnIntervalNumber=%d/%d",
                        currentEnIntervalNumber, latestEnIntervalNumber);
        //
        if (temporaryExposureKey == null && selfTemporaryExposureKeyDataStore != null) {
            TemporaryExposureKey existingKey =
                    selfTemporaryExposureKeyDataStore.getSingleKeyForIntervalNumber(currentEnIntervalNumber);
            // Keep the nullness checker happy, by never assigning null to temporaryExposureKey.
            if (existingKey != null) {
                temporaryExposureKey = existingKey;
            }
        }
        if (temporaryExposureKey == null
                || currentEnIntervalNumber
                >= TemporaryExposureKeySupport.getRollingEndIntervalNumber(temporaryExposureKey)) {
            temporaryExposureKey =
                    TemporaryExposureKeyGenerator.generateKey(
                            getAlignedEnIntervalNumber(currentEnIntervalNumber));
            Log.log.atInfo().log("getCurrentRollingProximityId: generated a new Temporary Exposure Key");
            Log.log
                    .atVerbose()
                    .log(
                            "getCurrentRollingProximityId: TEK=%s",
                            Constants.fromIdByteArrayToString(verifyNotNull(temporaryExposureKey).getKeyData()));
            bleDatabaseWriter.writeNewTemporaryExposureKey(verifyNotNull(temporaryExposureKey));
        }

        verifyNotNull(temporaryExposureKey);

        // Generates a new Rolling Proximity Id.
        latestEnIntervalNumber = currentEnIntervalNumber;
        latestRollingProximityId =
                new RollingProximityIdGenerator(
                        temporaryExposureKey.getKeyData(),
                        temporaryExposureKey.getRollingStartIntervalNumber(),
                        TemporaryExposureKeySupport.getRollingEndIntervalNumber(temporaryExposureKey),
                        (int) ContactTracingFeature.rollingProximityIdKeySizeBytes(),
                        ContactTracingFeature.rpikHkdfInfoString(),
                        ContactTracingFeature.rpidAesPaddedString())
                        .generateId(latestEnIntervalNumber);
        Log.log
                .atInfo()
                .log(
                        "getCurrentRollingProximityId: generated a new RollingProximityId=%s",
                        latestRollingProximityId);
        return verifyNotNull(latestRollingProximityId);
    }

    synchronized TemporaryExposureKey getTemporaryExposureKey() {
        // getCurrentRollingProximityId() must always be called before this method
        Preconditions.checkNotNull(temporaryExposureKey, "Must call getCurrentRollingProximityId()");
        return temporaryExposureKey;
    }

    private static int getCurrentEnIntervalNumber(Instant instant) {
        // TODO: Make sure using ceil or floor.
        return (int) (instant.toEpochMilli() /
                MINUTES.toMillis(ContactTracingFeature.idRollingPeriodMinutes()));
    }

    private static int getAlignedEnIntervalNumber(int enIntervalNumber) {
        int tkRollingPeriod = (int) ContactTracingFeature.tkRollingPeriodMultipleOfIdRollingPeriod();
        return tkRollingPeriod * (enIntervalNumber / tkRollingPeriod);
    }

    public synchronized void clearCache() {
        Log.log.atInfo().log("RollingProximityIdManager.clearCache");
        latestEnIntervalNumber = 0;
        temporaryExposureKey = null;
        latestRollingProximityId = null;
    }
}