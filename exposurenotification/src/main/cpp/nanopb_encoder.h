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

#ifndef LOCATION_NEARBY_CPP_EXPOSURENOTIFICATION_JNI_NANOPB_ENCODER_H_
#define LOCATION_NEARBY_CPP_EXPOSURENOTIFICATION_JNI_NANOPB_ENCODER_H_

#include <string>

#include "gen/exposure_key_export.pb.h"
#include "key_file_parser.h"

namespace exposure {
    constexpr static const int kEncodedBufferSize = 64;

    std::string EncodeTemporaryExposureKey(
        const TemporaryExposureKeyNano *temporary_exposure_key);
}  // namespace exposure
#endif  // LOCATION_NEARBY_CPP_EXPOSURENOTIFICATION_JNI_NANOPB_ENCODER_H_
