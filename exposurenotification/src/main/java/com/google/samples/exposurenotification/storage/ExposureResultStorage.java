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

package com.google.samples.exposurenotification.storage;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.samples.exposurenotification.Log;
import com.google.samples.exposurenotification.TemporaryExposureKey;
import com.google.samples.exposurenotification.data.DayNumber;
import com.google.samples.exposurenotification.features.ContactTracingFeature;
import com.google.samples.exposurenotification.storage.Encoder.SerialEncoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Storage database that stores {@link ExposureResult}s.
 *
 * <p>The store is keyed by PackageNameHash:Signature:TokenHash:TemporaryExposureKey. See {@link
 * KeyEncoder} for the details of the encoding.
 */
public class ExposureResultStorage implements AutoCloseable {

    private final List<Row> transactionRows = new ArrayList<>();
    private final Map<byte[], byte[]> threadSafeLevelDb = new HashMap<>();

    public static ExposureResultStorage open(Context context) throws StorageException {
        return new ExposureResultStorage(context);
    }

    private ExposureResultStorage(Context context) throws StorageException {
    }

    /**
     * Checks whether there's a {@link ExposureResult} calculated for the specified input already.
     *
     * <p>See {@link Row} for parameter definitions.
     *
     * @deprecated use hasResult(byte[], byte[]) instead.
     */
    @Deprecated
    public boolean hasResult(
            String packageName,
            byte[] signatureHash,
            String token,
            TemporaryExposureKey temporaryExposureKey) {
        Preconditions.checkArgument(signatureHash.length == 32, "Signature hash not Sha256 length.");
        return threadSafeLevelDb.get(
                new KeyEncoder(packageName, signatureHash, token, temporaryExposureKey.getKeyData())
                        .encode())
                != null;
    }

    /**
     * Checks whether there's a {@link ExposureResult} calculated for the specified input already.
     *
     * <p>See {@link Row} for parameter definitions.
     */
    public boolean hasResult(byte[] tokenRoot, byte[] exposureKey) {
        for (Row row : transactionRows) {
            if (row.tokenRoot() != null
                    && Arrays.equals(row.tokenRoot(), tokenRoot)
                    && Arrays.equals(row.key().getKeyData(), exposureKey)) {
                return true;
            }
        }
        return threadSafeLevelDb.get(new KeyEncoder(tokenRoot, exposureKey).encode()) != null;
    }

    /**
     * Stores a single result. Returns true if successfully stored, false otherwise.
     *
     * @deprecated use {@link #storeResult(byte[], TemporaryExposureKey, ExposureResult, boolean)} instead.
     */
    @Deprecated
    public boolean storeResult(Row row) {
        threadSafeLevelDb.put(new KeyEncoder(row).encode(), row.exposureResult().toByteArray());
        return true;
    }

    /**
     * Stores a single result. Returns true if successfully stored, false otherwise.
     */
    public boolean storeResult(
            byte[] tokenRoot,
            TemporaryExposureKey exposureKey,
            ExposureResult exposureResult,
            boolean holdForTransaction) {
        if (holdForTransaction) {
            transactionRows.add(
                    Row.builder()
                            .setTokenRoot(tokenRoot)
                            .setKey(exposureKey)
                            .setExposureResult(exposureResult)
                            .build());
            return true;
        } else {
            threadSafeLevelDb.put(
                    new KeyEncoder(tokenRoot, exposureKey.getKeyData()).encode(),
                    exposureResult.toByteArray());
            return true;
        }
    }

    /**
     * Commits {@link #storeResult} requests when processed using holdForTransaction, completing the
     * write.
     */
    public void commitStoreResultRequestsForTransaction() {
        storeResults(transactionRows);
        transactionRows.clear();
    }

    /**
     * Stores list of results. Returns true if succeeds, false otherwise.
     */
    public boolean storeResults(List<Row> results) {
        synchronized (threadSafeLevelDb) {
            for (Row row : results) {
                byte[] tokenRoot = row.tokenRoot();
                KeyEncoder keyEncoder =
                        tokenRoot != null
                                ? new KeyEncoder(tokenRoot, row.key().getKeyData())
                                : new KeyEncoder(row);
                threadSafeLevelDb.put(keyEncoder.encode(), row.exposureResult().toByteArray());
            }
            return true;
        }
    }

    /**
     * Returns an {@link ExposureResult} calculated for the specified input already.
     *
     * <p>See {@link Row} for parameter definitions.
     */
    public ExposureResult getResult(byte[] tokenRoot, byte[] exposureKey) {
        try {
            byte[] result = threadSafeLevelDb.get(new KeyEncoder(tokenRoot, exposureKey).encode());
            if (result != null) {
                return ExposureResult.parseFrom(result);
            }
        } catch (InvalidProtocolBufferException e) {
            Log.log.atSevere().withCause(e).log("Error checking for result.");
        }
        return ExposureResult.getDefaultInstance();
    }

    /**
     * Gets the exposure results for specified package and token.
     */
    public List<ExposureResult> getAll(String packageName, byte[] signatureHash, String token)
            throws StorageException {
        Preconditions.checkArgument(signatureHash.length == 32, "Signature hash not Sha256 length.");
        byte[] requestKeyRoot = new TokenRootEncoder(packageName, signatureHash, token).encode();

        ImmutableList.Builder<ExposureResult> exposureResultsBuilder = ImmutableList.builder();
        synchronized (threadSafeLevelDb) {
            for (Entry<byte[], byte[]> iterator : threadSafeLevelDb.entrySet()) {
                if (EncodingComparisonHelper.hasRoot(requestKeyRoot, iterator.getKey())) {
                    try {
                        exposureResultsBuilder.add(ExposureResult.parseFrom(iterator.getValue()));
                    } catch (InvalidProtocolBufferException e) {
                        Log.log.atSevere().withCause(e).log("Unable to parse exposure result for key.");
                    }
                }

            }
        }
        return exposureResultsBuilder.build();
    }

    /**
     * Delete results associated with provided package. Returns number of items deleted.
     */
    public int deleteAll(String packageName, byte[] signatureHash) {
        Preconditions.checkState(signatureHash.length == 32, "Signature hash not Sha256 length.");
        byte[] packageKeyRoot = new PackageRootEncoder(packageName, signatureHash).encode();
        int deleted = 0;
        synchronized (threadSafeLevelDb) {
            Iterator<Entry<byte[], byte[]>> iterator = threadSafeLevelDb.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<byte[], byte[]> item = iterator.next();
                if (EncodingComparisonHelper.hasRoot(packageKeyRoot, item.getKey())) {
                    threadSafeLevelDb.remove(item.getKey());
                }
            }
        }
        return deleted;
    }

    /**
     * Deletes all {@link ExposureResult}s in store.
     */
    public int deleteAll() {
        final int keysPurged;
        synchronized (threadSafeLevelDb) {
            keysPurged = threadSafeLevelDb.size();
            threadSafeLevelDb.clear();
        }
        return keysPurged;
    }

    /**
     * Delete results up to and including {@code lastDayToDelete}.
     *
     * <p>Returns number of results purged.
     */
    public int deletePrior(DayNumber lastDayToDelete) {
        // FIXME: Delete prescribed results.
        return 0;
    }

    @Override
    public void close() {
        /* nothing to close */
    }

    /**
     * A row in {@link ExposureResultStorage}.
     */
    @AutoValue
    public abstract static class Row {

        /**
         * Builder for {@link Row}.
         */
        @AutoValue.Builder
        public abstract static class Builder {

            public Row build() {
                Row row = autoBuild();
                byte[] signatureHash = row.signatureHash();
                if (signatureHash != null) {
                    Preconditions.checkState(signatureHash.length == 32, "Signature hash not Sha256 length.");
                }
                return row;
            }

            abstract Row autoBuild();

            public abstract Builder setKey(TemporaryExposureKey temporaryTracingKey);

            @Deprecated
            public abstract Builder setPackageName(String packageName);

            @Deprecated
            public abstract Builder setSignatureHash(byte[] signatureHash);

            @Deprecated
            public abstract Builder setToken(String token);

            public abstract Builder setTokenRoot(byte[] tokenRoot);

            public abstract Builder setExposureResult(ExposureResult exposureResult);
        }

        public static Builder builder() {
            return new AutoValue_ExposureResultStorage_Row.Builder();
        }

        /**
         * {@link TemporaryExposureKey} this {@link ExposureResult} was calculated for.
         */
        public abstract TemporaryExposureKey key();

        /**
         * Package that requested this row's {@link ExposureResult}.
         */
        @Deprecated
        @Nullable
        public abstract String packageName();

        /**
         * Package signature required to differentiate client apps.
         */
        @Deprecated
        @Nullable
        public abstract byte[] signatureHash();

        /**
         * Token this {@link ExposureResult} was provided with.
         */
        @Deprecated
        @Nullable
        abstract String token();

        @Nullable
        abstract byte[] tokenRoot();

        /**
         * {@link ExposureResult} calculated for.
         */
        abstract ExposureResult exposureResult();
    }

    /**
     * See {@link KeyEncoder} for details.
     */
    private static class PackageRootEncoder extends SerialEncoder {

        PackageRootEncoder(String packageName, byte[] signatureHash) {
            super(
                    ImmutableList.of(new Sha256StringEncoder(packageName), new BytesEncoder(signatureHash)));
        }
    }

    /**
     * See {@link KeyEncoder} for details.
     */
    private static class TokenRootEncoder extends SerialEncoder {

        TokenRootEncoder(String packageName, byte[] signatureHash, String token) {
            super(
                    ImmutableList.of(
                            new PackageRootEncoder(packageName, signatureHash), new Sha256StringEncoder(token)));
        }
    }

    /**
     * Encodes token root into byte array.
     */
    public static byte[] encodeTokenRoot(String packageName, byte[] signatureHash, String token) {
        return new ExposureResultStorage.TokenRootEncoder(packageName, signatureHash, token).encode();
    }

    /**
     * This {@link Encoder} is a four part encoding:
     *
     * <ol>
     *   <li>Sha256 hashed package name
     *   <li>Signature byte array (assumed to be Sha256)
     *   <li>Sha256 hashed token
     *   <li>Exposure key byte array ({@link ContactTracingFeature#contactIdLength()} in length)
     * </ol>
     * <p>
     * To encode only up to each of the parts, use the respective root encoders above:
     *
     * <ul>
     *   <li>To encode up to package and signature, use {@link PackageRootEncoder}
     *   <li>To encode up to token, use {@link TokenRootEncoder}
     * </ul>
     */
    private static class KeyEncoder extends SerialEncoder {

        KeyEncoder(byte[] tokenRoot, byte[] exposureKey) {
            super(ImmutableList.of(new BytesEncoder(tokenRoot), new BytesEncoder(exposureKey)));
        }

        /**
         * @deprecated use {@link #KeyEncoder(byte[], byte[])} instead.
         */
        @Deprecated
        KeyEncoder(String packageName, byte[] signatureHash, String token, byte[] exposureKey) {
            super(
                    ImmutableList.of(
                            new TokenRootEncoder(packageName, signatureHash, token),
                            new BytesEncoder(exposureKey)));
        }

        /**
         * @deprecated use {@link #KeyEncoder(byte[], byte[])} instead.
         */
        @Deprecated
        @SuppressWarnings("nullness")
        KeyEncoder(Row row) {
            this(row.packageName(), row.signatureHash(), row.token(), row.key().getKeyData());
        }
    }
}