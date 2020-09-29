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

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import androidx.annotation.Nullable;

import com.google.samples.exposurenotification.annotations.Hide;
import com.google.samples.exposurenotification.threads.TracingHandler;
import com.google.samples.matching.BuildConfig;

/**
 * Bindable service which will be used to wake up a valid Exposure Notification client app in the
 * case where it has been force stopped and is no longer able to provide diagnosis key files in the
 * background.
 *
 * <p>This service does no actual work. It creates a Messenger that can accept messages, but only
 * then logs any messages that it receives. It is expected for this service to be unbound from
 * shortly after being bound.
 *
 * @hide
 */
@Hide
public class WakeUpService extends Service {

    private static final String TAG = "WakeUpService";

    /** Identifies the name of this service for later binding. */
    public static final String SERVICE_NAME =
            "com.google.android.gms.nearby.exposurenotification.WakeUpService";

    /** A simple handler which logs incoming messages. */
    private static class LoggingHandler extends TracingHandler {
        @Override
        public void handleMessage(Message msg) {
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "Received message " + msg.what);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "Waking up due to EN binding.");
        }
        return new Messenger(new LoggingHandler()).getBinder();
    }
}