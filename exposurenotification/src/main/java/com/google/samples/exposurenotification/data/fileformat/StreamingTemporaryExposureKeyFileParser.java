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

package com.google.samples.exposurenotification.data.fileformat;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.WireFormat;
import com.google.samples.exposurenotification.ExposureKeyExportProto;
import com.google.samples.exposurenotification.ExposureKeyExportProto.TemporaryExposureKeyExport;
import com.google.samples.exposurenotification.TemporaryExposureKey;
import com.google.samples.exposurenotification.matching.TemporaryExposureKeyExportV1Header;
import com.google.samples.exposurenotification.storage.CloseableIterable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A class reads and parses a temporary exposure key export file in a streaming mode meaning that
 * the memory usage is independent to the file size.
 *
 * <p>See https://developers.google.com/android/exposure-notifications/exposure-key-file-format
 */
public class StreamingTemporaryExposureKeyFileParser {

    /**
     * Reads and parses the given {@link File} as {@link CloseableIterable<TemporaryExposureKey>}.
     *
     * <p>Parsing different files concurrently is safe. Parsing the same file before the previous
     * returned {@link CloseableIterable} closes is not allowed.
     *
     * @param file The key export file
     * @return A single-use iterable that must be closed after use. Not thread-safe.
     */
    public static CloseableIterable<TemporaryExposureKey> parse(File file) throws IOException {
        return ParserIterable.create(new FileInputStream(file));
    }

    /** Same as {@link #parse(File)} but takes an {@link InputStream} instead. */
    public static CloseableIterable<TemporaryExposureKey> parse(InputStream inputStream)
            throws IOException {
        return ParserIterable.create(inputStream);
    }

    /** The {@link CloseableIterable} that handles file reading while iteration. */
    private static class ParserIterable implements CloseableIterable<TemporaryExposureKey> {
        private final InputStream inputStream;
        private final CodedInputStream codedStream;

        // The tag (that has been read) for the field that yet to be read next. At anywhere outside of
        // readUntilNextKeyTagOrEnd(), the value of nextTag can only be KEYS_FIELD_NUMBER or 0 because
        // we always skip all other fields.
        private int nextTag;

        public static ParserIterable create(InputStream inputStream) throws IOException {
            ParserIterable parser = new ParserIterable(inputStream);

            // Prime the parser. Do this from outside of the constructor, to keep the nullness
            // checker happy.
            TemporaryExposureKeyExportV1Header.readAndVerifyHeader(inputStream);
            parser.readUntilNextKeyTagOrEnd();

            return parser;
        }

        private ParserIterable(InputStream inputStream) {
            this.inputStream = inputStream;
            codedStream = CodedInputStream.newInstance(inputStream);
        }

        @Override
        public void close() throws IOException {
            inputStream.close();
        }

        @Override
        public Iterator<TemporaryExposureKey> iterator() {
            return new Iterator<TemporaryExposureKey>() {
                @Override
                public boolean hasNext() {
                    return nextTag != 0;
                }

                @Override
                public TemporaryExposureKey next() {
                    return readNextKey();
                }
            };
        }

        private void readUntilNextKeyTagOrEnd() throws IOException {
            nextTag = codedStream.readTag();
            while (nextTag != 0 && !isTagForKeys(nextTag)) {
                codedStream.skipField(nextTag);
                nextTag = codedStream.readTag();
            }
        }

        private TemporaryExposureKey readNextKey() {
            if (!isTagForKeys(nextTag)) {
                throw new NoSuchElementException("Unexpected proto buffer field");
            }

            try {
                ExposureKeyExportProto.TemporaryExposureKey.Builder builder =
                        ExposureKeyExportProto.TemporaryExposureKey.newBuilder();
                codedStream.readMessage(builder, ExtensionRegistryLite.getEmptyRegistry());
                readUntilNextKeyTagOrEnd();
                return new TemporaryExposureKeyConverter().convert(builder.build());
            } catch (IOException e) {
                throw new NoSuchElementException("IOException: " + e.getMessage());
            }
        }
    }

    private static boolean isTagForKeys(int tag) {
        return WireFormat.getTagFieldNumber(tag) == TemporaryExposureKeyExport.KEYS_FIELD_NUMBER;
    }

    private StreamingTemporaryExposureKeyFileParser() {}
}