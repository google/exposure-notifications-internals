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

import com.google.auto.value.AutoValue;

/**
 * A {@link RollingProximityId} with a concept of what time interval number it was broadcasted for.
 *
 * <p>This class is intentionally different from {@link RollingProximityId} in that this class is
 * owned by system internally while {@link RollingProximityId}s are ids observed externally by
 * scanning.
 */
@AutoValue
public abstract class GeneratedRollingProximityId {

    /**
     * The {@link RollingProximityId} value.
     */
    public abstract RollingProximityId rollingProximityId();

    /**
     * The associated interval number that is encrypted inside of the {@link RollingProximityId}.
     */
    public abstract int intervalNumber();

    public static GeneratedRollingProximityId create(
            RollingProximityId rollingProximityId, int intervalNumber) {
        return new AutoValue_GeneratedRollingProximityId(rollingProximityId, intervalNumber);
    }
}