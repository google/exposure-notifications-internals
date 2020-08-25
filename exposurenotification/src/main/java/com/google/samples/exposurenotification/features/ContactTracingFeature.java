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

import android.bluetooth.le.ScanSettings;

import com.google.common.primitives.Ints;

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
     * The length for which a Temporary Tracing Key is valid in multiple of id rolling period.
     */
    public static int tkRollingPeriodMultipleOfIdRollingPeriod() {
        return 144;
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
     * Whether using startAdvertisingSet to start advertising for O and above
     */
    public static boolean advertiseUseOreoAdvertiser() {
        return false;
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
     * <p>
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

    /**
     * The Settings.Global name for the BLE low latency scan window value.
     */
    public static String lowLatencyScanWindowSettingsName() {
        return "ble_scan_low_latency_window_ms";
    }

    /**
     * The Settings.Global name for the BLE low latency scan interval value.
     */
    public static String lowLatencyScanIntervalSettingsName() {
        return "ble_scan_low_latency_interval_ms";
    }

    /**
     * The default low latency scan window value to be set when scanning for EN is inactive.
     */
    public static int lowLatencyScanWindowDefaultValueMillis() {
        return 4096;
    }

    /**
     * The default low latency scan interval value to be set when scanning for EN is inactive.
     */
    public static int lowLatencyScanIntervalDefaultValueMillis() {
        return 4096;
    }

    /**
     * Minimum Scan interval between each scan.
     */
    public static int minScanIntervalSeconds() {
        return 60;
    }

    /**
     * Scan interval between each scan.
     */
    public static int scanIntervalSeconds() {
        return 300; // 5 * 60
    }

    /**
     * Ratio for scan interval while audio playback. [0.1-1]
     */
    public static float scanIntervalRatioWhenAudioPlayback() {
        return 0.6f;
    }

    /**
     * Ratio for scan interval while screen on. [0.1-1]
     */
    public static float scanIntervalRatioWhenScreenOn() {
        return 0.8f;
    }

    /**
     * If enabled, the BLE scanning interval and window will be shrunk so that over the course of
     * the BLE scan, we use all 3 channels instead of only 1.
     */
    public static boolean bleScanningAdjustIntervalAndWindowValues() {
        return true;
    }

    /**
     * Offset to add to the measured RSSI to obtain a calibrated RSS.
     * <p>
     * The offset in measured rssi between an average iphone and a test
     * device. Experimentally determined on the most popular devices, and
     * extrapolated for similar models.
     * calibrated_rssi = measured_rssi + rssi_offset
     */
    public static int rssiOffset() {
        return 0;
    }

    /**
     * Random parameter for scan interval. The actual scan interval will be
     * scan_interval_seconds - RandomInt() % scan_interval_random_range_seconds,
     * and by default it could be 210 ~ 300 seconds.
     */
    public static int scanIntervalRandomRangeSeconds() {
        return 90; // 1.5 * 60
    }

    /**
     * Scan time for piggyback scan with other app's wake ups.
     */
    public static int piggybackScanTimeSeconds() {
        return 2;
    }

    /**
     * Scan time for each scan.
     */
    public static int scanTimeSeconds() {
        return 4;
    }

    /**
     * Additional scan time while some specific bluetooth profile is in use. If condition
     * matches, the total scan time will be scan_time_seconds +
     * scan_time_extend_for_profile_in_use_seconds,
     * by default it will be 15 seconds.
     */
    public static int scanTimeExtendForProfileInUseSeconds() {
        return 13;
    }

    /**
     * Only extend scan while bluetooth audio streaming or while in calls.
     */
    public static boolean extendScanOnlyForBtAudioOrCalls() {
        return true;
    }

    /**
     * Interval for piggyback scan with other app's wake ups.
     */
    public static int piggybackScanIntervalSeconds() {
        return 60;
    }

    /**
     * Delay for scheduled piggyback scan task while device idle, in seconds.
     */
    public static int piggybackScanDelayWhileDeviceIdleSeconds() {
        return 5;
    }

    /**
     * Delay for scheduled piggyback scan task while screen off, in seconds.
     */
    public static int piggybackScanDelayWhileScreenOffSeconds() {
        return 15;
    }

    /**
     * Threshold to determine that if the piggyback task is executed while cpu sleeping. If the
     * task is delayed for more than this value, then we assume that cpu is during idle mode
     * recently.
     */
    public static int piggybackAccetableMinDelaySeconds() {
        return 1;
    }

    /**
     * Master switch for piggyback scan.
     */
    public static boolean enablePiggybackScan() {
        return false;
    }

    /**
     * Whether the BLE scanner accepts only legacy advertising or not, will only take effect for
     * O and above
     */
    public static boolean scanOnlyLegacy() {
        return false;
    }

    /**
     * Hold wakelock for scanning to prevent cpu going to sleep while scanning and delay the stop
     * scan schedule.
     */
    public static boolean wakelockForScan() {
        return true;
    }

    /**
     * Use opportunistic scan between regular scans.
     */
    public static boolean enableOpportunisticScan() {
        return false;
    }

    /**
     * Scan mode for contact scan.
     */
    public static int scanMode() {
        return ScanSettings.SCAN_MODE_LOW_LATENCY;
    }

    /**
     * By design, every opportunistic scan will reschedule the low latency scan to 5 minutes
     * later, but it will reschedule too frequently if too many opportunistic scan happen in a in
     * short period. This constant is used for the minimal reschedule interval.
     */
    public static int rescheduleStartScanMinIntervalMs() {
        return 10000; // 10 * 1000
    }

    /**
     * Bucket bounds for risk score latency days.
     */
    public static List<Integer> riskScoreLatencyDaysBuckets() {
        int[] riskScoreLatencyDaysBuckets = new int[]{
                14,
                12,
                10,
                8,
                6,
                4,
                2,
        };
        return Ints.asList(riskScoreLatencyDaysBuckets);
    }

    /**
     * Bucket bounds for risk score duration.
     */
    public static List<Integer> riskScoreDurationBuckets() {
        int[] riskScoreDurationBuckets = new int[]{
                0,
                5,
                10,
                15,
                20,
                25,
                30,
        };
        return Ints.asList(riskScoreDurationBuckets);
    }

    public static List<Integer> riskScoreAttenuationValueBuckets() {
        int[] riskScoreAttenuationValueBuckets = new int[]{
                73,
                63,
                51,
                33,
                27,
                15,
                10,
        };
        return Ints.asList(riskScoreAttenuationValueBuckets);
    }

    /**
     * Minimum estimated time two devices need to be in range of each other before considered a
     * valid exposure.
     */
    public static long minExposureBucketizedDurationSeconds() {
        return 1;
    }

    /**
     * The default minimum attenuation value that must be reached within an exposure duration.
     */
    public static int defaultMinExposureAttenuationValue() {
        return 47; // ~2m distance
    }

    /**
     * Max threshold duration between scans to be considered continuous.
     * <p>
     * Note that this duration is not bucketized by scan interval, and is strictly
     * measured against the duration between two consecutive ble scans.
     */
    public static int maxInterpolationDurationSeconds() {
        // Set to be a little larger than scan interval,
        // which when the phone is in idle mode, can be up
        // to 9 minutes despite scan interval's value, to
        // make sure consecutive scans are not considered
        // individual encounters.
        return 10 * 60;
    }

    /**
     * Maximum duration (in minutes) of ExposureWindow.
     */
    public static int maxExposureWindowDurationMinutes() {
        return 30;
    }

    /**
     * Maximum minutes since last scan in ExposureWindow.
     */
    public static int maxMinutesSinceLastScan() {
        return 5;
    }

    /**
     * Default minutes since last scan in ExposureWindow, used when previous scan time is unknown.
     */
    public static int defaultMinutesSinceLastScan() {
        return 2;
    }

    /**
     * When true, clock drift (embargo) leeway will not be considered when matching near the end
     * of a key's validity period.
     */
    public static boolean ignoreEmbargoPeriodWhenMatchingNearKeyEdges() {
        return false;
    }

    /**
     * Whether to use native code to do matching
     */
    public static boolean matchingWithNative() {
        return true;
    }

    /**
     * Pre processing to filter non-matched IDs, then do the matching.
     */
    public static boolean useMatchingPreFilter() {
        return true;
    }

    /**
     * Whether to allow RECURSIVE report type in TEK.
     */
    public static boolean enableRecursiveTekReportType() {
        return false;
    }

    /**
     * Print more log for matching for debugging
     */
    public static boolean moreLogForMatching() {
        return false;
    }

    /**
     * Whether sightings of the same RPI in a single scan instance should be aggregated and only
     * min attenuation used.
     */
    public static boolean aggregateSightingsFromSingleScan() {
        return true;
    }

    /**
     * Maximum number of keys that will be passed to native for matching in one native matching call
     */
    public static int matchingWithNativeBufferKeySize() {
        return 10000;
    }

    /**
     * If enabled, will start supporting revocation and change of status for report type.
     */
    public static boolean supportRevocationAndChangeStatusReportType() {
        return true;
    }

    /**
     * If enabled, revoked key matches will be stored in the match database (but only when
     * using ExposureWindows).
     */
    public static boolean storeMatchesForRevokedKeys() {
        return true;
    }

    /**
     * Keep existing matching logic, only add a quick filter for non-matched IDs (which should be
     * 99.9% cases).
     */
    public static boolean useMatchingFilter() {
        return true;
    }

    /**
     * Matched RPIs with a TX power higher than this bound will be rejected as outside reasonable
     * range and potentially malicious.
     */
    public static int matchingTxPowerUpperBound() {
        return 20; // dB
    }

    /**
     * Matched RPIs with a TX power lower than this bound will be rejected as outside reasonable
     * range and potentially malicious.
     */
    public static int matchingTxPowerLowerBound() {
        return -50; // dB
    }

    /**
     * Whether the native matching should use the native key file parser
     */
    public static boolean useNativeKeyParser() {
        return true;
    }

    /**
     * If enabled, all exposure results will be written at the end of matching instead of
     * intermittedly throughout the process.
     */
    public static boolean storeExposureResultsInTransaction() {
        return true;
    }

    /**
     * The number of times Report Type can be changed.
     */
    public static int allowedReportTypeTransitions() {
        return 1;
    }

    /**
     * Whether to enable the compact storage format for contact record
     * data store (sightings storage). Instead storing all information for
     * individual received BLE packet, compact format aggregates some fields
     * while keeping other required packet-specific information unaggregated.
     */
    public static boolean contactRecordStoreCompactFormatEnabled() {
        return false;
    }
}
