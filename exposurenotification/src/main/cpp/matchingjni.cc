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

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <string>
#include <vector>

#include "constants.h"
#include "matching_helper.h"
#include "prefix_id_map.h"

extern "C" {

#define JND_PACKAGE(name) Java_com_google_samples_exposurenotification_matching_##name
#define JND(name) JND_PACKAGE(MatchingJni_##name)

JNIEXPORT jlong JNICALL JND(initNative)(JNIEnv *env, jclass clazz,
                                        jobjectArray scan_id_records) {
  if (scan_id_records == nullptr) {
    LOG_W("Invalid input for initNative, scan records is null");
    return 0;
  }

  if (env->GetArrayLength(scan_id_records) <= 0) {
    LOG_W("Invalid input for initNative, scan records is empty");
    return 0;
  }

  return reinterpret_cast<jlong>(
      new exposure::MatchingHelper(env, scan_id_records));
}

JNIEXPORT jobjectArray JNICALL
JND(matchingNative)(JNIEnv *env, jclass clazz, jlong native_ptr,
                    jobjectArray key_files_jstring) {
  if (native_ptr == 0 || key_files_jstring == nullptr) {
    LOG_W("Invalid input for matchingNative");
    return nullptr;
  }

  int key_file_count = env->GetArrayLength(key_files_jstring);
  LOG_I("matchingNative get %d files", key_file_count);

  std::vector<std::string> key_files;
  for (int i = 0; i < key_file_count; i++) {
    jstring key_file_jstring =
        (jstring) (env->GetObjectArrayElement(key_files_jstring, i));
    const char *key_file_string = env->GetStringUTFChars(key_file_jstring, 0);
    key_files.push_back(std::string(key_file_string));
    env->ReleaseStringUTFChars(key_file_jstring, key_file_string);
    env->DeleteLocalRef(key_file_jstring);
  }

  exposure::MatchingHelper *wrapper =
      reinterpret_cast<exposure::MatchingHelper *>(native_ptr);
  return wrapper->Matching(env, key_files);
}

JNIEXPORT jintArray JNICALL JND(matchingLegacyNative)(
    JNIEnv *env, jclass clazz, jlong native_ptr, jobjectArray diagnosis_keys,
    jintArray interval_numbers, jint key_count) {
  if (native_ptr == 0 || diagnosis_keys == nullptr ||
      interval_numbers == nullptr) {
    LOG_W("Invalid input for matchingNativeLegacy");
    return nullptr;
  }

  LOG_I("matchingNative get %d keys", key_count);

  exposure::MatchingHelper *wrapper =
      reinterpret_cast<exposure::MatchingHelper *>(native_ptr);
  return wrapper->MatchingLegacy(env, diagnosis_keys, interval_numbers,
                                 key_count);
}

JNIEXPORT jint JNICALL JND(lastProcessedKeyCountNative)(JNIEnv *env,
                                                        jclass clazz,
                                                        jlong native_ptr) {
  if (native_ptr == 0) {
    LOG_W("Invalid input for lastProcessedKeyCount");
    return -1;
  }

  exposure::MatchingHelper *wrapper =
      reinterpret_cast<exposure::MatchingHelper *>(native_ptr);
  return wrapper->LastProcessedKeyCount();
}

JNIEXPORT void JNICALL JND(releaseNative)(JNIEnv *env, jclass clazz,
                                          jlong native_ptr) {
  if (native_ptr == 0) {
    LOG_W("Invalid input for releaseNative");
    return;
  }
  exposure::MatchingHelper *wrapper =
      reinterpret_cast<exposure::MatchingHelper *>(native_ptr);
  delete wrapper;
}
} /* extern "C" */