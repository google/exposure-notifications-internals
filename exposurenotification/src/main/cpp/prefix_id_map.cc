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

#include "prefix_id_map.h"

#include <jni.h>

#include <algorithm>

namespace exposure {

    namespace {
        uint16_t GetPrefixInner(const uint8_t *id) {
          uint16_t ret;
          memcpy(&ret, id, sizeof(ret));
          return ret;
        }

        bool Compare(const std::string &lhs, const std::string &rhs) {
          assert(lhs.size() >= 2);
          assert(rhs.size() >= 2);
          return GetPrefixInner(reinterpret_cast<const uint8_t *>(lhs.data())) <
                 GetPrefixInner(reinterpret_cast<const uint8_t *>(rhs.data()));
        }
    }  // namespace

    PrefixIdMap::PrefixIdMap(JNIEnv *env, jobjectArray ble_scan_records) {
      memset(prefix_end_index, 0, sizeof(int) * kIdPrefixIndexSize);
      scan_record_size = env->GetArrayLength(ble_scan_records);

      for (int i = 0; i < scan_record_size; i++) {
        jbyteArray single_id =
            (jbyteArray) env->GetObjectArrayElement(ble_scan_records, i);
        jbyte *id_bytes = env->GetByteArrayElements(single_id, 0);
        scan_records.push_back(std::string((char *) id_bytes, env->GetArrayLength(single_id)));
        env->ReleaseByteArrayElements(single_id, id_bytes, 0);
        env->DeleteLocalRef(single_id);
      }
      std::sort(scan_records.begin(), scan_records.end(), Compare);
      int last_prefix = 0;
      for (int i = 0; i < scan_record_size; i++) {
        int prefix =
            GetPrefix(reinterpret_cast<const uint8_t *>(scan_records[i].data()));
        while (last_prefix < prefix) {
          prefix_end_index[last_prefix++] = i;
        }
      }
      while (last_prefix < kIdPrefixIndexSize) {
        prefix_end_index[last_prefix++] = scan_record_size;
      }
      LOG_I("PrefixIdMap load %d scan records", scan_record_size);
    }

    PrefixIdMap::~PrefixIdMap() { scan_records.clear(); }

    int PrefixIdMap::GetIdIndex(const uint8_t *id) {
      int prefix = GetPrefix(id);
      int start_index = (prefix > 0) ? prefix_end_index[prefix - 1] : 0;
      int end_index = prefix_end_index[prefix];
      for (; start_index < end_index; start_index++) {
        if (memcmp(id, scan_records[start_index].data(), exposure::kIdLength) ==
            0) {
          return start_index;
        }
      }
      return -1;
    }

    uint16_t PrefixIdMap::GetPrefix(const uint8_t *id) {
      return GetPrefixInner(id);
    }
}  // namespace exposure
