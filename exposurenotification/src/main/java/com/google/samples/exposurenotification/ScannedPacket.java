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

import androidx.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.samples.Hex;

import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * A scanned packet coming from other device's EN advertising.
 *
 * @hide
 */
public class ScannedPacket {

    byte[] id;
    byte[] encryptedMetadata;
    ScannedPacketContent[] scannedPacketContents;

    ScannedPacket(
            byte[] id,
            byte[] encryptedMetadata,
            ScannedPacketContent[] scannedPacketContents) {
        this.id = id;
        this.encryptedMetadata = encryptedMetadata;
        this.scannedPacketContents = scannedPacketContents;
    }

    /**
     * Returns the rolling proximity ID.
     */
    public byte[] getId() {
        return Arrays.copyOf(id, id.length);
    }

    /**
     * Returns the encrypted metadata.
     */
    public byte[] getEncryptedMetadata() {
        return Arrays.copyOf(encryptedMetadata, encryptedMetadata.length);
    }

    /**
     * Returns the scanned packet contents.
     */
    public ScannedPacketContent[] getScannedPacketContents() {
        return Arrays.copyOf(scannedPacketContents, scannedPacketContents.length);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof ScannedPacket) {
            ScannedPacket that = (ScannedPacket) obj;
            return Arrays.equals(id, that.id)
                    && Arrays.equals(encryptedMetadata, that.encryptedMetadata)
                    && Arrays.equals(scannedPacketContents, that.scannedPacketContents);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, encryptedMetadata, scannedPacketContents);
    }

    @Override
    public String toString() {
        StringBuilder scannedPacketContentsString = new StringBuilder();
        for (ScannedPacketContent scannedPacketContent : scannedPacketContents) {
            scannedPacketContentsString.append(scannedPacketContent.toString()).append(",");
        }
        return String.format(
                Locale.US,
                "ScannedPacket<id: %s, encryptedMetadata: %s, scannedPacketContents: %s>",
                Hex.bytesToStringLowercase(id),
                Hex.bytesToStringLowercase(encryptedMetadata),
                scannedPacketContentsString);
    }

    /**
     * A builder for {@link ScannedPacket}.
     */
    public static final class ScannedPacketBuilder {
        private byte[] id = new byte[0];
        private byte[] encryptedMetadata = new byte[0];
        private ScannedPacketContent[] scannedPacketContents = new ScannedPacketContent[0];

        public ScannedPacketBuilder setId(byte[] id) {
            this.id = Arrays.copyOf(id, id.length);
            return this;
        }

        public ScannedPacketBuilder setEncryptedMetadata(byte[] encryptedMetadata) {
            this.encryptedMetadata = Arrays.copyOf(encryptedMetadata, encryptedMetadata.length);
            return this;
        }

        public ScannedPacketBuilder setScannedPacketContents(
                ScannedPacketContent[] scannedPacketContents) {
            checkArgument(
                    scannedPacketContents != null && scannedPacketContents.length > 0,
                    "scannedPacketContents's size must be > 0");
            this.scannedPacketContents =
                    Arrays.copyOf(scannedPacketContents, scannedPacketContents.length);
            return this;
        }

        public ScannedPacket build() {
            return new ScannedPacket(id, encryptedMetadata, scannedPacketContents);
        }
    }

    /**
     * A scanned packet content coming from other device's EN advertising.
     *
     * @hide
     */
    public static class ScannedPacketContent {

        int epochSeconds;
        int rssi;
        int previousScanEpochSeconds;

        ScannedPacketContent(
                int epochSeconds,
                int rssi,
                int previousScanEpochSeconds) {
            this.epochSeconds = epochSeconds;
            this.rssi = rssi;
            this.previousScanEpochSeconds = previousScanEpochSeconds;
        }

        public int getEpochSeconds() {
            return epochSeconds;
        }

        public int getRssi() {
            return rssi;
        }

        public int getPreviousScanEpochSeconds() {
            return previousScanEpochSeconds;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof ScannedPacketContent) {
                ScannedPacketContent that = (ScannedPacketContent) obj;
                return Objects.equal(epochSeconds, that.epochSeconds)
                        && Objects.equal(rssi, that.rssi)
                        && Objects.equal(previousScanEpochSeconds, that.previousScanEpochSeconds);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(epochSeconds, rssi, previousScanEpochSeconds);
        }

        @Override
        public String toString() {
            return String.format(
                    Locale.US,
                    "ScannedPacketContent<"
                            + "epochSeconds: %s, "
                            + "rssi: %d, "
                            + "previousScanEpochSeconds: %s>",
                    new Date(SECONDS.toMillis(epochSeconds)),
                    rssi,
                    new Date(SECONDS.toMillis(previousScanEpochSeconds)));
        }

        /**
         * A builder for {@link ScannedPacketContent}.
         */
        public static final class ScannedPacketContentBuilder {
            private int epochSeconds = 0;
            private int rssi = 0;
            private int previousScanEpochSeconds = 0;

            public ScannedPacketContentBuilder setEpochSeconds(int epochSeconds) {
                this.epochSeconds = epochSeconds;
                return this;
            }

            public ScannedPacketContentBuilder setRssi(int rssi) {
                this.rssi = rssi;
                return this;
            }

            public ScannedPacketContentBuilder setPreviousScanEpochSeconds(int previousScanEpochSeconds) {
                this.previousScanEpochSeconds = previousScanEpochSeconds;
                return this;
            }

            public ScannedPacketContent build() {
                return new ScannedPacketContent(epochSeconds, rssi, previousScanEpochSeconds);
            }
        }
    }
}