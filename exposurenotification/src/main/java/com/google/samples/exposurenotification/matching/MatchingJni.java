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

package com.google.samples.exposurenotification.matching;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build.VERSION_CODES;
import android.util.Pair;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.samples.exposurenotification.ExposureKeyExportProto;
import com.google.samples.exposurenotification.Log;
import com.google.samples.exposurenotification.TemporaryExposureKey;
import com.google.samples.exposurenotification.data.fileformat.TemporaryExposureKeyConverter;
import com.google.samples.exposurenotification.features.ContactTracingFeature;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Implements generate id and key matching under native code. */
public class MatchingJni implements AutoCloseable {

    private static native long initNative(byte[][] bleScanResults);

    /**
     * Returns the {@link ExposureKeyExportProto.TemporaryExposureKey} array. Each element is the a
     * serialized byte array and can be converted by {@link
     * ExposureKeyExportProto.TemporaryExposureKey#parseFrom(byte[])}
     */
    private static native byte[][] matchingNative(long nativePtr, String[] keyFiles);

    private static native int[] matchingLegacyNative(
            long nativePtr, byte[][] tempKeys, int[] rollingStartIntervalNumber, int currentKeyIndex);

    /**
     * Returns the processed key count which are filtered by invoking {@link #matchingNative}. If the
     * {@code nativePtr} is invalid, returns -1.
     */
    private static native int lastProcessedKeyCountNative(long nativePtr);

    private static native void releaseNative(long nativePtr);

    private final long nativePtr;

    public static boolean loadNativeLibrary(Context context) {
        System.loadLibrary("matching");
        return true;
    }

    public MatchingJni(Context context, byte[][] bleScanResults) {
        loadNativeLibrary(context);
        this.nativePtr = initNative(bleScanResults);
        Log.log.atInfo().log("MatchingJni get native ptr %d", nativePtr);
    }

    public ImmutableSet<TemporaryExposureKey> matching(List<String> keyFiles) {
        byte[][] protoArray = matchingNative(nativePtr, keyFiles.toArray(new String[0]));
        if (protoArray == null) {
            Log.log.atInfo().log("MatchingJni get nullable key set from native.");
            return ImmutableSet.of();
        }

        ImmutableSet.Builder<TemporaryExposureKey> keySet = new ImmutableSet.Builder<>();
        for (byte[] proto : protoArray) {
            try {
                ExposureKeyExportProto.TemporaryExposureKey key =
                        ExposureKeyExportProto.TemporaryExposureKey.parseFrom(proto);
                TemporaryExposureKey convertedKey = new TemporaryExposureKeyConverter().convert(key);
                if (convertedKey == null) {
                    Log.log.atWarning().log("MatchingJni failed to convert the proto key.");
                    continue;
                }
                keySet.add(convertedKey);
            } catch (InvalidProtocolBufferException e) {
                Log.log.atWarning().withCause(e).log("MatchingJni failed to parse the proto byte.");
            }
        }
        return keySet.build();
    }

    public Pair<Integer, Set<TemporaryExposureKey>> matchingLegacy(
            Iterable<TemporaryExposureKey> temporaryExposureKeys) {
        int matchingWithNativeBufferKeySize =
                (int) ContactTracingFeature.matchingWithNativeBufferKeySize();
        Set<TemporaryExposureKey> result = new HashSet<>();
        TemporaryExposureKey[] keys = new TemporaryExposureKey[matchingWithNativeBufferKeySize];
        int keyCount = 0;
        byte[][] keyBuffer = new byte[matchingWithNativeBufferKeySize][];
        int[] rollingStartIntervalNumberBuffer = new int[matchingWithNativeBufferKeySize];
        int currentKeyIndex = 0;
        for (TemporaryExposureKey diagnosisKey : temporaryExposureKeys) {
            keyCount++;
            keyBuffer[currentKeyIndex] = diagnosisKey.getKeyData();
            rollingStartIntervalNumberBuffer[currentKeyIndex] =
                    diagnosisKey.getRollingStartIntervalNumber();
            keys[currentKeyIndex] = diagnosisKey;
            currentKeyIndex++;
            if (currentKeyIndex >= matchingWithNativeBufferKeySize) {
                processingResult(
                        matchingLegacyNative(
                                nativePtr, keyBuffer, rollingStartIntervalNumberBuffer, currentKeyIndex),
                        keys,
                        result);
                currentKeyIndex = 0;
            }
        }

        if (currentKeyIndex > 0) {
            processingResult(
                    matchingLegacyNative(
                            nativePtr, keyBuffer, rollingStartIntervalNumberBuffer, currentKeyIndex),
                    keys,
                    result);
        }
        Log.log
                .atInfo()
                .log(
                        "ExposureMatchingTracer.traceWithNative() %d keys matched, total %d keys",
                        result.size(), keyCount);
        return Pair.create(keyCount, result);
    }

    private static void processingResult(
            int[] matchedIdIndexes, TemporaryExposureKey[] keys, Set<TemporaryExposureKey> result) {
        if (matchedIdIndexes == null || matchedIdIndexes.length <= 0) {
            return;
        }
        for (int matchedIdIndex : matchedIdIndexes) {
            result.add(keys[matchedIdIndex]);
        }
    }

    public int getLastProcessedKeyCount() {
        return lastProcessedKeyCountNative(nativePtr);
    }

    @Override
    public void close() {
        releaseNative(nativePtr);
    }
}