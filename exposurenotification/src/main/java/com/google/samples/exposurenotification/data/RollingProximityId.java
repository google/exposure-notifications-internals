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
 * Represents a Rolling Proximity Identifier.
 */
@SuppressWarnings("unused")
public final class RollingProximityId extends ByteArrayValue {

    private static final byte[] MIN_VALUE = {
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };
    private static final byte[] MAX_VALUE = {
            (byte) 0xFF,
            (byte) 0xFF,
            (byte) 0xFF,
            (byte) 0xFF,
            (byte) 0xFF,
            (byte) 0xFF,
            (byte) 0xFF,
            (byte) 0xFF,
            (byte) 0xFF,
            (byte) 0xFF,
            (byte) 0xFF,
            (byte) 0xFF,
            (byte) 0xFF,
            (byte) 0xFF,
            (byte) 0xFF,
            (byte) 0xFF
    };
    public static final RollingProximityId MIN_ID = new RollingProximityId(MIN_VALUE);
    public static final RollingProximityId MAX_ID = new RollingProximityId(MAX_VALUE);

    public RollingProximityId(byte[] value) {
        super(value);
    }

    /**
     * Constructor which will take ownership of {@code value} if {@code takeOwnership} is true.
     *
     * <p>WARNING: This constructor can leave this {@link RollingProximityId} open to unexpected
     * mutation. Use only in cases where the {@code byte[]} is a temporary, to avoid the overhead of
     * cloning an array which will immediately go out of scope.
     */
    public RollingProximityId(byte[] value, boolean takeOwnership) {
        super(value, takeOwnership);
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