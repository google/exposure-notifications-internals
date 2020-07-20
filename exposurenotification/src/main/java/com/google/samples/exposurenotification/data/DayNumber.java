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

package com.google.samples.exposurenotification.data;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.TimeUnit.DAYS;

import java.nio.ByteBuffer;
import java.time.Instant;

/**
 * A day number is also known as exposure key rolling period number or EKRollingPeriod number. We
 * call it day number internally because the exposure key rolling period is 144 * 10 minutes or 24
 * hours. Each day number represents a 24-hour window that is based on Unix Epoch Time and is
 * timezone independent.
 *
 * <p>The value shall be in [0, 65535] interval. Though 18366 is the smallest legitimate day number
 * as of 2020/04/14, we allow the day number to be smaller than that to properly handle wrong system
 * clock on some devices.
 */
public class DayNumber {
    public static final DayNumber MIN_DAY_NUMBER = new DayNumber(0);
    public static final DayNumber MAX_DAY_NUMBER = new DayNumber(0xFFFF);
    private static final int SIZE_BYTES = 2;

    private final int value;

    public DayNumber(Instant instant) {
        value = getDayNumber(instant);
    }

    public DayNumber(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    /**
     * Puts the 2 bytes (big-endian) representation of this {@link DayNumber} in a {@link ByteBuffer}.
     * The 2 bytes can represent a day number till 2149/06/07.
     *
     * @param byteBuffer the destination buffer to {@link ByteBuffer#put} the byte representation.
     */
    public void putIn(ByteBuffer byteBuffer) {
        putIn(value, byteBuffer);
    }

    /** The static version of {@link #putIn(ByteBuffer)}. */
    public static void putIn(int dayNumber, ByteBuffer byteBuffer) {
        checkArgument(byteBuffer.remaining() >= SIZE_BYTES);
        byteBuffer.putShort((short) dayNumber);
    }

    /**
     * Gets a {@link DayNumber} from the 2 bytes (big-endian) representation in a {@link ByteBuffer}.
     */
    public static DayNumber getFrom(ByteBuffer byteBuffer) {
        return new DayNumber(getValueFrom(byteBuffer));
    }

    /** The static version of {@link #getFrom(ByteBuffer)}. */
    public static int getValueFrom(ByteBuffer byteBuffer) {
        checkArgument(byteBuffer.remaining() >= SIZE_BYTES);
        return ((int) byteBuffer.getShort()) & 0xFFFF; // Equivalent of Java 8 Short.toUnsignedInt().
    }

    /**
     * Returns the int representation of a day number of an {@link Instant}.
     *
     * @param instant the instant to get day number of
     * @return day number with the range in [0, 65535] interval, or undefined if the {@code instant}
     *     is later than June 6, 2149 12:00:00 AM GMT.
     */
    public static int getDayNumber(Instant instant) {
        return (int) (instant.toEpochMilli() / DAYS.toMillis(1));
    }

    public static int getSizeBytes() {
        return SIZE_BYTES;
    }
}