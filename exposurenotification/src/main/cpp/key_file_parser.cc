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

#include "key_file_parser.h"

namespace exposure {
    bool VerifyHeader(pb_istream_t *pb_istream) {
      char header[kFileHeaderSize] = {0};
      pb_read(pb_istream, reinterpret_cast<pb_byte_t *>(header), kFileHeaderSize);
      int result = memcmp(header, kFileHeader, kFileHeaderSize);
      if (result != 0) {
        LOG_E("Key file header mismatch %s, expected %s", header, kFileHeader);
        return false;
      }
      return true;
    }

    std::unique_ptr<KeyFileIterator> CreateKeyFileIterator(
        const std::string &key_file) {
      FILE *file = fopen(key_file.c_str(), "rb");
      if (file == nullptr) {
        LOG_E("Failed to open file %s", key_file.c_str());
        return nullptr;
      }

      auto buffer = std::make_unique<char[]>(kDefaultBufferSize);
      setbuf(file, buffer.get());

      auto pb_istream = CreatePbInputStream(file);
      if (!VerifyHeader(&pb_istream)) {
        LOG_E("Failed to verify the file header %s", key_file.c_str());
        fclose(file);
        return nullptr;
      }

      LOG_I("Created iterator for %s", key_file.c_str());
      return std::make_unique<KeyFileIterator>(file, std::move(buffer), pb_istream);
    }

    void KeyFileIterator::ReadUntilNextKeyTagOrEnd() {
      pb_wire_type_t wire_type;
      bool eof = false;
      while (!eof) {
        pb_decode_tag(&pb_istream_, &wire_type, &next_tag_, &eof);
        if (IsTagForKeys(next_tag_)) {
          break;
        }
        pb_skip_field(&pb_istream_, wire_type);
      }
    }

    std::unique_ptr<TemporaryExposureKeyNano> KeyFileIterator::ReadNextKey() {
      if (!IsTagForKeys(next_tag_)) {
        LOG_E("Unexpected proto buffer field");
        return nullptr;
      }

      TemporaryExposureKeyNano key = TemporaryExposureKeyNano_init_default;
      if (!pb_decode_delimited(&pb_istream_, TemporaryExposureKeyNano_fields,
                               &key)) {
        LOG_E("Failed to decode exposure key");
        return nullptr;
      }

      ReadUntilNextKeyTagOrEnd();
      return std::make_unique<TemporaryExposureKeyNano>(key);
    }

    bool ReadFromFileToStream(pb_istream_t *stream, pb_byte_t *buffer,
                              size_t size) {
      auto file = reinterpret_cast<FILE *>(stream->state);
      size_t count = fread(buffer, 1, size, file);
      if (ferror(file)) {
        LOG_E("Failed to read input file stream");
        return false;
      }
      if (feof(file)) {
        stream->bytes_left = 0;
      }
      return count == size;
    }

    pb_istream_t CreatePbInputStream(FILE *file) {
      pb_istream_t pb_istream;
      pb_istream.callback = &ReadFromFileToStream;
      pb_istream.state = reinterpret_cast<void *>(file);
      pb_istream.bytes_left = std::numeric_limits<size_t>::max();
      return pb_istream;
    }
}  // namespace exposure
