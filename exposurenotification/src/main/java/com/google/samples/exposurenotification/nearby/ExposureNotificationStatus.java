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

import com.google.common.collect.ImmutableSet;
import java.util.Set;

/** Detail status for exposure notification service. */
public enum ExposureNotificationStatus {
    /** Exposure notification is running. */
    ACTIVATED(0x00000001),
    /** Exposure notification is not running. */
    INACTIVATED(0x00000002),
    /** Bluetooth is not enabled. */
    BLUETOOTH_DISABLED(0x00000004),
    /** Location is not enabled. */
    LOCATION_DISABLED(0x00000008),
    /** User is not consent for the client. */
    NO_CONSENT(0x00000010),
    /** The client is not in approved client list. */
    NOT_IN_WHITELIST(0x00000020),
    /** Can't detected the BLE supporting of this device due to bluetooth is not enabled. */
    BLUETOOTH_SUPPORT_UNKNOWN(0x00000040),
    /** Hardware of this device doesn't support exposure notification. */
    HW_NOT_SUPPORT(0x00000080),
    /** There is another client running as active client. */
    FOCUS_LOST(0x00000100),
    /** Device storage is not sufficient for exposure notification. */
    LOW_STORAGE(0x00000200),
    /** Current status is unknown. */
    UNKNOWN(0x00000400),
    /** Exposure notification is not supported. */
    EN_NOT_SUPPORT(0x00000800),
    /** Exposure notification is not supported for current user profile. */
    USER_PROFILE_NOT_SUPPORT(0x00001000);

    private final long value;

    ExposureNotificationStatus(long value) {
        this.value = value;
    }

    public static long getValues(Set<ExposureNotificationStatus> statusSet) {
        long result = 0;
        for (ExposureNotificationStatus status : statusSet) {
            result |= status.value;
        }
        return result;
    }

    public static ImmutableSet<ExposureNotificationStatus> fromValue(long value) {
        ImmutableSet.Builder<ExposureNotificationStatus> builder = ImmutableSet.builder();
        for (ExposureNotificationStatus status : ExposureNotificationStatus.values()) {
            if ((status.value & value) != 0) {
                builder.add(status);
            }
        }
        return builder.build();
    }
}