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

#include "matching_helper.h"

#include <openssl/aes.h>
#include <openssl/cipher.h>
#include <openssl/crypto.h>
#include <openssl/digest.h>
#include <openssl/hkdf.h>
#include <stdio.h>

#include <string>
#include <vector>

#include "key_file_parser.h"
#include "nanopb_encoder.h"
#include "prefix_id_map.h"

constexpr char hexmap[] = {'0', '1', '2', '3', '4', '5', '6', '7',
                           '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

std::string hexStr(const unsigned char *data, int len) {
  std::string s(len * 2, ' ');
  for (int i = 0; i < len; ++i) {
    s[2 * i] = hexmap[(data[i] & 0xF0) >> 4];
    s[2 * i + 1] = hexmap[data[i] & 0x0F];
  }
  return s;
}

extern "C" {

namespace exposure {
    MatchingHelper::MatchingHelper(JNIEnv *env, jobjectArray scan_record_ids) {
      EVP_CIPHER_CTX_init(&context);

      prefix_key_map = std::make_unique<PrefixIdMap>(env, scan_record_ids);
      for (int i = 0; i < kIdPerKey; i++) {
        memcpy(&aesInputStorage[i * kIdLength], kRpiPaddedData,
               kRpiPaddedDataLength);
      }
      last_processed_key_count = 0;
    }

    MatchingHelper::~MatchingHelper() { EVP_CIPHER_CTX_cleanup(&context); }

#if _BYTE_ORDER != _LITTLE_ENDIAN
#error "Must use little endian"
#endif

    // PaddedData[0 - 5]: "EN-RPI".getBytes(UTF_8).
    // PaddedData[6 - 11]: 0x000000000000.
    // PaddedData[12 -15]: enIntervalNumber, uint32 little-endian.
    bool MatchingHelper::GenerateIds(const uint8_t *diagnosis_key,
                                     uint32_t rolling_start_number, uint8_t *ids) {
      uint8_t rpi_key[kRpikLength];
      // RPIK <- HKDF(tek, NULL, UTF8("EN-PRIK"), 16).
      if (HKDF(rpi_key, kRpikLength, EVP_sha256(), diagnosis_key, kTekLength,
          /*salt=*/nullptr, /*salt_len=*/0,
               reinterpret_cast<const uint8_t *>(kHkdfInfo), kHkdfInfoLength) != 1) {
        return false;
      }

      if (EVP_EncryptInit_ex(&context, EVP_aes_128_ecb(), /*impl=*/nullptr, rpi_key,
          /*iv=*/nullptr) != 1) {
        return false;
      }

      uint32_t en_interval_number = rolling_start_number;
      for (int index = 0; index < kIdPerKey * kIdLength;
           index += kIdLength, en_interval_number++) {
        *((uint32_t *) (&aesInputStorage[index + 12])) = en_interval_number;
      }

      int out_length;
      return EVP_EncryptUpdate(&context, ids, &out_length, aesInputStorage,
                               kIdPerKey * kIdLength) == 1;
    }

    jobjectArray MatchingHelper::Matching(
        JNIEnv *env, const std::vector<std::string> &key_files) {
      std::vector<std::unique_ptr<TemporaryExposureKeyNano>> matched_keys;
      uint8_t ids[kIdPerKey * kIdLength];
      static_assert(AES_BLOCK_SIZE == kIdLength, "Incorrect kIdLength.");
      last_processed_key_count = 0;
      for (const auto &key_file : key_files) {
        LOG_I("Matching with %s", key_file.c_str());
        std::unique_ptr<KeyFileIterator> key_file_iterator(
            CreateKeyFileIterator(key_file));
        if (key_file_iterator.get() == nullptr) {
          continue;
        }
        while (key_file_iterator->HasNext()) {
          std::unique_ptr<TemporaryExposureKeyNano> key = key_file_iterator->Next();
          if (key.get() == nullptr) {
            continue;
          }
//          LOG_I("TEK: %s - %d, %d", hexStr(key->key_data.bytes, 16).c_str(),
//                key->has_rolling_start_interval_number ? key->rolling_start_interval_number : -1,
//                key->has_rolling_period ? key->rolling_period : -1);
          last_processed_key_count++;
          if (GenerateIds(key->key_data.bytes,
                          static_cast<uint32_t>(key->rolling_start_interval_number),
                          ids)) {
            for (int j = 0; j < kIdPerKey * kIdLength; j += kIdLength) {
              if (prefix_key_map->GetIdIndex(&ids[j]) >= 0) {
                matched_keys.emplace_back(std::move(key));
                break;
              }
            }
          } else {
            LOG_E("GenerateIds failed");
          }
        }
      }

      if (matched_keys.size() == 0) {
        LOG_I("Matching done, total %d keys, no key matches",
              last_processed_key_count);
        return nullptr;
      }

      LOG_I("Matching done, total %d keys, find %d keys match",
            last_processed_key_count, (int) matched_keys.size());

      jobjectArray proto_array = env->NewObjectArray(
          static_cast<jsize>(matched_keys.size()), env->FindClass("[B"), nullptr);
      for (int i = 0; i < matched_keys.size(); i++) {
        auto serialized = EncodeTemporaryExposureKey(matched_keys.at(i).get());
        jbyteArray byte_array =
            env->NewByteArray(static_cast<jsize>(serialized.size()));
        env->SetByteArrayRegion(byte_array, 0,
                                static_cast<jsize>(serialized.size()),
                                reinterpret_cast<const jbyte *>(serialized.c_str()));
        env->SetObjectArrayElement(proto_array, i, byte_array);
        env->DeleteLocalRef(byte_array);
      }
      return proto_array;
    }

    // Converts a Java jbyteArray (encoding a UTF8 string) to a native UTF8 string.
    std::string JbyteArrayToString(JNIEnv *env, jbyteArray input) {
      jint len = env->GetArrayLength(input);
      std::string output;
      output.resize(len);
      env->GetByteArrayRegion(input, 0, len,
                              reinterpret_cast<jbyte *>(&(output)[0]));
      return output;
    }

    jintArray MatchingHelper::MatchingLegacy(JNIEnv *env,
                                             jobjectArray diagnosis_keys,
                                             jintArray rolling_start_numbers,
                                             int key_count) {
      if (key_count > env->GetArrayLength(diagnosis_keys)) {
        LOG_W("Key count not match diagnosis_keys numbers");
        return nullptr;
      }
      if (key_count > env->GetArrayLength(rolling_start_numbers)) {
        LOG_W("Key count not match rollingStartNumbers");
        return nullptr;
      }
      LOG_I("Matching with %d diagnosis key", key_count);
      std::vector<int> match_indexes;
      jint *rolling_start_number_array =
          env->GetIntArrayElements(rolling_start_numbers, 0);
      uint8_t ids[kIdPerKey * kIdLength];
      static_assert(kIdLength == AES_BLOCK_SIZE, "Incorrect kIdLength.");
      uint8_t *key_bytes[kIdLength];
      for (int i = 0; i < key_count; i++) {
        jbyteArray key_array =
            (jbyteArray) env->GetObjectArrayElement(diagnosis_keys, i);
        env->GetByteArrayRegion(key_array, 0, kIdLength, (jbyte *) key_bytes);
        if (GenerateIds(reinterpret_cast<const uint8_t *>(key_bytes),
                        rolling_start_number_array[i], ids)) {
          for (int j = 0; j < kIdPerKey * kIdLength; j += kIdLength) {
            if (prefix_key_map->GetIdIndex(&ids[j]) >= 0) {
              match_indexes.push_back(i);
              break;
            }
          }
        } else {
          LOG_E("GenerateIds failed");
        }
        env->DeleteLocalRef(key_array);
      }
      env->ReleaseIntArrayElements(rolling_start_numbers,
                                   rolling_start_number_array, 0);

      if (match_indexes.size() == 0) {
        LOG_I("Matching done, no key matches");
        return nullptr;
      }

      LOG_I("Matching done, find %d keys match", (int) match_indexes.size());

      jintArray result = env->NewIntArray((jsize) match_indexes.size());
      env->SetIntArrayRegion(result, 0, match_indexes.size(), match_indexes.data());
      return result;
    }

} // namespace exposure
} // extern "C"

