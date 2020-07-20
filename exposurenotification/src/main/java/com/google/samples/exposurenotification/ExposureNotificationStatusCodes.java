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

/**
 * Status codes for nearby contact tracing results.
 */
public final class ExposureNotificationStatusCodes {

    private ExposureNotificationStatusCodes() {
    }

    // IMPORTANT NOTE: The codes referenced in this file are used on both the client and service
    // side, and must not be modified after launch. It is fine to add new codes, but previously
    // existing codes must be left unmodified.

    // Common status codes that may be used by a variety of actions.

    /**
     * The operation was successful.
     */
    public static final int SUCCESS = 0;

    /**
     * An internal error occurred. Retrying should resolve the problem.
     */
    public static final int INTERNAL_ERROR = 8;

    /**
     * The application is misconfigured. This error is not recoverable and will be treated as fatal.
     * The developer should look at the logs after this to determine more actionable information.
     */
    public static final int DEVELOPER_ERROR = 10;

    /**
     * The operation failed, without any more information.
     */
    public static final int FAILED = 13;

    /**
     * A blocking call was interrupted while waiting and did not run to completion.
     */
    public static final int INTERRUPTED = 14;

    /**
     * Timed out while awaiting the result.
     */
    public static final int TIMEOUT = 15;

    /**
     * The result was canceled.
     */
    public static final int CANCELED = 16;

    /**
     * The app was already in the requested state so the call did nothing.
     */
    public static final int FAILED_ALREADY_STARTED = 39500;

    /**
     * The hardware capability of the device was not supported.
     */
    public static final int FAILED_NOT_SUPPORTED = 39501;

    /**
     * The user rejected the opt-in state.
     */
    public static final int FAILED_REJECTED_OPT_IN = 39502;

    /**
     * The functionality was disabled by the user or the phone.
     */
    public static final int FAILED_SERVICE_DISABLED = 39503;

    /**
     * The bluetooth was powered off.
     */
    public static final int FAILED_BLUETOOTH_DISABLED = 39504;

    /**
     * The service was disabled for some reasons temporarily.
     */
    public static final int FAILED_TEMPORARILY_DISABLED = 39505;

    /**
     * The operation failed during a disk read/write.
     */
    public static final int FAILED_DISK_IO = 39506;

    /**
     * The client is unauthorized to access the APIs.
     */
    public static final int FAILED_UNAUTHORIZED = 39507;

    /**
     * The client has been rate limited for access to this API.
     */
    public static final int FAILED_RATE_LIMITED = 39508;

    /**
     * Returns an untranslated debug (not user-friendly!) string based on the current status code.
     */
    public static String getStatusCodeString(int statusCode) {
        switch (statusCode) {
            case FAILED_ALREADY_STARTED:
                return "FAILED_ALREADY_STARTED";
            case FAILED_NOT_SUPPORTED:
                return "FAILED_NOT_SUPPORTED";
            case FAILED_REJECTED_OPT_IN:
                return "FAILED_REJECTED_OPT_IN";
            case FAILED_SERVICE_DISABLED:
                return "FAILED_SERVICE_DISABLED";
            case FAILED_BLUETOOTH_DISABLED:
                return "FAILED_BLUETOOTH_DISABLED";
            case FAILED_TEMPORARILY_DISABLED:
                return "FAILED_TEMPORARILY_DISABLED";
            case FAILED_DISK_IO:
                return "FAILED_DISK_IO";
            case FAILED_UNAUTHORIZED:
                return "FAILED_UNAUTHORIZED";
            case FAILED_RATE_LIMITED:
                return "FAILED_RATE_LIMITED";
            default:
                return "unknown status code: " + statusCode;
        }
    }
}