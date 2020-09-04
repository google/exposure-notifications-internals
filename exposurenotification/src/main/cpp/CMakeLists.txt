#
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

# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# The Exposure Notification API depends on BoringSSL for cryptographic operations.
add_library(crypto STATIC IMPORTED)
add_library(ssl STATIC IMPORTED)

# Configure the location of BoringSSL's libraries
# TODO(BoringSSL): If you want to change where these libraries are built (specifically the naming of
#                  the build directory, this is the place to update them.
set_target_properties(crypto PROPERTIES IMPORTED_LOCATION ${BORING_SSL_ROOT}/build-${ANDROID_ABI}/crypto/libcrypto.a)
set_target_properties(ssl PROPERTIES IMPORTED_LOCATION ${BORING_SSL_ROOT}/build-${ANDROID_ABI}/ssl/libssl.a)
include_directories(matching ${BORING_SSL_ROOT}/include)

# Determine the correct binary by platform
if(WIN32)
    set(PROTOC "protoc.exe")
else()
    set(PROTOC "protoc")
endif()

# Generate the NanoPB C source and headers
execute_process(
        COMMAND mkdir -p ./gen
        WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
        )
execute_process(
        COMMAND ${NANOPB_ROOT}/generator-bin/${PROTOC} -I ${NANOPB_ROOT} -I ${NANOPB_ROOT}/generator/proto/ -I ../proto --nanopb_out=./gen ../proto/exposure_key_export.proto
        WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
        OUTPUT_VARIABLE PROTOC_OUTPUT
        RESULT_VARIABLE PROTOC_RESULT
)
# The generated code includes a header file, so add it to the include path
include_directories(matching gen)

# NanoPB has some include files we'll need.
include_directories(matching ${NANOPB_ROOT})

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

include_directories(matching ${CMAKE_LIBRARY_PATH}/protos)

add_library(matching SHARED

        # NanoPB code
        # We just compile it ourselves since it's very small and simple
        ${NANOPB_ROOT}/pb_encode.c
        ${NANOPB_ROOT}/pb_decode.c
        ${NANOPB_ROOT}/pb_common.c

        # Protobuf Generated Code
        gen/exposure_key_export.pb.c

        # Key matching source
        key_file_parser.cc
        matching_helper.cc
        matchingjni.cc
        nanopb_encoder.cc
        prefix_id_map.cc)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library(log-lib log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries(matching ${log-lib} crypto ssl)
