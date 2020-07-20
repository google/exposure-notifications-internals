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

import com.google.samples.exposurenotification.ble.data.AdvertisementPacket;
import com.google.samples.exposurenotification.crypto.CryptoException;

/**
 * Interface used to generate rotating proximity ID for BLE advertisment.
 */
public interface BleKeyGenerator {

    /**
     * Generates a new AdvertisementPacket according to
     * https://covid19-static.cdn-apple.com/applications/covid19/current/static/contact-tracing/pdf/ExposureNotification-CryptographySpecificationv1.1.pdf
     */
    AdvertisementPacket generatePacket() throws CryptoException;
}