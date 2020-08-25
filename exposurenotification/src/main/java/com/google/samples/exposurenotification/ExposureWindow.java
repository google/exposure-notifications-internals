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

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Locale;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A duration of up to 30 minutes during which beacons from a TEK were observed.
 *
 * <p>Each ExposureWindow corresponds to a single TEK, but one TEK can lead to several
 * ExposureWindow due to random 15-30 minutes cuts. See getExposureWindows() for more info.
 *
 * <p>The TEK itself isn't exposed by the API.
 */
public final class ExposureWindow {

    long dateMillisSinceEpoch;

    final List<ScanInstance> scanInstances;
    @ReportType
    final int reportType;

    @Infectiousness
    final int infectiousness;

    @CalibrationConfidence
    final int calibrationConfidence;

    ExposureWindow(
            long dateMillisSinceEpoch,
            List<ScanInstance> scanInstances,
            @ReportType int reportType,
            @Infectiousness int infectiousness,
            @CalibrationConfidence int calibrationConfidence) {
        this.dateMillisSinceEpoch = dateMillisSinceEpoch;
        this.scanInstances = ImmutableList.copyOf(scanInstances);
        this.reportType = reportType;
        this.infectiousness = infectiousness;
        this.calibrationConfidence = calibrationConfidence;
    }

    /**
     * Returns the epoch time in milliseconds the exposure occurred. This will represent the start of
     * a day in UTC.
     */
    public long getDateMillisSinceEpoch() {
        return dateMillisSinceEpoch;
    }

    /**
     * Sightings of this ExposureWindow, time-ordered.
     *
     * <p>Each sighting corresponds to a scan (of a few seconds) during which a beacon with the TEK
     * causing this exposure was observed.
     */
    public List<ScanInstance> getScanInstances() {
        return scanInstances;
    }

    /**
     * Report Type of the TEK that caused this exposure
     *
     * <p>TEKs with no report type set are returned with reportType=CONFIRMED_TEST.
     *
     * <p>TEKs with RECURSIVE report type may be dropped because this report type is reserved for
     * future use.
     *
     * <p>TEKs with REVOKED or invalid report types do not lead to exposures.
     */
    @ReportType
    public int getReportType() {
        return reportType;
    }

    /**
     * Infectiousness of the TEK that caused this exposure, computed from the days since onset of
     * symptoms using the daysToInfectiousnessMapping.
     */
    @Infectiousness
    public int getInfectiousness() {
        return infectiousness;
    }

    /**
     * Confidence of the BLE Transmit power calibration of the transmitting device.
     */
    @CalibrationConfidence
    public int getCalibrationConfidence() {
        return calibrationConfidence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExposureWindow that = (ExposureWindow) o;
        return reportType == that.reportType
                && dateMillisSinceEpoch == that.dateMillisSinceEpoch
                && scanInstances.equals(that.scanInstances)
                && infectiousness == that.infectiousness
                && calibrationConfidence == that.calibrationConfidence;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
                dateMillisSinceEpoch, scanInstances, reportType, infectiousness, calibrationConfidence);
    }

    @Override
    public String toString() {
        return "ExposureWindow{"
                + "dateMillisSinceEpoch="
                + dateMillisSinceEpoch
                + ", reportType="
                + reportType
                + ", scanInstances="
                + scanInstances
                + ", infectiousness="
                + infectiousness
                + ", calibrationConfidence="
                + calibrationConfidence
                + '}';
    }

    /**
     * Builder for ExposureWindow.
     */
    public static class Builder {

        private long dateMillisSinceEpoch;
        private List<ScanInstance> scanInstances;
        @ReportType
        private int reportType;
        @Infectiousness
        private int infectiousness;
        @CalibrationConfidence
        private int calibrationConfidence;

        public Builder() {
            dateMillisSinceEpoch = 0;
            scanInstances = ImmutableList.of();
            reportType = ReportType.CONFIRMED_TEST;
            infectiousness = Infectiousness.STANDARD;
            calibrationConfidence = CalibrationConfidence.LOWEST;
        }

        public Builder setDateMillisSinceEpoch(long dateMillisSinceEpoch) {
            this.dateMillisSinceEpoch = dateMillisSinceEpoch;
            return this;
        }

        public Builder setScanInstances(List<ScanInstance> scanInstances) {
            this.scanInstances = Preconditions.checkNotNull(scanInstances);
            return this;
        }

        public Builder setReportType(@ReportType int reportType) {
            checkArgument(
                    reportType > ReportType.UNKNOWN && reportType < ReportType.REVOKED,
                    "reportType (%d) is not allowed",
                    reportType);
            this.reportType = reportType;
            return this;
        }

        public Builder setInfectiousness(@Infectiousness int infectiousness) {
            checkNotNull(
                    ExposureNotificationEnums.Infectiousness.forNumber(infectiousness),
                    String.format(Locale.getDefault(), "infectiousness (%d) is invalid", infectiousness));
            this.infectiousness = infectiousness;
            return this;
        }

        public Builder setCalibrationConfidence(@CalibrationConfidence int calibrationConfidence) {
            checkNotNull(
                    ExposureNotificationEnums.CalibrationConfidence.forNumber(calibrationConfidence),
                    String.format(
                            Locale.getDefault(), "calibrationConfidence (%d) is invalid", calibrationConfidence));
            this.calibrationConfidence = calibrationConfidence;
            return this;
        }

        public ExposureWindow build() {
            return new ExposureWindow(
                    dateMillisSinceEpoch, scanInstances, reportType, infectiousness, calibrationConfidence);
        }
    }
}