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
FROM golang

# Library versions
ARG BORINGSSL_COMMIT_ID
ENV ANDROID_NDK_VERSION r21d

# Install dependencies
RUN apt-get update && apt-get install --no-install-recommends -yq \
    ca-certificates cmake git unzip ninja-build && \
    rm -rf /var/lib/apt/lists/*

# Download the Android NDK
ENV ANDROID_NDK_ROOT /go/android-ndk-${ANDROID_NDK_VERSION}
RUN wget -qq https://dl.google.com/android/repository/android-ndk-${ANDROID_NDK_VERSION}-linux-x86_64.zip -O /go/android-ndk.zip && \
    unzip -qq /go/android-ndk.zip -d /go && \
    rm /go/android-ndk.zip

# Clone the BoringSSL source
ENV BORINGSSL_SRC /src/boringssl
RUN git clone https://boringssl.googlesource.com/boringssl ${BORINGSSL_SRC}

# Check out the appropriate commit for reproducibility
WORKDIR ${BORINGSSL_SRC}
RUN git checkout ${BORINGSSL_COMMIT_ID}

# Image arguments used to determine the target Android ABI
ARG ANDROID_ABI
RUN echo "Building BoringSSL for Android ${ANDROID_ABI}"

# Cross compile the Android binaries
WORKDIR /opt/boringssl/android/${ANDROID_ABI}
RUN cmake \
        -DANDROID_ABI=${ANDROID_ABI} \
        -DCMAKE_TOOLCHAIN_FILE=${ANDROID_NDK_ROOT}/build/cmake/android.toolchain.cmake \
        -DANDROID_NATIVE_API_LEVEL=16 \
        -GNinja ${BORINGSSL_SRC} && \
    ninja all

