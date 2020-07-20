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

#include "nanopb_encoder.h"

#include "constants.h"
#include "pb_encode.h"

namespace exposure {
    std::string EncodeTemporaryExposureKey(
        const TemporaryExposureKeyNano *temporary_exposure_key) {
      uint8_t write_buffer[kEncodedBufferSize];
      pb_ostream_t stream =
          pb_ostream_from_buffer(write_buffer, kEncodedBufferSize);
      if (!pb_encode(&stream, TemporaryExposureKeyNano_fields,
                     temporary_exposure_key)) {
        LOG_E("Failed to encode exposure key");
        return nullptr;
      }

      return std::string(reinterpret_cast<char *>(write_buffer),
                         stream.bytes_written);
    }
}  // namespace exposure
