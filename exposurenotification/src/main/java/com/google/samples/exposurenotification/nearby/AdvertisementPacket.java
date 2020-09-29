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

package com.google.samples.exposurenotification.nearby;

import androidx.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Bytes;
import com.google.samples.exposurenotification.ble.utils.Constants;
import com.google.samples.exposurenotification.features.ContactTracingFeature;

/**
 * Fully constructed service data that should be advertised over BLE.
 */
public class AdvertisementPacket {
    private final byte[] id;
    private final byte[] metadata;
    private final byte[] encryptedMetadata;

    private AdvertisementPacket(byte[] id, byte[] metadata, byte[] encryptedMetadata) {
        this.id = id;
        this.metadata = metadata;
        this.encryptedMetadata = encryptedMetadata;
    }

    /**
     * Retrieves fully constructed service data.
     */
    public byte[] getPacket() {
        return Bytes.concat(id, encryptedMetadata);
    }

    /**
     * Returns the rolling proximity ID.
     *
     * <p>This method returns the mutable reference to the byte array. Use with caution!
     */
    public byte[] getIdDirect() {
        return id;
    }

    /**
     * Returns the raw (unencrypted) metadata for debugging purpose.
     *
     * <p>This method returns the mutable reference to the byte array. Use with caution!
     */
    public byte[] getRawMetadataDirect() {
        return metadata;
    }

    /**
     * Returns the encrypted metadata.
     *
     * <p>This method returns the mutable reference to the byte array. Use with caution!
     */
    public byte[] getEncryptedMetadataDirect() {
        return encryptedMetadata;
    }

    @Override
    public String toString() {
        return "ID: "
                + Constants.fromIdByteArrayToString(id)
                + " Metadata: "
                + Constants.fromIdByteArrayToString(encryptedMetadata);
    }

    /**
     * Builder that assists with constructing valid AdvertisementPackets.
     */
    public static class Builder {
        @Nullable
        private byte[] id;
        @Nullable
        private byte[] metadata;
        @Nullable
        private byte[] encryptedMetadata;

        /**
         * Sets the rotating proximity ID that should be put into the advertisement packet.
         *
         * @throws IllegalArgumentException If the {@code id} is not the right length.
         */
        public Builder setProxId(byte[] id) {
            Preconditions.checkArgument(
                    id.length == ContactTracingFeature.contactIdLength(),
                    "Invalid prox ID length %s",
                    id.length);

            this.id = id;

            return this;
        }

        /**
         * Sets the raw metadata.
         *
         * <p>Raw metadata is for debugging purpose only. It's not put into the advertisement packet.
         *
         * @throws IllegalArgumentException if the {@code metadata} is not the right length.
         */
        public Builder setRawMetadata(byte[] metadata) {
            Preconditions.checkArgument(
                    metadata.length == Constants.METADATA_LENGTH,
                    "Invalid metadata length %s",
                    metadata.length);

            this.metadata = metadata;
            return this;
        }

        /**
         * Sets the encrypted metadata that should be put into the advertisement packet.
         *
         * @throws IllegalArgumentException if the {@code encryptedMetadata} is not the right length.
         */
        public Builder setEncryptedMetadata(byte[] encryptedMetadata) {
            Preconditions.checkArgument(
                    encryptedMetadata.length == Constants.METADATA_LENGTH,
                    "Invalid encryptedMetadata length %s",
                    encryptedMetadata.length);

            this.encryptedMetadata = encryptedMetadata;
            return this;
        }

        /**
         * Constructs an advertisement packet.
         */
        public AdvertisementPacket build() {
            Preconditions.checkNotNull(id, "id must be set.");
            Preconditions.checkNotNull(metadata, "metadata must be set.");
            Preconditions.checkNotNull(encryptedMetadata, "encryptedMetadata must be set.");
            return new AdvertisementPacket(id, metadata, encryptedMetadata);
        }
    }
}