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

import android.content.Context;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.samples.exposurenotification.ExposureKeyExportProto;
import com.google.samples.exposurenotification.Log;
import com.google.samples.exposurenotification.TemporaryExposureKey;
import com.google.samples.exposurenotification.data.fileformat.TemporaryExposureKeyConverter;

import java.util.List;
import java.util.Set;

/**
 * Implements generate id and key matching under native code.
 */
public class MatchingJni implements AutoCloseable {

    private static native long initNative(byte[][] bleScanResults);

    /**
     * Returns the {@link ExposureKeyExportProto.TemporaryExposureKey} array. Each element is the a
     * serialized byte array and can be converted by {@link
     * ExposureKeyExportProto.TemporaryExposureKey#parseFrom(byte[])}
     */
    private static native byte[][] matchingNative(long nativePtr, String[] keyFiles);

    /**
     * Returns the processed key count which are filtered by invoking {@link #matchingNative}. If the
     * {@code nativePtr} is invalid, returns -1.
     */
    private static native int lastProcessedKeyCountNative(long nativePtr);

    private static native void releaseNative(long nativePtr);

    private final long nativePtr;

    /**
     * Creates an object that will perform matching of seen RPIs to diagnosis keys downloaded
     * from a PHA server.
     *
     * @param context        The context to use (unused in this simplified version)
     * @param bleScanResults A list of RPI keys seen by the device over the past 14 days
     */
    public MatchingJni(Context context, byte[][] bleScanResults) {
        System.loadLibrary("matching");
        this.nativePtr = initNative(bleScanResults);
        Log.log.atInfo().log("MatchingJni get native ptr %d", nativePtr);
    }

    /**
     * Performs the actual matching of RPIs seen (provided in the constructor) with the
     * diagnosis key files provided.
     * <p>
     * The list of files provided here are *NOT* the ZIP files downloaded from the server. This
     * method expects a list of "export.bin" files that have already been verified to be
     * authentic by {@link ProvideDiagnosisKeys}.
     *
     * @param keyFiles List of "export.bin" files of diagnosis keys to check for matches against
     *                 the BLE Scan Results provided.
     * @return A list of diagnosis keys that the device has seen RPIs for that are included in
     * the provided file
     */
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