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

package com.google.samples.ensnippets

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.samples.exposurenotification.matching.ProvideDiagnosisKeys
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/** Sample app package name */
private const val SAMPLE_PACKAGE = "com.google.android.apps.exposurenotification"

@RunWith(AndroidJUnit4::class)
class SignatureVerificationTests {
    private lateinit var diagnosisKeyFile: File

    @Before
    fun setup() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        appContext.resources.openRawResource(R.raw.sample_diagnosis_key_file).use { inputStream ->
            diagnosisKeyFile = File(appContext.filesDir, "export.zip")
            FileOutputStream(diagnosisKeyFile).use { fileOutputStream ->
                inputStream.copyTo(fileOutputStream)
            }
        }

        val targetFolder = File(appContext.filesDir, "en_diagnosis_keys")
        targetFolder.deleteRecursively()
    }

    @After
    fun teardown() {
        if (diagnosisKeyFile.exists()) diagnosisKeyFile.delete()
    }

    @Test
    fun testVerifyDiagnosisKeysZipFile() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val provideDiagnosisKeys = ProvideDiagnosisKeys(appContext)

        assertTrue(provideDiagnosisKeys.verify(SAMPLE_PACKAGE, diagnosisKeyFile))
    }
}
