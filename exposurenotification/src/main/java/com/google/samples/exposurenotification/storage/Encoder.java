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

package com.google.samples.exposurenotification.storage;

import com.google.common.collect.ImmutableList;

import java.nio.ByteBuffer;
import java.util.List;

import static com.google.common.hash.Hashing.sha256;

/**
 * An encoder that encodes some input into a byte array.
 *
 * <p>To use, extends this class and create a constructor that accepts an input of any type.
 * Implement {@link #encodeToBuffer(ByteBuffer)} and use the class by instantiating and calling
 * {@link #encode()}. To chain a series of these, see {@link SerialEncoder}.
 */
public abstract class Encoder {

    /**
     * "Encode" or "decode" a byte array for saving or restoring from leveldb. Mainly a dummy wrapper
     * class for easy chaining. See {@link SerialEncoder}.
     */
    public static class BytesEncoder extends Encoder {

        private final byte[] bytes;

        public BytesEncoder(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        protected void encodeToBuffer(ByteBuffer byteBuffer) {
            byteBuffer.put(bytes);
        }

        @Override
        public int numBytes() {
            return bytes.length;
        }
    }

    /**
     * An encoder that takes a list of encoders to encode a series of value.
     */
    public static abstract class SerialEncoder extends Encoder {

        private final List<Encoder> encoders;

        protected SerialEncoder(ImmutableList<Encoder> encoders) {
            this.encoders = encoders;
        }

        @Override
        protected void encodeToBuffer(ByteBuffer byteBuffer) {
            for (Encoder encoder : encoders) {
                encoder.encodeToBuffer(byteBuffer);
            }
        }

        @Override
        protected int numBytes() {
            int numBytes = 0;
            for (Encoder encoder : encoders) {
                numBytes += encoder.numBytes();
            }
            return numBytes;
        }
    }

    /**
     * Encodes a {@link String} into a byte array by Sha256ing the {@link
     * java.nio.charset.StandardCharsets#UTF_8} encoding of the input string.
     */
    @SuppressWarnings("UnstableApiUsage")
    public static class Sha256StringEncoder extends Encoder {

        private final String string;

        // TODO(wukevin,enxun): Revisit semantics around Encoder classes.
        public Sha256StringEncoder(String string) {
            this.string = string;
        }

        @Override
        protected void encodeToBuffer(ByteBuffer byteBuffer) {
            byteBuffer.put(sha256().hashUnencodedChars(string).asBytes());
        }

        @Override
        protected int numBytes() {
            return sha256().bits() / 8;
        }
    }

    /**
     * Encodes value with {@link #numBytes()} into {@link ByteBuffer}.
     */
    protected abstract void encodeToBuffer(ByteBuffer byteBuffer);

    /**
     * The number of bytes that will be written when {@link #encodeToBuffer(ByteBuffer)} is called.
     */
    protected abstract int numBytes();

    /**
     * Returns a byte array containing the encoded version {@link Encoder} wrapped values.
     */
    public byte[] encode() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(numBytes());
        encodeToBuffer(byteBuffer);
        return byteBuffer.array();
    }
}