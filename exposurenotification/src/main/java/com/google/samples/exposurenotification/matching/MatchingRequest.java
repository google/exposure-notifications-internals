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

import androidx.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.samples.exposurenotification.ExposureConfiguration;
import com.google.samples.exposurenotification.TemporaryExposureKey;
import com.google.samples.exposurenotification.data.fileformat.StreamingTemporaryExposureKeyFileParser;
import com.google.samples.exposurenotification.storage.CloseableIterable;
import com.google.samples.exposurenotification.storage.CloseableIterables;
import com.google.samples.exposurenotification.storage.ExposureConfigurationConverter;
import com.google.samples.exposurenotification.storage.ExposureConfigurationProto;
import com.google.samples.exposurenotification.storage.MatchingRequestProto;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.google.samples.exposurenotification.Log.log;

/**
 * Encloses information about a matching request.
 */
public class MatchingRequest {
    private final long requestId;
    private final String shortenedPackageName;
    private final MatchingRequestProto matchingRequestProto;

    public MatchingRequest(long requestId, MatchingRequestProto matchingRequestProto) {
        this.requestId = requestId;
        this.matchingRequestProto = matchingRequestProto;
        shortenedPackageName = createShortenedPackageName(matchingRequestProto.getPackageName());
    }

    /**
     * The generated requestId.
     */
    public long requestId() {
        return requestId;
    }

    /**
     * Package name of client request.
     */
    public String packageName() {
        return matchingRequestProto.getPackageName();
    }

    /**
     * Gets the shortened version of the package name, used primarily for logging.
     */
    public String getShortenedPackageName() {
        return shortenedPackageName;
    }

    /**
     * Signature hash of client app.
     */
    public byte[] signatureHash() {
        return matchingRequestProto.getSignatureHash().toByteArray();
    }

    /**
     * Token associated with request.
     */
    public String token() {
        return matchingRequestProto.getToken();
    }

    /**
     * The SHA-256 has associated with the key files in this matching request, in base64.
     */
    public String keyFilesHash() {
        return matchingRequestProto.getKeyFilesHash();
    }

    public long requestTimeMillis() {
        return matchingRequestProto.getRequestTimeMillis();
    }

    /**
     * exposure configuration for request.
     */
    public ExposureConfiguration exposureConfiguration() {
        return new ExposureConfigurationConverter()
                .reverse()
                .convert(matchingRequestProto.getExposureConfiguration());
    }

    public ExposureConfigurationProto exposureConfigurationProto() {
        return matchingRequestProto.getExposureConfiguration();
    }

    /**
     * Gets the diagnosis keys provided with this request. The returned closable must be closed
     * after use so that the underlying resources (files) can be closed correctly.
     */
    public CloseableIterable<TemporaryExposureKey> diagnosisKeys() {
        List<CloseableIterable<TemporaryExposureKey>> keyIterables = new ArrayList<>();
        for (String filePath : matchingRequestProto.getKeyFilesList()) {
            try {
                keyIterables.add(StreamingTemporaryExposureKeyFileParser.parse(new File(filePath)));
            } catch (IOException e) {
                log.atSevere().withCause(e).log("Can't parse keys from %s", filePath);
            }
        }
        return CloseableIterables.concat(keyIterables);
    }

    public ImmutableList<String> diagnosisKeyFiles() {
        return ImmutableList.copyOf(matchingRequestProto.getKeyFilesList());
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId, matchingRequestProto);
    }

    @Override
    public boolean equals(@Nullable Object object) {
        if (object instanceof MatchingRequest) {
            MatchingRequest that = (MatchingRequest) object;
            return this.requestId == that.requestId
                    && this.matchingRequestProto.equals(that.matchingRequestProto);
        }
        return false;
    }

    /**
     * Shortens package name to the string past the second to last period.
     */
    static String createShortenedPackageName(String packageName) {
        if (packageName.lastIndexOf(".") == -1) {
            return packageName;
        }
        return packageName.substring(
                packageName.lastIndexOf(".", packageName.lastIndexOf(".") - 1) + 1);
    }
}