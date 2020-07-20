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

package com.google.samples.exposurenotification.data;

import com.google.samples.ByteArrayValue;

/**
 * The Associated Encrypted Metadata is data encrypted along with the Rolling Proximity Identifier
 * and that can only be decrypted later, if the user broadcasting it is tested positive and reveals
 * their Temporary Exposure Key.
 */
public final class AssociatedEncryptedMetadata extends ByteArrayValue {

    /**
     * Create an {@link AssociatedEncryptedMetadata} by taking ownership of a {@code byte[]}.
     *
     * <p>WARNING: This value could be unexpectedly mutated if {@code data} will still be accessible
     * to the caller. To ensure an immutable value, use {@code new AssociatedEncryptedMetadata(data,
     * false)} to construct from a copy of {@code data}.
     */
    public static AssociatedEncryptedMetadata create(byte[] data) {
        return new AssociatedEncryptedMetadata(data, /*takeOwnership=*/ true);
    }

    public AssociatedEncryptedMetadata(byte[] data, boolean takeOwnership) {
        super(data, takeOwnership);
    }

    /**
     * Return a direct reference to the underlying data.
     *
     * <p>WARNING: This breaks the encapsulation and could allow the value to be mutated. Only use in
     * performance sensitive cases where the cost of cloning is too high, and the use of the {@code
     * byte[]} is guaranteed to be read-only.
     */
    public byte[] getDirect() {
        return exposeInternalBytesAndRiskMutation();
    }
}