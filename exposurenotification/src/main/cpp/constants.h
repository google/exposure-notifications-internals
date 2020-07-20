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

#ifndef LOCATION_NEARBY_CPP_EXPOSURENOTIFICATION_JNI_CONSTANTS_H_
#define LOCATION_NEARBY_CPP_EXPOSURENOTIFICATION_JNI_CONSTANTS_H_

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifdef __ANDROID__

#include <android/log.h>

#define LOG_TAG "ExposureNotificationJni"
#define LOG_V(...) \
  __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#define LOG_I(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOG_W(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOG_E(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#else
#include <cstdio>
#define LOG_V(...) fprintf(stderr, __VA_ARGS__)
#define LOG_I(...) fprintf(stderr, __VA_ARGS__)
#define LOG_W(...) fprintf(stderr, __VA_ARGS__)
#define LOG_E(...) fprintf(stderr, __VA_ARGS__)
#endif
namespace exposure {
    constexpr static const int kRpikLength = 16;
    constexpr static const int kTekLength = 16;
    constexpr static const int kIdLength = 16;
    constexpr static const int kIdPerKey = 144;
    constexpr static const int kHkdfInfoLength = 7;
    constexpr static const char kHkdfInfo[] = u8"EN-RPIK";
    constexpr static const int kRpiPaddedDataLength = 12;
    constexpr static const char kRpiPaddedData[] = u8"EN-RPI\0\0\0\0\0\0";

}  // namespace exposure

#endif  // LOCATION_NEARBY_CPP_EXPOSURENOTIFICATION_JNI_CONSTANTS_H_
