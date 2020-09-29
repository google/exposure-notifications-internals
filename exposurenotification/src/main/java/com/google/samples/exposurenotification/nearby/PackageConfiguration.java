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

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.samples.exposurenotification.safeparcel.AbstractSafeParcelable;
import com.google.samples.exposurenotification.safeparcel.SafeParcelable;

import java.util.Locale;

/**
 * Holds configuration values that can be passed onto the client app after it has finished
 * installing via {@link ExposureNotificationClient#getPackageConfiguration()}.
 */
@SafeParcelable.Class(creator = "PackageConfigurationCreator")
public final class PackageConfiguration extends AbstractSafeParcelable {

    Bundle values;

    @Constructor
    PackageConfiguration(@Param(id = 1) Bundle values) {
        this.values = values;
    }

    /** Returns a Bundle containing all configuration options. */
    public Bundle getValues() {
        return values;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof PackageConfiguration) {
            PackageConfiguration that = (PackageConfiguration) obj;
            return Objects.equal(values, that.getValues());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(values);
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "PackageConfiguration<values: %s>", values);
    }

    /** A builder for {@link PackageConfiguration}. */
    public static final class PackageConfigurationBuilder {

        private Bundle values;

        /** Sets a Bundle containing configuration options. */
        public PackageConfigurationBuilder setValues(Bundle values) {
            this.values = new Bundle(values);
            return this;
        }

        /** Builds a {@link PackageConfiguration}. */
        public PackageConfiguration build() {
            return new PackageConfiguration(values);
        }
    }
}
