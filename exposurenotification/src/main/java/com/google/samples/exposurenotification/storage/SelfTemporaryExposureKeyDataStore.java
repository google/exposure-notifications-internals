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

import androidx.annotation.NonNull;

import com.google.samples.exposurenotification.Log;
import com.google.samples.exposurenotification.nearby.TemporaryExposureKey;
import com.google.samples.exposurenotification.data.TemporaryExposureKeySupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * TODO: Implement a way to store {@link TemporaryExposureKey}s.
 */
public class SelfTemporaryExposureKeyDataStore {
    private List<TemporaryExposureKey> storedKeys = new ArrayList<>();

    public void putKey(TemporaryExposureKey temporaryExposureKey) {
        if (!storedKeys.contains(temporaryExposureKey)) {
            storedKeys.add(temporaryExposureKey);
        }
    }

    public boolean hasKey(TemporaryExposureKey temporaryExposureKey) {
        return storedKeys.contains(temporaryExposureKey);
    }

    public CloseableIterable<TemporaryExposureKey> getAllKeys() throws StorageException {
        return new CloseableIterable<TemporaryExposureKey>() {
            @Override
            public void close() throws IOException {
                // Nothing to close in this stub.
            }

            @NonNull
            @Override
            public Iterator<TemporaryExposureKey> iterator() {
                return storedKeys.iterator();
            }
        };
    }

    public TemporaryExposureKey getSingleKeyForIntervalNumber(int intervalNumber) {
        Log.log.atInfo().log("Getting exposure key that covers interval number=%d.", intervalNumber);
        try (CloseableIterable<TemporaryExposureKey> iterableStoredKeys = getAllKeys()) {
            for (TemporaryExposureKey key : iterableStoredKeys) {
                if (key.getRollingStartIntervalNumber() <= intervalNumber
                        && intervalNumber < TemporaryExposureKeySupport.getRollingEndIntervalNumber(key)) {
                    Log.log.atInfo().log("Exposure key found");
                    return key;
                }
            }
        } catch (StorageException | IOException e) {
            Log.log.atWarning().withCause(e).log("Exposure key not found.");
            return null;
        }
        Log.log.atInfo().log("Exposure key not found.");
        return null;
    }
}
