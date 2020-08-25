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

package com.google.samples.exposurenotification.ble.scanner;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanFilter.Builder;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build.VERSION_CODES;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.WorkSource;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.arch.core.util.Function;
import androidx.core.util.Supplier;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.samples.PlatformVersion;
import com.google.samples.exposurenotification.Log;
import com.google.samples.exposurenotification.ble.interfaces.BleDatabaseWriter;
import com.google.samples.exposurenotification.ble.interfaces.BleExecutors;
import com.google.samples.exposurenotification.ble.interfaces.BleExecutors.ScheduledTask;
import com.google.samples.exposurenotification.ble.interfaces.BleScanner;
import com.google.samples.exposurenotification.ble.utils.BluetoothUtils;
import com.google.samples.exposurenotification.ble.utils.Constants;
import com.google.samples.exposurenotification.ble.utils.Constants.StatusCode;
import com.google.samples.exposurenotification.features.ContactTracingFeature;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import static com.google.samples.exposurenotification.ble.utils.Constants.StatusCode.ERROR_BLUETOOTH_UNKNOWN;
import static com.google.samples.exposurenotification.ble.utils.Constants.StatusCode.OK;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Implementation of {@link BleScanner}, used to scan BLE advertisement for contact tracing.
 */
@TargetApi(VERSION_CODES.LOLLIPOP)
@SuppressWarnings("MissingPermission")
public class BleScannerImpl implements BleScanner {
    private static final String WAKE_LOCK_TAG = "nearby:ExposureNotificationScanner";
    private static final int MAX_RSSI_VALUE = 126;
    private static final int MIN_RSSI_VALUE = -127;
    private static final ImmutableList<Integer> CHECKING_PROFILES =
            ImmutableList.of(BluetoothProfile.A2DP, BluetoothProfile.HEADSET);
    private static final int SCAN_CHANNEL_NUMBER = 3;

    private final Context context;
    private final BleExecutors bleExecutors;
    private final Supplier<Long> currentTimeMsSupplier;
    private final BleSightingCallback bleSightingCallback;
    private final AudioManager audioManager;
    private final PowerManager powerManager;
    @VisibleForTesting
    final ScanStatusManager scanStatusManager;
    private final ScanTimePersistence scanTimePersistence;

    @Nullable
    @VisibleForTesting
    ScheduledTask scheduledTask;
    @Nullable
    private ScheduledTask piggybackScheduledTask;
    private long lastScheduleTimeMsForStartScan;
    /**
     * Master WakeLock that keeps the cpu awake while scanning.
     */
    private final WakeLock wakeLock;

    @Nullable
    private WorkSource workSource;

    private Function<Integer, Integer> randomFunction =
            integer -> new SecureRandom().nextInt(integer);
    private Predicate<List<Integer>> profileInUsingPredicator =
            BluetoothUtils::isBluetoothProfileInUsing;

    private int defaultScanWindowMs;
    private int defaultScanIntervalMs;

    // This object will be fully initialized before rescheduleScanForOpportunisticScan() is called
    // incompatible types in assignment.
    public BleScannerImpl(
            Context context,
            BleDatabaseWriter databaseWriter,
            BleExecutors bleExecutors,
            Supplier<Long> currentTimeMsSupplier,
            Runnable startScanFailCallback) {
        this.context = context;
        this.bleExecutors = bleExecutors;
        this.currentTimeMsSupplier = currentTimeMsSupplier;
        this.scanTimePersistence = new ScanTimePersistence();
        this.scanStatusManager = new ScanStatusManager();
        this.bleSightingCallback =
                new BleSightingCallback(
                        databaseWriter,
                        () -> rescheduleScanForOpportunisticScan(/* ignoreFrequency= */ false),
                        scanStatusManager,
                        scanTimePersistence,
                        startScanFailCallback);
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        // FIXME: Aquire a WakeLock
        this.wakeLock = null;
        this.defaultScanIntervalMs = getCurrentSettingsGlobalScanInterval();
        this.defaultScanWindowMs = getCurrentSettingsGlobalScanWindow();
    }

    /**
     * Verifies the device supports BLE scanning.
     * FIXME: Implement checks to ensure the device supports BLE Advertising
     */
    @Override
    public StatusCode supportScanning() {
        return OK;
    }

    @Override
    public boolean isScanning() {
        return !scanStatusManager.isStopped();
    }

    @VisibleForTesting
    @Nullable
    WorkSource getWorkSource() {
        return workSource;
    }

    @Override
    public StatusCode startScanning() {
        StatusCode statusCode = supportScanning();
        if (statusCode != OK) {
            return statusCode;
        }

        if (isScanning()) {
            return OK;
        }

        setActiveClientAsWorkSource();
        return startScanningInternal(/* isPiggyBackScan= */ false) ? OK : ERROR_BLUETOOTH_UNKNOWN;
    }

    private void setActiveClientAsWorkSource() {
        if (workSource != null) {
            // FIXME: Remove old WorkSource if needed
            workSource = null;
        }

        // FIXME: Get record of calling package
        String callingPackage = "com.google.android.apps.exposurenotification";
        // FIXME: Create a meaningful WorkSource
        workSource = new WorkSource();
        if (workSource != null) {
            wakeLock.setWorkSource(workSource);
            Log.log.atInfo().log("BleScanner set work source to %s", callingPackage);
        }
    }

    private synchronized boolean startScanningInternal(boolean isPiggyBackScan) {
        if (scanStatusManager.isScanning()) {
            Log.log.atInfo().log("Already scanning!");
            return false;
        }
        cancelAllScheduleTasks();

        maybeUpdateBleScannerInstance();

        scanStatusManager.setScanning();
        int scanMode = (int) ContactTracingFeature.scanMode();
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(scanMode);
        if (PlatformVersion.isAtLeastM()) {
            int callbackType = ScanSettings.CALLBACK_TYPE_ALL_MATCHES;
            int matchMode = ScanSettings.MATCH_MODE_STICKY;
            builder.setCallbackType(callbackType).setMatchMode(matchMode);
            if (PlatformVersion.isAtLeastO()) {
                builder.setLegacy(ContactTracingFeature.scanOnlyLegacy());
                Log.log
                        .atInfo()
                        .log("Set scanning parameters. setLegacy=%b", ContactTracingFeature.scanOnlyLegacy());
            }
            Log.log
                    .atInfo()
                    .log("Set scanning parameters. matchMode=%d, callback type=%d", matchMode, callbackType);
        }

        int durationSeconds = getScanTime(isPiggyBackScan);

        // Set new scanning parameters allowing us to more closely control which channels we're using.
        // The channel is only updated when the window value passes, meaning that with the default
        // implementation we will scan for 4s on channel 37, then 4s on channel 38, and 4s on channel
        // 39 before returning to 37. By shrinking the window, we can cause scanning to cycle through
        // all 3 channels over the course of our EN scan.
        defaultScanWindowMs = getCurrentSettingsGlobalScanWindow();
        defaultScanIntervalMs = getCurrentSettingsGlobalScanInterval();
        int scanChannelTimeMs = (durationSeconds * 1000) / SCAN_CHANNEL_NUMBER;
        setScanningSettingsGlobalParameters(scanChannelTimeMs, scanChannelTimeMs);

        ScanFilter filter = new Builder()
                .setServiceData(Constants.CONTACT_TRACER_UUID, new byte[]{0}, new byte[]{0})
                .build();
        // FIXME: Call through to BluetoothLeScanner#startScan with the `filter` declared above
        //  and the bleSightingCallback callback.
        boolean result = true;
        Log.log.atInfo().log("Starting scanning. scanMode=%d", scanMode);
        if (durationSeconds > 0) {
            Log.log.atInfo().log("Schedule stop the scan after %d seconds", durationSeconds);
            scheduledTask =
                    bleExecutors.schedule(
                            () -> {
                                if (PlatformVersion.isAtLeastM()
                                        && ContactTracingFeature.enableOpportunisticScan()) {
                                    stopScanningInternal();
                                    scanStatusManager.setPaused();
                                    startOpportunisticScanningInternal();
                                } else {
                                    pauseScanning();
                                }
                            },
                            durationSeconds,
                            SECONDS);
            if (ContactTracingFeature.wakelockForScan()) {
                Log.log.atInfo().log("Hold wakelock %d seconds for scanning", durationSeconds + 1);
                wakeLock.acquire(SECONDS.toMillis(durationSeconds + 1));
            }
        }
        return result;
    }

    @RequiresApi(VERSION_CODES.M)
    private synchronized boolean startOpportunisticScanningInternal() {
        if (scanStatusManager.isOpportunisticScanning()) {
            Log.log.atInfo().log("Already opportunistic scanning!");
            return false;
        }

        maybeUpdateBleScannerInstance();

        scanStatusManager.setOpportunisticScanning();
        ScanSettings.Builder builder =
                new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_OPPORTUNISTIC)
                        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                        .setMatchMode(ScanSettings.MATCH_MODE_STICKY);
        if (PlatformVersion.isAtLeastO()) {
            builder.setLegacy(ContactTracingFeature.scanOnlyLegacy());
        }

        ScanFilter filter = new Builder()
                .setServiceData(Constants.CONTACT_TRACER_UUID, new byte[]{0}, new byte[]{0})
                .build();
        // FIXME: Call through to BluetoothLeScanner#startScan with the `filter` declared above
        //  and the bleSightingCallback callback.
        boolean result = true;
        Log.log.atInfo().log("Starting opportunistic scanning.");
        rescheduleScanForOpportunisticScan(/* ignoreFrequency= */ true);
        return result;
    }

    private void rescheduleScanForOpportunisticScan(boolean ignoreFrequency) {
        if (!scanStatusManager.isOpportunisticScanning()) {
            return;
        }
        if (!ignoreFrequency) {
            long timeMsSinceLastSchedule = currentTimeMsSupplier.get() - lastScheduleTimeMsForStartScan;
            if (timeMsSinceLastSchedule < ContactTracingFeature.rescheduleStartScanMinIntervalMs()) {
                Log.log
                        .atInfo()
                        .log(
                                "Reschedule interval too short, timeMsSinceLastSchedule=%d",
                                timeMsSinceLastSchedule);
                return;
            }
        }

        rescheduleStartScan();
        lastScheduleTimeMsForStartScan = currentTimeMsSupplier.get();
    }

    private void rescheduleStartScan() {
        cancelAllScheduleTasks();

        int intervalSeconds = getScanInterval();
        Log.log.atInfo().log("Schedule start normal scan after %d seconds", intervalSeconds);
        scheduledTask =
                bleExecutors.schedule(
                        () -> restartScanByScheduledTask(/* isPiggyBackScan= */ false),
                        intervalSeconds,
                        SECONDS);
        schedulePiggybackScan();
    }

    private synchronized void schedulePiggybackScan() {
        if (!ContactTracingFeature.enablePiggybackScan()) {
            return;
        }

        long startTime = currentTimeMsSupplier.get();
        int piggyBackScanMinIntervalSeconds =
                (int) ContactTracingFeature.piggybackScanIntervalSeconds();
        int piggybackScanDelaySeconds =
                (PlatformVersion.isAtLeastM() && powerManager.isDeviceIdleMode())
                        ? (int) ContactTracingFeature.piggybackScanDelayWhileDeviceIdleSeconds()
                        : powerManager.isInteractive()
                        ? piggyBackScanMinIntervalSeconds
                        : (int) ContactTracingFeature.piggybackScanDelayWhileScreenOffSeconds();
        Log.log.atInfo().log("Try to piggyback scan after %d seconds", piggybackScanDelaySeconds);
        piggybackScheduledTask =
                bleExecutors.scheduleWithScheduledExecutor(
                        () -> {
                            int actualDelay =
                                    (int) MILLISECONDS.toSeconds(currentTimeMsSupplier.get() - startTime);
                            if (actualDelay - piggyBackScanMinIntervalSeconds
                                    < ContactTracingFeature.piggybackAccetableMinDelaySeconds()) {
                                Log.log
                                        .atInfo()
                                        .log(
                                                "Ignored piggyback task, actually delayed %ds is too short, cpu might"
                                                        + " still awake",
                                                actualDelay);
                                schedulePiggybackScan();
                                return;
                            }

                            Log.log.atInfo().log("Executing piggyback task, actually delayed %ds", actualDelay);
                            restartScanByScheduledTask(/* isPiggyBackScan= */ true);
                        },
                        piggybackScanDelaySeconds,
                        SECONDS);
    }

    private synchronized void restartScanByScheduledTask(boolean isPiggyBackScan) {
        if (scanStatusManager.isScanning()) {
            Log.log.atInfo().log("Already scanning, skip restartScanByScheduledTask");
            return;
        }
        Log.log.atInfo().log("restartScanByScheduledTask prepare to restart scan");
        stopScanningInternal();
        scanStatusManager.setPaused();
        startScanningInternal(isPiggyBackScan);
    }

    private synchronized void cancelAllScheduleTasks() {
        if (scheduledTask != null) {
            scheduledTask.cancel();
            scheduledTask = null;
        }
        if (piggybackScheduledTask != null) {
            piggybackScheduledTask.cancel();
            piggybackScheduledTask = null;
        }
    }

    @Override
    public StatusCode stopScanning() {
        Log.log.atInfo().log("User turn off scanning.");
        cancelAllScheduleTasks();

        boolean result = stopScanningInternal();
        if (scanStatusManager.isPaused()) {
            result = true;
        }
        scanStatusManager.setStopped();
        return result ? OK : ERROR_BLUETOOTH_UNKNOWN;
    }

    private synchronized void pauseScanning() {
        stopScanningInternal();
        scanStatusManager.setPaused();
        rescheduleStartScan();
    }

    private synchronized boolean stopScanningInternal() {
        try {
            if (!scanStatusManager.isScanning() && !scanStatusManager.isOpportunisticScanning()) {
                Log.log.atInfo().log("Not scanning!");
                return false;
            }

            Log.log.atInfo().log("Stopping scanning.");
            scanTimePersistence.markLastScanStopTime();

            // Reset the scanning parameters back to their default values. This is so that other apps also
            // performing scanning aren't affected long term by the updated values set during
            // startScanningInternal.
            setScanningSettingsGlobalParameters(defaultScanWindowMs, defaultScanIntervalMs);

            // FIXME: Call BluetoothLeScanner#stopScan with bleSightingCallback
            return true;
        } finally {
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    private void maybeUpdateBleScannerInstance() {
        // FIXME: Get a reference to a BluetoothLeScanner if necessary
    }

    private int getScanTime(boolean isPiggyBackScan) {
        if (ContactTracingFeature.scanIntervalSeconds() <= 0) {
            return 0;
        }

        int scanTime =
                (int)
                        (isPiggyBackScan
                                ? ContactTracingFeature.piggybackScanTimeSeconds()
                                : ContactTracingFeature.scanTimeSeconds());

        if (needToExtendScanTime()) {
            Log.log.atInfo().log("Extend the scan time because bluetooth is in using.");
            return (int) (scanTime + ContactTracingFeature.scanTimeExtendForProfileInUseSeconds());
        }
        return scanTime;
    }

    private boolean needToExtendScanTime() {
        if (ContactTracingFeature.extendScanOnlyForBtAudioOrCalls()) {
            if (audioManager.isBluetoothScoOn()) {
                Log.log.atInfo().log("Using bluetooth device for communication");
                return true;
            } else if (audioManager.isMusicActive() && audioManager.isBluetoothA2dpOn()) {
                Log.log.atInfo().log("Using bluetooth device for streaming audio");
                return true;
            }
            return false;
        }

        if (profileInUsingPredicator.apply(CHECKING_PROFILES)) {
            Log.log.atInfo().log("Extend the san time because some bt profiles is in using.");
            return true;
        } else {
            return false;
        }
    }

    @VisibleForTesting
    void setProfileInUsingPredicator(Predicate<List<Integer>> profileInUsingPredicator) {
        this.profileInUsingPredicator = profileInUsingPredicator;
    }

    private int getScanInterval() {
        if (ContactTracingFeature.scanIntervalSeconds() <= 0) {
            return 1;
        }

        int scanIntervalSeconds = (int) ContactTracingFeature.scanIntervalSeconds();
        int scanRandomRange = (int) ContactTracingFeature.scanIntervalRandomRangeSeconds();

        if (scanRandomRange >= scanIntervalSeconds) {
            scanRandomRange = scanIntervalSeconds - 1;
        }

        scanIntervalSeconds =
                scanIntervalSeconds - (scanRandomRange > 0 ? randomFunction.apply(scanRandomRange) : 0);

        if (powerManager.isInteractive()) {
            scanIntervalSeconds =
                    (int) (scanIntervalSeconds * ContactTracingFeature.scanIntervalRatioWhenScreenOn());
            Log.log
                    .atInfo()
                    .log(
                            "BleScanner adjust scan interval due to screen on, adjust ratio %.2f",
                            ContactTracingFeature.scanIntervalRatioWhenScreenOn());
        }

        if (audioManager.isMusicActive()) {
            scanIntervalSeconds =
                    (int) (scanIntervalSeconds * ContactTracingFeature.scanIntervalRatioWhenAudioPlayback());
            Log.log
                    .atInfo()
                    .log(
                            "BleScanner adjust scan interval due to audio playback, adjust ratio %.2f",
                            ContactTracingFeature.scanIntervalRatioWhenAudioPlayback());
        }

        int minInterval =
                Math.min(
                        (int) ContactTracingFeature.minScanIntervalSeconds(),
                        (int) ContactTracingFeature.scanIntervalSeconds());
        if (scanIntervalSeconds < minInterval) {
            scanIntervalSeconds = minInterval;
        }
        return scanIntervalSeconds;
    }

    @VisibleForTesting
    int getCurrentSettingsGlobalScanWindow() {
        return getCurrentSettingsGlobalScanParameter(
                ContactTracingFeature.lowLatencyScanWindowSettingsName(),
                (int) ContactTracingFeature.lowLatencyScanWindowDefaultValueMillis());
    }

    @VisibleForTesting
    int getCurrentSettingsGlobalScanInterval() {
        return getCurrentSettingsGlobalScanParameter(
                ContactTracingFeature.lowLatencyScanIntervalSettingsName(),
                (int) ContactTracingFeature.lowLatencyScanIntervalDefaultValueMillis());
    }

    private int getCurrentSettingsGlobalScanParameter(String name, int defaultValue) {
        return Settings.Global.getInt(context.getContentResolver(), name, defaultValue);
    }

    private void setScanningSettingsGlobalParameters(int scanWindow, int scanInterval) {
        if (ContactTracingFeature.bleScanningAdjustIntervalAndWindowValues()) {
            ContentResolver resolver = context.getContentResolver();
            Settings.Global.putInt(
                    resolver, ContactTracingFeature.lowLatencyScanWindowSettingsName(), scanWindow);
            Settings.Global.putInt(
                    resolver, ContactTracingFeature.lowLatencyScanIntervalSettingsName(), scanInterval);
        }
    }

    @VisibleForTesting
    void setRandomFunction(Function<Integer, Integer> randomFunction) {
        this.randomFunction = randomFunction;
    }

    private static class BleSightingCallback extends ScanCallback {
        private final BleDatabaseWriter databaseWriter;
        private final Runnable rescheduler;
        private final ScanTimePersistence scanTimePersistence;
        private final ScanStatusManager scanStatusManager;
        private final Runnable startScanFailCallback;

        public BleSightingCallback(
                BleDatabaseWriter databaseWriter,
                Runnable rescheduler,
                ScanStatusManager scanStatusManager,
                ScanTimePersistence scanTimePersistence,
                Runnable startScanFailCallback) {
            this.databaseWriter = databaseWriter;
            this.rescheduler = rescheduler;
            this.scanStatusManager = scanStatusManager;
            this.scanTimePersistence = scanTimePersistence;
            this.startScanFailCallback = startScanFailCallback;
        }

        @Override
        public void onScanResult(int callbackType, ScanResult scanResult) {
            ScanRecord scanRecord;
            if (scanResult == null
                    || (scanRecord = scanResult.getScanRecord()) == null
                    || scanRecord.getServiceData() == null) {
                Log.log.atWarning().log("Empty scanResult or getScanRecord, skipping.");
                return;
            }
            byte[] serviceData = scanRecord.getServiceData(Constants.CONTACT_TRACER_UUID);
            if (serviceData == null || serviceData.length <= 0) {
                Log.log.atWarning().log("Empty service data, skipping.");
                return;
            }

            if (serviceData.length < ContactTracingFeature.contactIdLength()) {
                Log.log.atWarning().log("Invalid ID:%s", Constants.fromIdByteArrayToString(serviceData));
                return;
            }

            byte[] idByte = Arrays.copyOf(serviceData, (int) ContactTracingFeature.contactIdLength());
            String id = Constants.fromIdByteArrayToString(idByte);
            byte[] metadata =
                    serviceData.length > ContactTracingFeature.contactIdLength()
                            ? Arrays.copyOfRange(
                            serviceData, (int) ContactTracingFeature.contactIdLength(), serviceData.length)
                            : new byte[0];
            int adjustedRssi = getAdjustedRssi(scanResult);
            // Default value, used if we have not seen any previous scans.
            int previousScanEpochSeconds = 0;
            // Opportunistic scans can only measure time since last stopScanning(), not from each other.
            if (scanStatusManager.isOpportunisticScanning()) {
                scanTimePersistence.markLastOpportunisticSightingTime();
                previousScanEpochSeconds = scanTimePersistence.getLastScanStopTime();
            } else {
                // Scheduled sightings measure time since last stopScanning() or since the last
                // opportunistic sighting, whichever is later.
                previousScanEpochSeconds =
                        Math.max(
                                scanTimePersistence.getLastScanStopTime(),
                                scanTimePersistence.getLastOpportunisticSightingTime());
            }
            Log.log
                    .atInfo()
                    // LINT.IfChange
                    .log(
                            "Scan device %s, type=%d, id=%s, raw_rssi=%d, calibrated_rssi=%d, meta=%s,"
                                    + " previous_scan=%d",
                            scanResult.getDevice(),
                            callbackType,
                            id,
                            scanResult.getRssi(),
                            adjustedRssi,
                            metadata.length <= 0 ? "Empty" : Constants.fromIdByteArrayToString(metadata),
                            previousScanEpochSeconds);
            // ID is used only for deduping.
            rescheduler.run();
            databaseWriter.writeBleSighting(idByte, adjustedRssi, metadata, previousScanEpochSeconds);
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.log.atWarning().log("Scan failed! errorCode=%d", errorCode);
            if (errorCode == SCAN_FAILED_APPLICATION_REGISTRATION_FAILED) {
                startScanFailCallback.run();
            }
        }

        private static int getAdjustedRssi(ScanResult scanResult) {
            int adjustedRssi = scanResult.getRssi() + (int) ContactTracingFeature.rssiOffset();
            if (adjustedRssi > MAX_RSSI_VALUE) {
                adjustedRssi = MAX_RSSI_VALUE;
            } else if (adjustedRssi < MIN_RSSI_VALUE) {
                adjustedRssi = MIN_RSSI_VALUE;
            }
            return adjustedRssi;
        }
    }

    @VisibleForTesting
    static class ScanStatusManager {
        enum Status {
            SCANNING,
            OPPORTUNISTIC_SCANNING,
            PAUSED,
            USER_STOPPED_SCANNING
        }

        private Status status = Status.USER_STOPPED_SCANNING;

        public ScanStatusManager() {
        }

        public Status getStatus() {
            return status;
        }

        public boolean isStopped() {
            return status == Status.USER_STOPPED_SCANNING;
        }

        public boolean isScanning() {
            return status == Status.SCANNING;
        }

        public boolean isOpportunisticScanning() {
            return status == Status.OPPORTUNISTIC_SCANNING;
        }

        public boolean isPaused() {
            return status == Status.PAUSED;
        }

        public void setScanning() {
            setStatus(Status.SCANNING);
        }

        public void setPaused() {
            setStatus(Status.PAUSED);
        }

        public void setOpportunisticScanning() {
            setStatus(Status.OPPORTUNISTIC_SCANNING);
        }

        public void setStopped() {
            setStatus(Status.USER_STOPPED_SCANNING);
        }

        private void setStatus(Status status) {
            this.status = status;
        }
    }

    private class ScanTimePersistence {
        private int lastScanStopTimeSeconds = 0;
        private int lastOpportunisticSightingTimeSeconds = 0;

        public ScanTimePersistence() {
        }

        public void markLastScanStopTime() {
            lastScanStopTimeSeconds = (int) MILLISECONDS.toSeconds(currentTimeMsSupplier.get());
        }

        public void markLastOpportunisticSightingTime() {
            lastOpportunisticSightingTimeSeconds =
                    (int) MILLISECONDS.toSeconds(currentTimeMsSupplier.get());
        }

        public int getLastScanStopTime() {
            return lastScanStopTimeSeconds;
        }

        public int getLastOpportunisticSightingTime() {
            return lastOpportunisticSightingTimeSeconds;
        }
    }
}