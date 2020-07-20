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

package com.google.samples.exposurenotification.crypto;

import androidx.annotation.Nullable;

import com.google.common.base.Preconditions;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Key derivation helper functions.
 */
public final class KeyDerivation {
    private static final int HKDF_OUTPUT_LENGTH = 16;
    private static final int HASH_LENGTH = 32;
    /**
     * The algorithm name used for Exposure Notification Cryptography.
     */
    public static final String ALGORITHM_NAME = "HmacSHA256";

    private KeyDerivation() {
    }

    /**
     * Note that this function is for Exposure Notification Cryptography Specification 1.1 only, it
     * only support 16-byte length output.
     */
    public static byte[] hkdfSha256(
            byte[] inputKeyingMaterial, @Nullable byte[] inputSalt, byte[] info, int length)
            throws CryptoException {
        try {
            Mac mac = Mac.getInstance(ALGORITHM_NAME);
            return hkdfSha256(mac, inputKeyingMaterial, inputSalt, info, length);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        }
    }

    /**
     * Note that this function is for Exposure Notification Cryptography Specification 1.1 only, it
     * only support 16-byte length output.
     */
    public static byte[] hkdfSha256(
            Mac mac, byte[] inputKeyingMaterial, @Nullable byte[] inputSalt, byte[] info, int length)
            throws CryptoException {
        Preconditions.checkArgument(mac.getAlgorithm().equals(ALGORITHM_NAME));
        if (length != HKDF_OUTPUT_LENGTH) {
            throw new CryptoException(new NoSuchAlgorithmException("Only support 16-byte."));
        }
        byte[] salt = (inputSalt == null || inputSalt.length == 0) ? new byte[HASH_LENGTH] : inputSalt;
        try {
            return hkdfSha256ExpandLength16(mac, hkdfSha256Extract(mac, inputKeyingMaterial, salt), info);
        } catch (InvalidKeyException e) {
            throw new CryptoException(e);
        }
    }

    /**
     * The HKDF (RFC 5869) extraction function, using the SHA-256 hash function. The output PRK is
     * calculated as follows: PRK = HMAC-SHA256(salt, IKM), i.e. salt as the key of hmac-sha256, and
     * ikm (input keying material) as the message.
     */
    private static byte[] hkdfSha256Extract(Mac mac, byte[] inputKeyingMaterial, byte[] salt)
            throws InvalidKeyException {
        mac.init(new SecretKeySpec(salt, ALGORITHM_NAME));

        return mac.doFinal(inputKeyingMaterial);
    }

    /**
     * HKDF (RFC 5869) expansion function, using the SHA-256 hash function, the output size is
     * 16-byte.
     */
    private static byte[] hkdfSha256ExpandLength16(Mac mac, byte[] pseudoRandomKey, byte[] info)
            throws InvalidKeyException {
        // For length being 16 cases, counter always equals to 0x01.
        byte[] counter = {0x01};
        mac.init(new SecretKeySpec(pseudoRandomKey, ALGORITHM_NAME));
        mac.update(info);

        return Arrays.copyOf(mac.doFinal(counter), HKDF_OUTPUT_LENGTH);
    }
}