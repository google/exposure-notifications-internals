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

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.common.collect.ImmutableList;
import com.google.samples.exposurenotification.crypto.AesEcbEncryptor;
import com.google.samples.exposurenotification.crypto.CryptoException;
import com.google.samples.exposurenotification.crypto.KeyDerivation;
import com.google.samples.exposurenotification.data.GeneratedRollingProximityId;
import com.google.samples.exposurenotification.data.RollingProximityId;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import javax.crypto.Mac;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A class to generate {@link RollingProximityId}(s).
 */
public class RollingProximityIdGeneratorBase {
    private static final int AES_BLOCK_SIZE = 16;
    private final int rollingStartIntervalNumber;
    private final int rollingEndIntervalNumber;
    private final AesEcbEncryptor encryptor;
    private final byte[] aesPadding;
    @Nullable
    private PaddedDataCache paddedDataCache;

    /**
     * Used primarily to avoid re-creating {@link AesEcbEncryptor} for performance.
     *
     * @deprecated use {@link #RollingProximityIdGeneratorBase(Mac, AesEcbEncryptor, byte[], int, int,
     * int, byte[], byte[], PaddedDataCache)} instead.
     */
    @Deprecated
    @SuppressWarnings("NewApi")
    public RollingProximityIdGeneratorBase(
            AesEcbEncryptor aesEcbEncryptor,
            byte[] keyData,
            int rollingStartIntervalNumber,
            int rollingEndIntervalNumber,
            int rollingProximityIdKeySizeBytes,
            String rpikHkdfInfoString,
            String rpidAesPaddedString)
            throws CryptoException {
        this.rollingStartIntervalNumber = rollingStartIntervalNumber;
        this.rollingEndIntervalNumber = rollingEndIntervalNumber;
        aesPadding = rpidAesPaddedString.getBytes(UTF_8);
        encryptor = aesEcbEncryptor;
        encryptor.init(generateRpiKey(keyData, rpikHkdfInfoString, rollingProximityIdKeySizeBytes));
    }

    /**
     * Used primarily to avoid re-creating {@link Mac}, and {@link AesEcbEncryptor} for performance.
     */
    @SuppressWarnings("NewApi")
    public RollingProximityIdGeneratorBase(
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
        this.rollingStartIntervalNumber = rollingStartIntervalNumber;
        this.rollingEndIntervalNumber = rollingEndIntervalNumber;
        this.paddedDataCache = paddedDataCache;
        aesPadding = rpidAesPaddedBytes;
        encryptor = aesEcbEncryptor;
        encryptor.init(generateRpiKey(mac, keyData, rpikHkdfInfoBytes, rollingProximityIdKeySizeBytes));
    }

    public RollingProximityIdGeneratorBase(
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
     * Generates a list of rolling proximity ids.
     */
    @SuppressWarnings("NewApi")
    public ImmutableList<GeneratedRollingProximityId> generateIds(byte[] reusedOutput)
            throws CryptoException {
        int numIds = rollingEndIntervalNumber - rollingStartIntervalNumber;
        byte[] paddedData = null;
        if (paddedDataCache != null && numIds <= paddedDataCache.getIdsPerKey()) {
            paddedData = paddedDataCache.getCachedData(rollingStartIntervalNumber);
        }
        if (paddedData == null) {
            paddedData = generateAggregatePaddedData(rollingStartIntervalNumber, numIds, aesPadding);
        }
        return convertToList(encryptor.encrypt(paddedData, reusedOutput), numIds);
    }

    /**
     * Generates the Rolling Proximity Id key by the given temporary exposure key.
     */
    @VisibleForTesting
    @SuppressWarnings("NewApi")
    static byte[] generateRpiKey(
            byte[] temporaryExposureKey, String rpikHkdfInfoString, int rollingProximityIdKeySizeBytes)
            throws CryptoException {
        // RPIK = HKDF(tek, NULL, UTF8("EN-RPIK"), 16).
        return KeyDerivation.hkdfSha256(
                temporaryExposureKey,
                /* inputSalt =*/ null,
                rpikHkdfInfoString.getBytes(UTF_8),
                rollingProximityIdKeySizeBytes);
    }

    /**
     * Generates the Rolling Proximity Id key by the given temporary exposure key.
     */
    @SuppressWarnings("NewApi")
    private static byte[] generateRpiKey(
            Mac mac,
            byte[] temporaryExposureKey,
            byte[] rpikHkdfInfoBytes,
            int rollingProximityIdKeySizeBytes)
            throws CryptoException {
        // RPIK = HKDF(tek, NULL, UTF8("EN-RPIK"), 16).
        return KeyDerivation.hkdfSha256(
                mac,
                temporaryExposureKey,
                /* inputSalt =*/ null,
                rpikHkdfInfoBytes,
                rollingProximityIdKeySizeBytes);
    }

    /**
     * Generates a single rolling proximity id using AES(RPIK, PaddedData).
     *
     * @param intervalNumber index of 10-minute interval since Epoch.
     */
    public RollingProximityId generateId(int intervalNumber) throws CryptoException {
        return new RollingProximityId(
                encryptor.encrypt(generatePaddedData(intervalNumber, aesPadding)), /*takeOwnership=*/ true);
    }

    private ImmutableList<GeneratedRollingProximityId> convertToList(
            byte[] rawRollingProximityIds, int numIds) {
        ImmutableList.Builder<GeneratedRollingProximityId> generatedIds = ImmutableList.builder();
        for (int i = 0; i < numIds; ++i) {
            generatedIds.add(
                    GeneratedRollingProximityId.create(
                            new RollingProximityId(
                                    Arrays.copyOfRange(
                                            rawRollingProximityIds, i * AES_BLOCK_SIZE, (i + 1) * AES_BLOCK_SIZE),
                                    true),
                            rollingStartIntervalNumber + i));
        }
        return generatedIds.build();
    }

    private static byte[] generatePaddedData(int intervalNumber, byte[] aesPadding) {
        ByteBuffer paddedData = ByteBuffer.allocate(AES_BLOCK_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        return addPaddedData(paddedData, intervalNumber, aesPadding).array();
    }

    protected static byte[] generateAggregatePaddedData(
            int startIntervalNumber, int numIds, byte[] aesPadding) {
        ByteBuffer allPaddedData =
                ByteBuffer.allocate(AES_BLOCK_SIZE * numIds).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < numIds; ++i) {
            allPaddedData = addPaddedData(allPaddedData, startIntervalNumber + i, aesPadding);
        }
        return allPaddedData.array();
    }

    /**
     * Padded data is the following sequence of 16 bytes:
     *
     * <ol>
     *   <li>PaddedData[0 - 5]: "EN-RPI".getBytes(UTF_8).
     *   <li>PaddedData[6 - 11]: 0x000000000000.
     *   <li>PaddedData[12 - 15]: intervalNumber, uint32 little-endian.
     * </ol>
     *
     * <p>{@code buffer} must be zero filled.
     */
    private static ByteBuffer addPaddedData(
            ByteBuffer buffer, int intervalNumber, byte[] aesPadding) {
        buffer
                .put(aesPadding)
                .position(buffer.position() + AES_BLOCK_SIZE - aesPadding.length - 4); // 4 is Integer.BYTES
        buffer.putInt(intervalNumber);
        return buffer;
    }

    static class PaddedDataCache {
        private final int baseEnIntervalNumber;
        private final int size;
        private final int idsPerKey;
        private final byte[][] cachedData;

        public static PaddedDataCache createInstance(
                int baseEnIntervalNumber, int cacheSize, byte[] aesPadding, int idsPerKey) {
            return new PaddedDataCache(
                    createPaddedDataCache(baseEnIntervalNumber, cacheSize, aesPadding, idsPerKey),
                    baseEnIntervalNumber,
                    cacheSize,
                    idsPerKey);
        }

        private PaddedDataCache(
                byte[][] cachedData, int baseEnIntervalNumber, int size, int idsPerKey) {
            this.baseEnIntervalNumber = baseEnIntervalNumber;
            this.cachedData = cachedData;
            this.size = size;
            this.idsPerKey = idsPerKey;
        }

        int getSize() {
            return size;
        }

        int getIdsPerKey() {
            return idsPerKey;
        }

        @Nullable
        public byte[] getCachedData(int enIntervalNumber) {
            int index = getIndex(enIntervalNumber, baseEnIntervalNumber);
            return (index >= 0 && index < getSize()) ? cachedData[index] : null;
        }

        private int getIndex(int enIntervalNumber, int baseEnIntervalNumber) {
            // The given enIntervalNumber should be aligned to idsPerKey.
            return (((enIntervalNumber - baseEnIntervalNumber) % idsPerKey) == 0)
                    ? ((enIntervalNumber - baseEnIntervalNumber) / idsPerKey)
                    : -1;
        }

        private static byte[][] createPaddedDataCache(
                int baseEnIntervalNumber, int size, byte[] aesPadding, int idsPerKey) {
            byte[][] paddedDataSet = new byte[size][];
            for (int i = 0, intervalNumber = baseEnIntervalNumber;
                 i < size;
                 i++, intervalNumber += idsPerKey) {
                paddedDataSet[i] = generateAggregatePaddedData(intervalNumber, idsPerKey, aesPadding);
            }
            return paddedDataSet;
        }
    }
}