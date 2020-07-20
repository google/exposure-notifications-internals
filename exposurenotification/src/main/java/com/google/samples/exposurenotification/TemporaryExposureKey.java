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

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * A key generated for advertising over a window of time.
 */
public final class TemporaryExposureKey implements Parcelable {

    public static final Parcelable.Creator<TemporaryExposureKey> CREATOR =
            new Parcelable.Creator<TemporaryExposureKey>() {

                @Override
                public TemporaryExposureKey createFromParcel(Parcel source) {
                    return new TemporaryExposureKey(source);
                }

                @Override
                public TemporaryExposureKey[] newArray(int size) {
                    return new TemporaryExposureKey[size];
                }
            };

    byte[] keyData;
    int rollingStartIntervalNumber;
    @RiskLevel
    int transmissionRiskLevel;
    int rollingPeriod;

    TemporaryExposureKey(
            byte[] keyData,
            int rollingStartIntervalNumber,
            @RiskLevel int transmissionRiskLevel,
            int rollingPeriod) {
        this.keyData = keyData;
        this.rollingStartIntervalNumber = rollingStartIntervalNumber;
        this.transmissionRiskLevel = transmissionRiskLevel;
        this.rollingPeriod = rollingPeriod;
    }

    private TemporaryExposureKey(@NonNull Parcel in) {
        int keyLen = in.readInt();
        this.keyData = new byte[keyLen];
        in.readByteArray(this.keyData);
        this.rollingStartIntervalNumber = in.readInt();
        this.transmissionRiskLevel = in.readInt();
        this.rollingPeriod = in.readInt();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(this.keyData.length);
        dest.writeByteArray(this.keyData);
        dest.writeInt(this.rollingStartIntervalNumber);
        dest.writeInt(this.transmissionRiskLevel);
        dest.writeInt(this.rollingPeriod);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * The randomly generated Temporary Exposure Key information.
     */
    public byte[] getKeyData() {
        return Arrays.copyOf(keyData, keyData.length);
    }

    /**
     * A number describing when a key starts. It is equal to startTimeOfKeySinceEpochInSecs / (60 *
     * 10).
     */
    public int getRollingStartIntervalNumber() {
        return rollingStartIntervalNumber;
    }

    /**
     * Risk of transmission associated with the person this key came from.
     */
    @RiskLevel
    public int getTransmissionRiskLevel() {
        return transmissionRiskLevel;
    }

    /**
     * A number describing how long a key is valid. It is expressed in increments of 10 minutes (e.g.
     * 144 for 24 hours).
     */
    public int getRollingPeriod() {
        return rollingPeriod;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof TemporaryExposureKey) {
            TemporaryExposureKey that = (TemporaryExposureKey) obj;
            return Arrays.equals(keyData, that.getKeyData())
                    && Objects.equals(rollingStartIntervalNumber, that.getRollingStartIntervalNumber())
                    && Objects.equals(transmissionRiskLevel, that.getTransmissionRiskLevel())
                    && Objects.equals(rollingPeriod, that.getRollingPeriod());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                keyData, rollingStartIntervalNumber, transmissionRiskLevel, rollingPeriod);
    }

    @Override
    public String toString() {
        return String.format(
                Locale.US,
                "TemporaryExposureKey<"
                        + "keyData: %s, "
                        + "rollingStartIntervalNumber: %s, "
                        + "transmissionRiskLevel: %d, "
                        + "rollingPeriod: %d"
                        + ">",
                new BigInteger(1, keyData).toString(16),
                new Date(MINUTES.toMillis(rollingStartIntervalNumber * 10L)),
                transmissionRiskLevel,
                rollingPeriod);
    }

    /**
     * A builder for {@link TemporaryExposureKey}.
     */
    @SuppressWarnings("unused")
    public static final class TemporaryExposureKeyBuilder {

        private byte[] keyData = new byte[0];
        private int rollingStartIntervalNumber = 0;
        @RiskLevel
        private int transmissionRiskLevel = RiskLevel.RISK_LEVEL_INVALID;
        private int rollingPeriod = 0;

        public TemporaryExposureKeyBuilder setKeyData(byte[] keyData) {
            this.keyData = Arrays.copyOf(keyData, keyData.length);
            return this;
        }

        public TemporaryExposureKeyBuilder setRollingStartIntervalNumber(
                int rollingStartIntervalNumber) {
            checkArgument(
                    rollingStartIntervalNumber >= 0,
                    "rollingStartIntervalNumber (%s) must be >= 0",
                    rollingStartIntervalNumber);
            this.rollingStartIntervalNumber = rollingStartIntervalNumber;
            return this;
        }

        public TemporaryExposureKeyBuilder setTransmissionRiskLevel(
                @RiskLevel int transmissionRiskLevel) {
            checkArgument(
                    transmissionRiskLevel >= 0 && transmissionRiskLevel <= 8,
                    "transmissionRiskLevel (%s) must be >= 0 and <= 8",
                    transmissionRiskLevel);
            this.transmissionRiskLevel = transmissionRiskLevel;
            return this;
        }

        public TemporaryExposureKeyBuilder setRollingPeriod(int rollingPeriod) {
            checkArgument(rollingPeriod >= 0, "rollingPeriod (%s) must be >= 0", rollingPeriod);
            this.rollingPeriod = rollingPeriod;
            return this;
        }

        public TemporaryExposureKey build() {
            return new TemporaryExposureKey(
                    keyData, rollingStartIntervalNumber, transmissionRiskLevel, rollingPeriod);
        }
    }
}