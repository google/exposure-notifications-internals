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

package com.google.samples.exposurenotification.features;

import java.util.Arrays;
import java.util.List;

/**
 * This class contains various constants and feature flags used through the exposure notifications
 * system. Many of these values should ideally be delivered in a way that they can be updated
 * without needing to push a new version of code. For example, the method
 * {@link #partnerPublicKeys()} returns the public keys of all currently active partners using
 * the system.
 * FIXME: Implement this as a deserialized object or some sort of key/value store.
 */
public class ContactTracingFeature {
    /**
     * The input parameter as HKDF info to generate AEMK.
     */
    public static String aemkHkdfInfoString() {
        return "EN-AEMK";
    }

    /**
     * The length of associated metadata encryption key in bytes.
     */
    public static int associatedMetadataEncryptionKeySizeBytes() {
        return 16;
    }

    /**
     * Maximum number of keys that will be passed to native for matching in one
     * native matching call.
     */
    public static int matchingWithNativeBufferKeySize() {
        return 10000;
    }

    /**
     * The length for which a Temporary Tracing Key is valid in multiple of id rolling period.
     */
    public static int tkRollingPeriodMultipleOfIdRollingPeriod() {
        return 144;
    }

    /**
     * Whether to store the rolling period alongside the TEK.
     */
    public static boolean storeRollingPeriod() {
        return true;
    }

    /**
     * Whether to support flexible starting intervals for TEK.
     */
    public static boolean storeFlexibleStartingIntervals() {
        return true;
    }

    /**
     * The length of temporary tracing key in bytes.
     */
    public static int temporaryTracingKeySizeBytes() {
        return 16;
    }

    /**
     * The number of rolling periods that can be used before and after an RPIs validity time
     * during matching.
     */
    public static int tkMatchingClockDriftRollingPeriods() {
        return 12;
    }

    /**
     * The length for which an advertising ID is valid.
     */
    public static int idRollingPeriodMinutes() {
        return 10;
    }

    /**
     * The length of rolling proximity ID key in bytes.
     */
    public static int rollingProximityIdKeySizeBytes() {
        return 16;
    }

    /**
     * The input parameter as HKDF info to generate RPIK.
     */
    public static String rpikHkdfInfoString() {
        return "EN-RPIK";
    }

    /**
     * The padded constant for generating rolling proximity IDs
     */
    public static String rpidAesPaddedString() {
        return "EN-RPI";
    }

    /**
     * The number of (full) days to keep the contact record data
     */
    public static int contactRecordDaysToKeep() {
        return 14;
    }

    /**
     * The transmit power as would be reported by an average iPhone
     * transmitting at the same power as the Android device running with
     * ADVERTISE_TX_POWER_LOW
     */
    public static int txCalibrationPower() {
        return 0;
    }

    /**
     * Offset to add to the tx calibration power to obtain the tx
     * power to publish in the payload
     */
    public static int txCalibrationPowerToTxPower() {
        return 0;
    }

    /**
     * If enabled, the v1.1 metadata format will be advertised which
     * includes the calibration confidence value.
     */
    public static boolean advertisementMetadataV11() {
        return true;
    }

    /**
     * The confidence value for tx_calibration_power and rssi_offset.
     * Higher value means higher confidence.
     * Sees: exposurenotification/src/main/proto/calibration_enums.proto
     */
    public static byte calibrationConfidence() {
        return 0;
    }

    /**
     * The length of contact ID.
     */
    public static int contactIdLength() {
        return 16;
    }

    /**
     * Value of ADVERTISE_MODE_X enum that should be used.
     */
    public static int advertiseMode() {
        return 1; // BALANCED (250ms)
    }

    /**
     * Value of ADVERTISE_TX_POWER_X enum that should be used
     */
    public static int advertiseTxPower() {
        return 1;  // LOW (-15dB)
    }

    /**
     * Whether the advertisement should be connectable
     */
    public static boolean advertiseConnectable() {
        return false;
    }

    /**
     * Whether the BLE advertiser is legacy mode, will only take effect for O and above
     */
    public static boolean advertiseLegacyMode() {
        return true;
    }

    /**
     * The BLE advertising interval, will only take effect for O and above
     */
    public static int advertiseInterval() {
        return 400;  // AdvertisingSetParameters.INTERVAL_MEDIUM
    }

    /**
     * Value of ADVERTISE_TX_POWER_X enum that should be used
     */
    public static int advertiseTxPowerO() {
        return -15; // AdvertisingSetParameters.TX_POWER_LOW
    }

    /**
     * Whether the BLE advertiser is scannable, will only take effect for O and above
     */
    public static boolean advertiseScannable() {
        return false;
    }

    /**
     * Whether the BLE advertiser should require multi-advertisement support
     */
    public static boolean requireMultiAdvertisement() {
        return true;
    }

    /**
     * Whether using startAdvertisingSet to start advertising for O and above
     */
    public static boolean advertiseUseOreoAdvertiser() {
        return false;
    }

    /**
     * If enabled, more error messages are returned to clients.
     */
    public static boolean enhanceApiErrorMessage() {
        return true;
    }

    /**
     * File name of signature inside diagnosis keys zip
     */
    public static String diagnosisKeySignatureFileName() {
        return "export.sig";
    }

    /**
     * File name of key data binary inside diagnosis keys zip
     */
    public static String diagnosisKeyBinFileName() {
        return "export.bin";
    }

    /**
     * Calculates the hash of all keys when provideDiagnosisKeys is called.
     */
    public static boolean calculateDiagnosisKeyHash() {
        return true;
    }

    /**
     * Maximum permissible batch size group size of signature
     */
    public static int signatureMaxAllowedBatchGroupSize() {
        return 1_000_000;
    }

    /**
     * If enabled, signature verification would accept multiple whole batches
     */
    public static boolean enableMultipleWholeBatchSignatureVerification() {
        return true;
    }

    /**
     * Allow signature files to have signatures without a matching public key, or more matching
     * public keys than signatures, during verification.
     */
    public static boolean ignoreUnmatchedSignatures() {
        return true;
    }

    /**
     * A list of partner public keys for diagnosis key signature verification.
     */
    public static List<String> partnerPublicKeys() {
        return Arrays.asList(TEST_PARTNER_PUBLIC_KEYS);
    }

    /**
     * each list entry is a string formatted as:
     * <my.package.name>:<COSIGN_SET>|<COSIGN_SET>|...
     * <p>
     * COSIGN_SET:
     * <PUBLIC_KEY>&<PUBLIC_KEY>&...
     * <p>
     * PUBLIC_KEY:
     * <NAME>,<BASE64KEYVALUE>
     * <p>
     * NAME:
     * <KEY_ID>-<KEY_VERSION>
     * <p>
     * KEY_ID and KEY_VERSION:
     * arbitrary alphanumeric identifiers. Match verification_key_id
     * and verification_key_version in SignatureInfo fields of the
     * diagnosis export proto. Should definitely not contain any separator
     * character used in the flag definition. A name must be defined here, but
     * verification_key_id may be omitted in the proto if there is only a
     * single signature in the proto.
     * <p>
     * each co-signing set may include multiple key values, where all values
     * within a set are verified together. multiple co-signing sets may be
     * specified in order to support key rotations.
     *
     * FIXME: Deliver authorized PHA public keys to devices.
     */
    private static final String[] TEST_PARTNER_PUBLIC_KEYS = {
            "com.google.android.apps.exposurenotification:ExampleServer-v1,MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEUczyMAkfSeoU77Nmcb1G7t7xyGCAhQqMOIVDFLFas3J+elP7CiotovigCLWj706F07j1EPL27ThRzZl7Ha9uOA==|310-v1,MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE49JY6kekDgxj3Crm4y6kEHdfoKQFSNDM4mV9cgDb+e5nOAw0GeRoRThCu9/wX5wDT2QloFoOjl2pGZHI0f3C3w=="
    };

    /**
     * If enabled, will read full header
     */
    public static boolean readFullMetadataHeader() {
        return true;
    }
}
