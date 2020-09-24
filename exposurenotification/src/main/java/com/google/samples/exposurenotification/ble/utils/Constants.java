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

package com.google.samples.exposurenotification.ble.utils;

import static com.google.common.io.BaseEncoding.base16;

import android.os.ParcelUuid;

import com.google.samples.exposurenotification.nearby.ExposureNotificationStatusCodes;

import java.util.UUID;

/** Constants for BLE scanner and advertiser. */
public class Constants {
    public static final int METADATA_LENGTH = 4;
    public static final ParcelUuid CONTACT_TRACER_UUID = new ParcelUuid(to128BitUuid((short) 0xFD6F));
    private static final int BIT_INDEX_OF_16_BIT_UUID = 32;

    public static UUID to128BitUuid(short shortUuid) {
        UUID baseUuid = UUID.fromString("00000000-0000-1000-8000-00805F9B34FB");
        return new UUID(
                ((shortUuid & 0xFFFFL) << BIT_INDEX_OF_16_BIT_UUID) | baseUuid.getMostSignificantBits(),
                baseUuid.getLeastSignificantBits());
    }

    public static String fromIdByteArrayToString(byte[] idByteArray) {
        return base16().encode(idByteArray);
    }

    /** The result of start or stop scanning & advertising. */
    public enum StatusCode {
        UNKNOWN(ExposureNotificationStatusCodes.FAILED),
        OK(ExposureNotificationStatusCodes.SUCCESS),
        ERROR_BLUETOOTH_NOT_SUPPORTED(ExposureNotificationStatusCodes.FAILED_NOT_SUPPORTED),
        ERROR_BLUETOOTH_DISABLED(ExposureNotificationStatusCodes.FAILED_BLUETOOTH_DISABLED),
        ERROR_BLUETOOTH_UNKNOWN(ExposureNotificationStatusCodes.INTERNAL_ERROR);

        private final int apiStatusCode;

        StatusCode(int apiStatusCode) {
            this.apiStatusCode = apiStatusCode;
        }

        public int getApiStatusCode() {
            return apiStatusCode;
        }
    }

    private Constants() {}
}