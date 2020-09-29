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

import com.google.android.play.core.tasks.Task;

import java.io.File;
import java.util.List;
import java.util.Set;

/** Interface for contact tracing APIs. */
public interface ExposureNotificationClient {

    /**
     * Action which will be invoked via a BroadcastReceiver as a callback when the user has an updated
     * exposure status. Also see {@link #EXTRA_EXPOSURE_SUMMARY} and {@link #EXTRA_TOKEN}, which will
     * be included in this broadcast.
     */
    String ACTION_EXPOSURE_STATE_UPDATED =
            "com.google.android.gms.exposurenotification.ACTION_EXPOSURE_STATE_UPDATED";

    /**
     * Action which will be invoked via a BroadcastReceiver as a callback when matching has finished
     * and no matches were found. Also see {@link #EXTRA_TOKEN}, which will be included in this
     * broadcast.
     */
    String ACTION_EXPOSURE_NOT_FOUND =
            "com.google.android.gms.exposurenotification.ACTION_EXPOSURE_NOT_FOUND";

    /**
     * Extra attached to the {@link #ACTION_EXPOSURE_STATE_UPDATED} broadcast, giving a summary of the
     * exposure details detected. Also see {@link #getExposureSummary}.
     *
     * @deprecated {@link ExposureSummary} is no longer provided when using the {@link
     *     #getExposureWindows} API. Instead, use {@link #getDailySummaries}.
     */
    @Deprecated
    String EXTRA_EXPOSURE_SUMMARY =
            "com.google.android.gms.exposurenotification.EXTRA_EXPOSURE_SUMMARY";

    /**
     * Extra attached to the {@link #ACTION_EXPOSURE_STATE_UPDATED} broadcast, providing the token
     * associated with the {@link #provideDiagnosisKeys} request.
     *
     * @deprecated Tokens are no longer used. Instead, prefer using the tokenless versions of {@link
     *     #provideDiagnosisKeys}, {@link #getExposureWindows}, and {@link #getDailySummaries}.
     */
    @Deprecated String EXTRA_TOKEN = "com.google.android.gms.exposurenotification.EXTRA_TOKEN";

    /** Activity action which shows the exposure notification settings screen. */
    String ACTION_EXPOSURE_NOTIFICATION_SETTINGS =
            "com.google.android.gms.settings.EXPOSURE_NOTIFICATION_SETTINGS";

    /**
     * Action which will be invoked via a BroadcastReceiver when the user modifies the state of
     * exposure notifications via the Google Settings page. {@link #EXTRA_SERVICE_STATE} will be
     * included as part of this broadcast.
     */
    String ACTION_SERVICE_STATE_UPDATED =
            "com.google.android.gms.exposurenotification.ACTION_SERVICE_STATE_UPDATED";

    /**
     * Boolean extra attached to the {@link #ACTION_SERVICE_STATE_UPDATED} broadcast signifying
     * whether the service is enabled or disabled.
     */
    String EXTRA_SERVICE_STATE = "com.google.android.gms.exposurenotification.EXTRA_SERVICE_STATE";

    /**
     * Token to be used with ExposureWindows API. Must be used with {@link #provideDiagnosisKeys}
     * request when later using {@link #getExposureWindows}.
     *
     * @deprecated Tokens are no longer used. Instead, prefer using the tokenless versions of {@link
     *     #provideDiagnosisKeys}, {@link #getExposureWindows}, and {@link #getDailySummaries}.
     */
    @Deprecated String TOKEN_A = "TYZWQ32170AXEUVCDW7A";

    /**
     * Settings.Global key for indicating whether a device supports locationless scanning for Exposure
     * Notifications.
     *
     * @hide
     */
    public static final String LOCATIONLESS_SCANNING_SUPPORTED_SETTING =
            "bluetooth_sanitized_exposure_notification_supported";

    /**
     * Starts BLE broadcasts and scanning based on the defined protocol.
     *
     * <p>If not previously started, this shows a user dialog for consent to start exposure detection
     * and get permission.
     *
     * <p>Callbacks regarding exposure status will be provided via a BroadcastReceiver. Clients should
     * register a receiver in their AndroidManifest which can handle the following action:
     *
     * <ul>
     *   <li><code>com.google.android.gms.exposurenotification.ACTION_EXPOSURE_STATE_UPDATED</code>
     * </ul>
     *
     * This receiver should also be guarded by the <code>
     * com.google.android.gms.nearby.exposurenotification.EXPOSURE_CALLBACK</code> permission so that
     * other apps are not able to fake this broadcast.
     */
    Task<Void> start();

    /**
     * Starts BLE broadcasts and scanning for a given app.
     *
     * <p>Same as {@link #start()}, but could be called by a 1P client on behalf of the app. For
     * example, this could be useful for Play Store.
     *
     * @param packageName the package of the app about which the requester inquires
     * @param appName the human readable name of that app to be displayed in the resulted UI
     * @param signatureHash the signature of that app
     * @hide
     */
    Task<Void> startForPackage(String packageName, String appName, byte[] signatureHash);

    /**
     * Starts BLE broadcasts and scanning for a given app.
     *
     * <p>Same as {@link #start()}, but could be called by a 1P client on behalf of the app. For
     * example, this could be useful for Play Store.
     *
     * @param packageName the package of the app about which the requester inquires
     * @param appName the human readable name of that app to be displayed in the resulted UI
     * @param signatureHash the signature of that app
     * @param skipOptInDialog whether or not the opt in dialog should be skipped. This dialog should
     *     only be skipped in the case where the caller has displayed the appropriate approval screens
     *     to the user already and have gotten opt in from the user.
     * @hide
     */
    Task<Void> startForPackage(
            String packageName, String appName, byte[] signatureHash, boolean skipOptInDialog);

    /**
     * Starts BLE broadcasts and scanning for a given app.
     *
     * <p>Same as {@link #start()}, but could be called by a 1P client on behalf of the app. For
     * example, this could be useful for Play Store.
     *
     * @param packageName the package of the app about which the requester inquires
     * @param appName the human readable name of that app to be displayed in the resulted UI
     * @param signatureHash the signature of that app
     * @param skipOptInDialog whether or not the opt in dialog should be skipped. This dialog should
     *     only be skipped in the case where the caller has displayed the appropriate approval screens
     *     to the user already and have gotten opt in from the user.
     * @param configuration settings which may be passed onto the app after it has finished starting.
     * @hide
     */
    Task<Void> startForPackage(
            String packageName,
            String appName,
            byte[] signatureHash,
            boolean skipOptInDialog,
            PackageConfiguration configuration);

    /**
     * Disables advertising and scanning. Contents of the database and keys will remain.
     *
     * <p>If the client app has been uninstalled by the user, this will be automatically invoked and
     * the database and keys will be wiped from the device.
     */
    Task<Void> stop();

    /** Indicates whether contact tracing is currently running for the requesting app. */
    Task<Boolean> isEnabled();

    /**
     * Indicates whether contact tracing is currently running for a given app.
     *
     * <p>For use by 1P clients only.
     *
     * @param packageName the package of the app about which the requester inquires
     * @param signatureHash the signature of that app
     * @hide
     */
    Task<Boolean> isEnabledForPackage(String packageName, byte[] signatureHash);

    /**
     * Gets {@link TemporaryExposureKey} history to be stored on the server.
     *
     * <p>This should only be done after proper verification is performed on the client side that the
     * user is diagnosed positive. Each key returned will have an unknown transmission risk level,
     * clients should choose an appropriate risk level for these keys before uploading them to the
     * server.
     *
     * <p>The keys provided here will only be from previous days; keys will not be released until
     * after they are no longer an active exposure key.
     *
     * <p>This shows a user permission dialog for sharing and uploading data to the server.
     */
    Task<List<TemporaryExposureKey>> getTemporaryExposureKeyHistory();

    /**
     * Provides a list of diagnosis key files for exposure checking. The files are to be synced from
     * the server. Old diagnosis keys (for example older than 14 days), will be ignored.
     *
     * <p>Diagnosis keys will be stored and matching will be performed in the near future, after which
     * you’ll receive a broadcast with the {@link #ACTION_EXPOSURE_STATE_UPDATED} action. If no
     * matches are found, you'll receive an {@link #ACTION_EXPOSURE_NOT_FOUND} action.
     *
     * <p>The diagnosis key files must be signed appropriately. Exposure configuration options can be
     * provided to tune the matching algorithm. A unique token for this batch can also be provided,
     * which will be used to associate the matches with this request as part of {@link
     * #getExposureSummary} and {@link #getExposureInformation}. Alternatively, the same token can be
     * passed in multiple times to concatenate results.
     *
     * <p>After the result Task has returned, keyFiles can be deleted.
     *
     * <p>Results for a given token remain for 14 days.
     *
     * @deprecated Tokens and configuration are no longer used. Instead, prefer using the tokenless,
     *     configuration-less version of {@link #provideDiagnosisKeys}.
     */
    @Deprecated
    Task<Void> provideDiagnosisKeys(
            List<File> keyFiles, ExposureConfiguration configuration, String token);

    /**
     * Provides a list of diagnosis key files for exposure checking. The files are to be synced from
     * the server. Old diagnosis keys (for example older than 14 days), will be ignored.
     *
     * <p>Diagnosis keys will be stored and matching will be performed in the near future, after which
     * you’ll receive a broadcast with the {@link #ACTION_EXPOSURE_STATE_UPDATED} action. If no
     * matches are found, you'll receive an {@link #ACTION_EXPOSURE_NOT_FOUND} action.
     *
     * <p>The diagnosis key files must be signed appropriately. Results from this request can also be
     * queried at any time via {@link #getExposureWindows} and {@link #getDailySummaries}.
     *
     * <p>After the result Task has returned, keyFiles can be deleted.
     *
     * <p>Results remain for 14 days.
     */
    Task<Void> provideDiagnosisKeys(List<File> keyFiles);

    /**
     * Provides diagnosis key files for exposure checking. The files are to be synced from the server.
     * Old diagnosis keys (for example older than 14 days), will be ignored.
     *
     * <p>Diagnosis keys will be stored and matching will be performed in the near future, after which
     * you’ll receive a broadcast with the {@link #ACTION_EXPOSURE_STATE_UPDATED} action. If no
     * matches are found, you'll receive an {@link #ACTION_EXPOSURE_NOT_FOUND} action.
     *
     * <p>The diagnosis key files must be signed appropriately. Results from this request can also be
     * queried at any time via {@link #getExposureWindows} and {@link #getDailySummaries}.
     *
     * <p>After the result Task has returned, files can be deleted.
     *
     * <p>Results remain for 14 days.
     *
     * <p>This method is identical to providing a list of key files, but skips checking the EN version
     * and assumes the provider can be used.
     */
    Task<Void> provideDiagnosisKeys(DiagnosisKeyFileProvider provider);

    /**
     * Retrieves the list of exposure windows corresponding to the TEKs given to provideKeys with
     * token=TOKEN_A.
     *
     * <p>Long exposures to one TEK are split into windows of up to 30 minutes of scans, so a given
     * TEK may lead to several exposure windows if beacon sightings for it spanned more than 30
     * minutes. The link between them (the fact that they all correspond to the same TEK) is lost
     * because those windows are shuffled before being returned and the underlying TEKs are not
     * exposed by the API.
     *
     * <p>The provided token must be TOKEN_A.
     *
     * @deprecated Tokens are no longer used. Instead, prefer using the tokenless version of {@link
     *     #getExposureWindows}.
     */
    @Deprecated
    Task<List<ExposureWindow>> getExposureWindows(String token);

    /**
     * Retrieves the list of exposure windows corresponding to the TEKs given to {@link
     * #provideDiagnosisKeys}.
     *
     * <p>Long exposures to one TEK are split into windows of up to 30 minutes of scans, so a given
     * TEK may lead to several exposure windows if beacon sightings for it spanned more than 30
     * minutes. The link between them (the fact that they all correspond to the same TEK) is lost
     * because those windows are shuffled before being returned and the underlying TEKs are not
     * exposed by the API.
     */
    Task<List<ExposureWindow>> getExposureWindows();

    /**
     * Gets a summary of the exposure calculation for the token, which should match the token provided
     * in {@link #provideDiagnosisKeys}.
     *
     * @deprecated When using the {@link ExposureWindow} API, use {@link #getDailySummaries} instead.
     */
    @Deprecated
    Task<ExposureSummary> getExposureSummary(String token);

    /**
     * Gets detailed information about exposures that have occurred related to the provided token,
     * which should match the token provided in {@link #provideDiagnosisKeys}.
     *
     * <p>When multiple {@link ExposureInformation} objects are returned, they can be:
     *
     * <ul>
     *   <li>Multiple encounters with a single diagnosis key.
     *   <li>Multiple encounters with the same device across key rotation boundaries.
     *   <li>Encounters with multiple devices.
     * </ul>
     *
     * Records of calls to this method will be retained and viewable by the user.
     *
     * @deprecated When using the {@link ExposureWindow} API, use {@link #getExposureWindows} instead.
     */
    @Deprecated
    Task<List<ExposureInformation>> getExposureInformation(String token);

    /** Gets the current Exposure Notification version. */
    Task<Long> getVersion();

    /** Gets {@link CalibrationConfidence} of the current device. */
    Task<Integer> getCalibrationConfidence();

    /**
     * Retrieves the per-day exposure summaries associated with the provided configuration.
     *
     * <p>A valid configuration must be provided to compute the summaries.
     */
    Task<List<DailySummary>> getDailySummaries(DailySummariesConfig dailySummariesConfig);

    /**
     * Sets the diagnosis keys data mapping if it wasn't already changed recently.
     *
     * <p>If called twice within 7 days, the second call will have no effect and will raise an
     * exception with status code FAILED_RATE_LIMITED.
     */
    Task<Void> setDiagnosisKeysDataMapping(DiagnosisKeysDataMapping diagnosisKeysMetadataMapping);

    /** Retrieves the current {@link DiagnosisKeysDataMapping}. */
    Task<DiagnosisKeysDataMapping> getDiagnosisKeysDataMapping();

    /**
     * Checks whether the device supports Exposure Notification BLE scanning without requiring
     * location to be enabled first.
     */
    boolean deviceSupportsLocationlessScanning();

    /** Gets the current Exposure Notification status. */
    Task<Set<ExposureNotificationStatus>> getStatus();

    /**
     * Retrieves the associated {@link PackageConfiguration} for the calling package. Note that this
     * value can be null if no configuration was when starting.
     */
    Task<PackageConfiguration> getPackageConfiguration();
}