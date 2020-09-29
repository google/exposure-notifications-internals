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

package com.google.samples.exposurenotification.ble.advertising;

import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.AdvertisingSet;
import android.bluetooth.le.AdvertisingSetCallback;
import android.bluetooth.le.AdvertisingSetParameters;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.util.Consumer;

import com.google.samples.exposurenotification.Log;
import com.google.samples.exposurenotification.nearby.AdvertisementPacket;
import com.google.samples.exposurenotification.ble.utils.Constants.StatusCode;
import com.google.samples.exposurenotification.features.ContactTracingFeature;

import java.util.Objects;

import static com.google.common.base.Verify.verifyNotNull;
import static com.google.samples.exposurenotification.ble.utils.Constants.CONTACT_TRACER_UUID;
import static com.google.samples.exposurenotification.ble.utils.Constants.StatusCode.ERROR_BLUETOOTH_UNKNOWN;
import static com.google.samples.exposurenotification.ble.utils.Constants.StatusCode.OK;

/**
 * BLE advertiser class. Used to advertising personal ID for contact tracing.
 */
public class BleAdvertiser {
    @Nullable
    private PlatformBasedAdvertiser advertiser;

    private boolean started = false;

    public BleAdvertiser() {
    }

    /**
     * Verifies device currently meets all constraints needed to advertise
     * FIXME: Implement checks to ensure the device supports BLE Advertising
     */
    public StatusCode supportsAdvertising() {
        return OK;
    }

    /**
     * Indicates whether the advertiser is running.
     */
    public boolean isAdvertising() {
        return started;
    }

    /**
     * Starts advertising. Subsequent calls to this function cause advertising to restart with the
     * latest packet.
     */
    public StatusCode startAdvertising(AdvertisementPacket packet) {
        /*
         * This is the key to ensuring a new, random Bluetooth MAC address is used when rotating
         * to the next RPI. When advertising is restarted, a new, random Bluetooth MAC address
         * will be used.
         */
        stopAdvertising();

        StatusCode statusCode = supportsAdvertising();
        if (statusCode != OK) {
            Log.log.atWarning().log("Advertising not supported, error code=%s", statusCode.name());
            return statusCode;
        }

        AdvertiseData data =
                new AdvertiseData.Builder()
                        .addServiceUuid(CONTACT_TRACER_UUID)
                        .addServiceData(CONTACT_TRACER_UUID, packet.getPacket())
                        .build();
        PlatformBasedAdvertiser advertiserLocal = advertiser;
        if (advertiserLocal == null) {
            advertiserLocal = advertiser = PlatformBasedAdvertiser.getInstance(this::onStartFailure);
        }

        started = (advertiserLocal != null && advertiserLocal.advertise(data));
        Log.log.atInfo().log("Start advertising, started=%b", started);
        return started ? OK : ERROR_BLUETOOTH_UNKNOWN;
    }

    /**
     * Stops advertising. Should only be called when user opts out, or contact tracing is no longer
     * necessary.
     */
    public StatusCode stopAdvertising() {
        StatusCode statusCode = OK;
        if (started) {
            // startAdvertising() will ensure that advertiser is non-null
            // noinspection ConstantConditions
            boolean stopResult = verifyNotNull(advertiser).stopAdvertising();
            statusCode = stopResult ? OK : ERROR_BLUETOOTH_UNKNOWN;
            Log.log.atInfo().log("stopAdvertising get stopResult=%b", stopResult);
        }
        resetState();
        return statusCode;
    }

    /**
     * Ensures any internal state is reset if advertising support is lost at any point (BLE turns off)
     * or if advertising is stopped.
     */
    private void resetState() {
        started = false;
    }

    private void onStartFailure(int errorCode) {
        resetState();
        Log.log.atInfo().log("Start advertising failed with %s", getErrorCodeString(errorCode));
    }

    private abstract static class PlatformBasedAdvertiser {
        @Nullable
        static PlatformBasedAdvertiser getInstance(Consumer<Integer> onFailCallback) {
            if (VERSION.SDK_INT >= VERSION_CODES.O
                    && ContactTracingFeature.advertiseUseOreoAdvertiser()) {
                return new OreoAdvertiser(onFailCallback);
            } else {
                return new BaseAdvertiser(onFailCallback);
            }
        }

        abstract boolean advertise(AdvertiseData data);

        abstract boolean stopAdvertising();
    }

    /**
     * For pre O devices.
     */
    private static class BaseAdvertiser extends PlatformBasedAdvertiser {
        private final ContactTracerAdvertiseCallback advertiseCallback;

        BaseAdvertiser(Consumer<Integer> onFailCallback) {
            advertiseCallback = new ContactTracerAdvertiseCallback(onFailCallback);
        }

        @Override
        boolean advertise(AdvertiseData data) {
            int advertiseMode = (int) ContactTracingFeature.advertiseMode();
            int txPowerLevel = (int) ContactTracingFeature.advertiseTxPower();
            AdvertiseSettings settings =
                    new AdvertiseSettings.Builder()
                            .setAdvertiseMode(advertiseMode)
                            .setTxPowerLevel(txPowerLevel)
                            .setConnectable(ContactTracingFeature.advertiseConnectable())
                            .build();

            // FIXME: Call through to BluetoothLeAdvertiser#startAdvertising
            boolean started = Objects.nonNull(advertiseCallback);
            if (started) {
                Log.log
                        .atInfo()
                        .log(
                                "Start advertising with packet (%s) mode: %s tx power level %s",
                                data, getAdvertiseModeString(advertiseMode), getTxPowerLevelString(txPowerLevel));
            }

            return started;
        }

        @Override
        boolean stopAdvertising() {
            // FIXME: Call through to BluetoothLeAdvertiser#stopAdvertising
            return Objects.nonNull(advertiseCallback);
        }

        /**
         * Notified after attempting to start advertising of whether the advertisement registration was
         * a success or not.
         */
        private static class ContactTracerAdvertiseCallback extends AdvertiseCallback {
            private final Consumer<Integer> onFailCallback;

            private ContactTracerAdvertiseCallback(Consumer<Integer> onFailCallback) {
                this.onFailCallback = onFailCallback;
            }

            @Override
            public void onStartFailure(int errorCode) {
                onFailCallback.accept(errorCode);
            }

            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.log.atInfo().log("Start advertising success");
            }
        }
    }

    /**
     * For O and above devices.
     */
    @RequiresApi(api = VERSION_CODES.O)
    private static class OreoAdvertiser extends PlatformBasedAdvertiser {
        private final ContactTracerAdvertisingSetCallback advertisingSetCallback;

        OreoAdvertiser(Consumer<Integer> onFailCallback) {
            advertisingSetCallback = new ContactTracerAdvertisingSetCallback(onFailCallback);
        }

        @Override
        boolean advertise(AdvertiseData data) {
            boolean legacyMode = ContactTracingFeature.advertiseLegacyMode();
            int advertiseInterval = (int) ContactTracingFeature.advertiseInterval();
            int txPower = (int) ContactTracingFeature.advertiseTxPowerO();
            boolean connectable = ContactTracingFeature.advertiseConnectable();
            boolean scannable = ContactTracingFeature.advertiseScannable();
            AdvertisingSetParameters parameters =
                    new AdvertisingSetParameters.Builder()
                            .setLegacyMode(legacyMode)
                            .setInterval(advertiseInterval)
                            .setTxPowerLevel(txPower)
                            .setConnectable(connectable)
                            .setScannable(scannable)
                            .build();

            // FIXME: Call through to BluetoothLeAdvertiser#startAdvertising
            boolean started = Objects.nonNull(advertisingSetCallback);
            if (started) {
                Log.log
                        .atInfo()
                        .log(
                                "Start advertising with packet (%s) interval: %d, txPower %d,"
                                        + " legacyMode=%b, connectable=%b, scannable=%b",
                                data, advertiseInterval, txPower, legacyMode, connectable, scannable);
            }

            return started;
        }

        @Override
        boolean stopAdvertising() {
            // FIXME: Call through to BluetoothLeAdvertiser#stopAdvertising
            return Objects.nonNull(advertisingSetCallback);
        }

        /**
         * Notified after attempting to start advertising of whether the advertisement registration was
         * a success or not.
         */
        private static class ContactTracerAdvertisingSetCallback extends AdvertisingSetCallback {
            private final Consumer<Integer> onFailCallback;

            private ContactTracerAdvertisingSetCallback(Consumer<Integer> onFailCallback) {
                this.onFailCallback = onFailCallback;
            }

            @Override
            public void onAdvertisingSetStarted(AdvertisingSet set, int txPower, int status) {
                Log.log.atInfo().log("Start advertising,  txPower=%d, status=%d", txPower, status);
                if (status != AdvertisingSetCallback.ADVERTISE_SUCCESS) {
                    onFailCallback.accept(status);
                }
            }
        }
    }

    private static String getErrorCodeString(int errorCode) {
        switch (errorCode) {
            case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                return "ALREADY_STARTED";
            case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                return "DATA_TOO_LARGE";
            case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                return "FEATURE_UNSUPPORTED";
            case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                return "INTERNAL_ERROR";
            case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                return "TOO_MANY_ADVERTISERS";
            default:
                return "Unknown code " + errorCode;
        }
    }

    private static String getAdvertiseModeString(int advertiseMode) {
        switch (advertiseMode) {
            case AdvertiseSettings.ADVERTISE_MODE_LOW_POWER:
                return "ADVERTISE_MODE_LOW_POWER";
            case AdvertiseSettings.ADVERTISE_MODE_BALANCED:
                return "ADVERTISE_MODE_BALANCED";
            case AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY:
                return "ADVERTISE_MODE_LOW_LATENCY";
            default:
                return "Unknown mode " + advertiseMode;
        }
    }

    private static String getTxPowerLevelString(int txPowerLevel) {
        switch (txPowerLevel) {
            case AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW:
                return "ADVERTISE_TX_POWER_ULTRA_LOW";
            case AdvertiseSettings.ADVERTISE_TX_POWER_LOW:
                return "ADVERTISE_TX_POWER_LOW";
            case AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM:
                return "ADVERTISE_TX_POWER_MEDIUM";
            case AdvertiseSettings.ADVERTISE_TX_POWER_HIGH:
                return "ADVERTISE_TX_POWER_HIGH";
            default:
                return "Unknown level " + txPowerLevel;
        }
    }
}