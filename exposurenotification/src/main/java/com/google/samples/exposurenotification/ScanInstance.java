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

package com.google.samples.exposurenotification;


import com.google.common.base.Objects;

/**
 * Information about the sighting of a TEK within a BLE scan (of a few seconds).
 *
 * <p>The TEK itself isn't exposed by the API.
 */
public final class ScanInstance {

    int typicalAttenuationDb;
    int minAttenuationDb;
    int secondsSinceLastScan;

    ScanInstance(
            int typicalAttenuationDb,
            int minAttenuationDb,
            int secondsSinceLastScan) {
        this.typicalAttenuationDb = typicalAttenuationDb;
        this.minAttenuationDb = minAttenuationDb;
        this.secondsSinceLastScan = secondsSinceLastScan;
    }

    /**
     * Aggregation of the attenuations of all of this TEK's beacons received during the scan, in dB.
     * This is most likely to be an average in the dB domain.
     */
    public int getTypicalAttenuationDb() {
        return typicalAttenuationDb;
    }

    /**
     * Minimum attenuation of all of this TEK's beacons received during the scan, in dB.
     */
    public int getMinAttenuationDb() {
        return minAttenuationDb;
    }

    /**
     * Seconds elapsed since the previous scan, typically used as a weight.
     *
     * <p>Two example uses: - Summing those values over all sightings of an exposure provides the
     * duration of that exposure. - Summing those values over all sightings in a given attenuation
     * range and over all exposures recreates the durationAtBuckets of v1.
     *
     * <p>Note that the previous scan may not have led to a sighting of that TEK.
     */
    public int getSecondsSinceLastScan() {
        return secondsSinceLastScan;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ScanInstance that = (ScanInstance) o;
        return typicalAttenuationDb == that.typicalAttenuationDb
                && minAttenuationDb == that.minAttenuationDb
                && secondsSinceLastScan == that.secondsSinceLastScan;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(typicalAttenuationDb, minAttenuationDb, secondsSinceLastScan);
    }

    @Override
    public String toString() {
        return "ScanInstance{"
                + "typicalAttenuationDb="
                + typicalAttenuationDb
                + ", minAttenuationDb="
                + minAttenuationDb
                + ", secondsSinceLastScan="
                + secondsSinceLastScan
                + '}';
    }

    /**
     * Builder for ScanInstance.
     */
    public static class Builder {

        private int typicalAttenuationDb;
        private int minAttenuationDb;
        private int secondsSinceLastScan;

        public Builder() {
            typicalAttenuationDb = 0;
            minAttenuationDb = 0;
            secondsSinceLastScan = 0;
        }

        public Builder setTypicalAttenuationDb(int typicalAttenuationDb) {
            this.typicalAttenuationDb = typicalAttenuationDb;
            return this;
        }

        public Builder setMinAttenuationDb(int minAttenuationDb) {
            this.minAttenuationDb = minAttenuationDb;
            return this;
        }

        public Builder setSecondsSinceLastScan(int secondsSinceLastScan) {
            this.secondsSinceLastScan = secondsSinceLastScan;
            return this;
        }

        public ScanInstance build() {
            return new ScanInstance(typicalAttenuationDb, minAttenuationDb, secondsSinceLastScan);
        }
    }
}