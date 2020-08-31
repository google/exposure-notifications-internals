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

readonly BORINGSSL_IMAGE="boringssl"
readonly BORINGSSL_COMMIT_ID="bc2480510960a77bea24edc64fcb089aca103940"

#######################################
# Cross-compiles BoringSSL for Android using the Docker image in this folder
# Globals:
#   BORINGSSL_IMAGE: Name of the docker image used to build BoringSSL
# Arguments:
#   android_abi: ABI flavour, e.g. armeabi-v7a.
#######################################
boringssl_android_build() {

  # Process arguments
  local -r android_abi=$1

  # Build the Docker image for the specific Android ABI
  local -r dockerfile_path=`dirname "$0"`
  docker build --rm \
    --build-arg ANDROID_ABI=$android_abi \
    --build-arg BORINGSSL_COMMIT_ID=$BORINGSSL_COMMIT_ID \
    --tag $BORINGSSL_IMAGE:$android_abi \
    $dockerfile_path
}

#######################################
# Copies the build artifacts for the given Android ABI flavour into the
# provided destination folder.
# Globals:
#   BORINGSSL_IMAGE: Name of the docker image used to build BoringSSL
# Arguments:
#   android_abi: ABI flavour, e.g. armeabi-v7a.
#   dest: Destination folder to place artifacts.
#######################################
copy_build_artifacts() {

  # Process arguments
  local -r android_abi=$1
  local -r dest=$2

  # Create a Docker image and tar the build artifacts into local storage
  local -r image_id=$(docker create ${BORINGSSL_IMAGE}:${android_abi})
  local -r output_tar="/tmp/boringssl-${android_abi}.tar"
  docker cp ${image_id}:"/opt/boringssl/android/${android_abi}" - > $output_tar
  docker rm -v $image_id

  # Extract the tar into the desired destination folder
  mkdir -p $dest
  tar -xvf $output_tar -C $dest
}

# Determine the location of the base directory
readonly BASE_DIR=$(dirname "$0")

# Download the source so we can reference the header files
git clone https://boringssl.googlesource.com/boringssl "$BASE_DIR"/src
git --git-dir "$BASE_DIR"/src/.git checkout ${BORINGSSL_COMMIT_ID}

# Compile each ABI and output artifacts into a subfolder
for flavour in "armeabi-v7a" "arm64-v8a" "x86" "x86_64"
do
    boringssl_android_build $flavour
    copy_build_artifacts $flavour "$BASE_DIR"
done
