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

#ifndef LOCATION_NEARBY_CPP_EXPOSURENOTIFICATION_JNI_PREFIX_ID_MAP_H_
#define LOCATION_NEARBY_CPP_EXPOSURENOTIFICATION_JNI_PREFIX_ID_MAP_H_

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>

#include <string>
#include <vector>

#include "constants.h"

namespace exposure {
    constexpr static const int kIdPrefixIndexSize = 65536;

    class PrefixIdMap {
    public:
        int prefix_end_index[exposure::kIdPrefixIndexSize];
        std::vector<std::string> scan_records;
        int scan_record_size;

        // scanRecordIds is a byte[][], which contains all scanned ID from database.
        // Due to we'll only try to copy its value to scan_records, so it's not
        // owned by PrefixIdMap after construction.
        PrefixIdMap(JNIEnv *env, jobjectArray ble_scan_records);

        ~PrefixIdMap();

        int GetIdIndex(const uint8_t *id);

        uint16_t GetPrefix(const uint8_t *id);
    };
}  // namespace exposure
#endif  // LOCATION_NEARBY_CPP_EXPOSURENOTIFICATION_JNI_PREFIX_ID_MAP_H_
