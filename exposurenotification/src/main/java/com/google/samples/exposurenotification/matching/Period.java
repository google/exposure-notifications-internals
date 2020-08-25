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

package com.google.samples.exposurenotification.matching;

import com.google.auto.value.AutoValue;

/** A pair of two scans. */
@AutoValue
abstract class Period {
    static Period create(TimeAndAttenuation scan1, TimeAndAttenuation scan2) {
        return new AutoValue_Period(scan1, scan2);
    }

    abstract TimeAndAttenuation scan1();

    abstract TimeAndAttenuation scan2();

    public int duration() {
        return scan2().time() - scan1().time();
    }

    public int attenuationDiff() {
        return scan2().attenuation() - scan1().attenuation();
    }

    public int calculateTimeCross(int threshold, boolean interpolate) {
        int timeCross = scan1().time();
        if (interpolate && attenuationDiff() != 0) {
            // calculate the time at which the interpolated attenuation equals the threshold.
            timeCross =
                    (int)
                            Math.round(
                                    scan1().time()
                                            + (threshold - scan1().attenuation())
                                            / (double) attenuationDiff()
                                            * duration());
        }
        return timeCross;
    }
}