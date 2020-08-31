#!/bin/sh
# Copyright 2020 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#######################################
# Download and extract the nanopb precompiled binaries for the given platform
# Arguments:
#   dest: destination folder for the binaries
#   platform: linux, macosx or windows
#######################################
download_nanopb() {

  # Process arguments
  local -r dest=$1
  local -r platform=$2

  # Download appropriate binaries
  local -r output="${dest}/${platform}"
  local -r tar_file="${dest}/${platform}.tar.gz"
  wget -c "https://jpa.kapsi.fi/nanopb/download/nanopb-0.4.2-${platform}-x86.tar.gz" -O "$tar_file"
  mkdir -p "$output" && tar xvf "$tar_file" -C "$output" --strip-components 1 && rm -rf "$tar_file"
}

readonly base_path=`dirname "$0"`
download_nanopb "$base_path" linux
download_nanopb "$base_path" macosx

