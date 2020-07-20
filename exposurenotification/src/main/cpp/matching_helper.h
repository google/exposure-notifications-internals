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

#ifndef LOCATION_NEARBY_CPP_EXPOSURENOTIFICATION_JNI_MATCHING_HELPER_H_
#define LOCATION_NEARBY_CPP_EXPOSURENOTIFICATION_JNI_MATCHING_HELPER_H_

#include <jni.h>
#include <openssl/aes.h>
#include <openssl/cipher.h>
#include <openssl/crypto.h>
#include <openssl/digest.h>
#include <openssl/hkdf.h>

#include <memory>
#include <string>
#include <vector>

#include "constants.h"
#include "prefix_id_map.h"

namespace exposure {
    class MatchingHelper {
    public:
        MatchingHelper(JNIEnv *env, jobjectArray scan_record_ids);

        ~MatchingHelper();

        // Doing the matching, and return matched diagnosis_keys set.
        jobjectArray Matching(JNIEnv *env, const std::vector<std::string> &key_files);

        // Doing the matching, and return int[] for matched diagnosis_keys indexes.
        jintArray MatchingLegacy(JNIEnv *env, jobjectArray diagnosis_keys,
                                 jintArray rolling_start_numbers, int key_count);

        bool GenerateIds(const uint8_t *diagnosis_key, uint32_t rolling_start_number,
                         uint8_t *ids);

        inline jint LastProcessedKeyCount() const { return last_processed_key_count; }

    private:
        std::unique_ptr<PrefixIdMap> prefix_key_map;
        EVP_CIPHER_CTX context;
        uint8_t aesInputStorage[kIdPerKey * kIdLength];
        uint32_t last_processed_key_count;
    };
}  // namespace exposure
#endif  // LOCATION_NEARBY_CPP_EXPOSURENOTIFICATION_JNI_MATCHING_HELPER_H_