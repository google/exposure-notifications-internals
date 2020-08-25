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

/** Encoding helper function for comparing the key. */
public class EncodingComparisonHelper {

    /**
     * Returns true if the {@code root} is a sub array of {@code key}.
     *
     * <p>A null input for root will always return true.
     */
    public static boolean hasRoot(byte[] root, byte[] key) {
        if (root.length > key.length) {
            return false;
        }
        for (int i = 0; i < root.length && i < key.length; ++i) {
            if (root[i] != key[i]) {
                return false;
            }
        }
        return root.length <= key.length;
    }

    private EncodingComparisonHelper() {}
}