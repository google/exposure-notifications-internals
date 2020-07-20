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

#ifndef LOCATION_NEARBY_CPP_EXPOSURENOTIFICATION_JNI_KEY_FILE_PARSER_H_
#define LOCATION_NEARBY_CPP_EXPOSURENOTIFICATION_JNI_KEY_FILE_PARSER_H_

#include <string>
#include <utility>
#include <vector>

#include "gen/exposure_key_export.pb.h"
#include "constants.h"
#include "pb_decode.h"

#define TemporaryExposureKeyNano \
  com_google_samples_exposurenotification_TemporaryExposureKey
#define TemporaryExposureKeyExportNano_keys_tag \
  com_google_samples_exposurenotification_TemporaryExposureKeyExport_keys_tag  // NOLINT
#define TemporaryExposureKeyNano_init_default \
  com_google_samples_exposurenotification_TemporaryExposureKey_init_default  // NOLINT
#define TemporaryExposureKeyNano_fields \
  com_google_samples_exposurenotification_TemporaryExposureKey_fields  // NOLINT

namespace exposure {
    static const char kFileHeader[] = "EK Export v1    ";
    constexpr static const size_t kFileHeaderSize = sizeof(kFileHeader) - 1;
    constexpr static const int kDefaultBufferSize = 64 * 1024;  // 64 KB

    class KeyFileIterator {
    public:
        // The client of KeyFileIterator transfers the responsibility of closing
        // `file` to KeyFileIterator, to avoid double closing.
        explicit KeyFileIterator(FILE *file, std::unique_ptr<char[]> buffer,
                                 pb_istream_t pb_istream)
            : file_(file),
              buffer_(std::move(buffer)),
              pb_istream_(pb_istream),
              next_tag_(0) {
          ReadUntilNextKeyTagOrEnd();
        }

        ~KeyFileIterator() { fclose(file_); }

        inline bool HasNext() const { return next_tag_ != 0; }

        // Gets the next exposure key if HasNext() return true. If HasNext() return
        // false or failed parse the proto message, return nullptr.
        inline std::unique_ptr<TemporaryExposureKeyNano> Next() {
          return ReadNextKey();
        }

    private:
        void ReadUntilNextKeyTagOrEnd();

        std::unique_ptr<TemporaryExposureKeyNano> ReadNextKey();

        static inline bool IsTagForKeys(uint32_t tag) {
          return tag == TemporaryExposureKeyExportNano_keys_tag;
        }

        FILE *file_;
        std::unique_ptr<char[]> buffer_;
        pb_istream_t pb_istream_;
        uint32_t next_tag_;
    };

    std::vector<std::unique_ptr<TemporaryExposureKeyNano>> ParseFileDirectly(
        const std::string &key_file);

    std::vector<std::unique_ptr<TemporaryExposureKeyNano>> ParseKeysDirectly(
        pb_istream_t *pb_istream);

    bool VerifyHeader(pb_istream_t *pb_istream);

    std::unique_ptr<KeyFileIterator> CreateKeyFileIterator(
        const std::string &key_file);

    pb_istream_t CreatePbInputStream(FILE *file);

// NanoPB Callback, reads the specified size in bytes into buffer from the key
// file 'stream->state', and set the internal state of stream accordingly.
    bool ReadFromFileToStream(pb_istream_t *stream, pb_byte_t *buffer,
                              std::size_t size);
}  // namespace exposure

#endif  // LOCATION_NEARBY_CPP_EXPOSURENOTIFICATION_JNI_KEY_FILE_PARSER_H_
