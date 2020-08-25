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

package com.google.samples.exposurenotification.ble.interfaces;

import com.google.samples.exposurenotification.ble.utils.Constants.StatusCode;

/**
 * BLE scanner interface. Used to scan nearby BLE advertisements for contact tracing.
 */
public interface BleScanner {
    /**
     * Checks if current device is able to scan.
     */
    StatusCode supportScanning();

    /**
     * Starts advertising. Should only be called when user opt in, or reboot completed.
     */
    StatusCode startScanning();

    /**
     * Starts advertising. Should only be called when user opt out, or contact tracing is no longer
     * necessary.
     */
    StatusCode stopScanning();

    /**
     * Indicates current scanner's state is scanning or not.
     */
    boolean isScanning();
}