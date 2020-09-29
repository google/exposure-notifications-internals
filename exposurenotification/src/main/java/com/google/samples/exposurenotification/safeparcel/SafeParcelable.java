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

package com.google.samples.exposurenotification.safeparcel;

import android.os.Parcelable;

/**
 * This interface is used only to preserve compatibility with the annotations used in the Exposure
 * Notifications code, but the actual implementation is not available here because it's not relevant
 * to the usage of Exposure Notifications.
 */
public interface SafeParcelable extends Parcelable {
    /** @hide */
    public static final String NULL = "SAFE_PARCELABLE_NULL_STRING";

    /** @hide */
    public @interface Class {
        String creator();
        boolean validate() default false;
    }

    /** @hide */
    public @interface Field {
        int id();
        String getter() default NULL;
        String type() default NULL;
        String defaultValue() default NULL;
        String defaultValueUnchecked() default NULL;
    }

    /** @hide */
    public @interface VersionField {
        int id();
        String getter() default NULL;
        String type() default NULL;
    }

    /** @hide */
    public @interface Indicator {
        String getter() default NULL;
    }

    /** @hide */
    public @interface Constructor { }

    /** @hide */
    public @interface Param {
        int id();
    }

    /** @hide */
    public @interface Reserved {
        int[] value();
    }
}