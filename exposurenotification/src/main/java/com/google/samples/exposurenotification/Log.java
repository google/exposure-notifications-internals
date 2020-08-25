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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Utility class to manage logging of messages with a similar API to systems in Google Play
 * Services.
 */
public class Log {
    private static final String TAG = "ExposureNotification";
    public static final Log log = new Log();

    public Logger atVerbose() {
        return new Logger(android.util.Log::v);
    }

    public Logger atFine() {
        return new Logger(android.util.Log::d);
    }

    public Logger atDebug() {
        return new Logger(android.util.Log::d);
    }

    public Logger atInfo() {
        return new Logger(android.util.Log::i);
    }

    public Logger atWarning() {
        return new Logger(android.util.Log::w);
    }

    public Logger atSevere() {
        return new Logger(android.util.Log::e);
    }

    private Log() {
    }

    public static final class Logger {
        @NonNull
        private final LogFunction<String, String, Throwable> out;
        @Nullable
        private Throwable cause;

        Logger(@NonNull LogFunction<String, String, Throwable> out) {
            this.out = out;
        }

        public Logger withCause(@NonNull Throwable cause) {
            this.cause = cause;
            return this;
        }

        public void log(@NonNull String message) {
            out.invoke(TAG, message, cause);
        }

        public void log(@NonNull String format, Object... args) {
            log(String.format(format, args));
        }
    }

    private interface LogFunction<P1, P2, P3> {
        void invoke(P1 p1, P2 p2, P3 p3);
    }
}
