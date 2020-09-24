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

import androidx.core.util.Supplier
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.samples.exposurenotification.nearby.TemporaryExposureKey
import com.google.samples.exposurenotification.ble.advertising.RollingProximityIdManager
import com.google.samples.exposurenotification.ble.interfaces.BleDatabaseWriter
import com.google.samples.exposurenotification.features.ContactTracingFeature
import com.google.samples.exposurenotification.matching.MatchingJni
import com.google.samples.exposurenotification.storage.SelfTemporaryExposureKeyDataStore
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.math.BigInteger
import java.util.concurrent.TimeUnit

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class RoundTripTests {
    private lateinit var exportKeyFile: File

    @Before
    fun setup() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        appContext.resources.openRawResource(R.raw.export).use { inputStream ->
            exportKeyFile = File(appContext.filesDir, "export.bin")
            FileOutputStream(exportKeyFile).use { fileOutputStream ->
                inputStream.copyTo(fileOutputStream)
            }
        }
    }

    @After
    fun teardown() {
        if (exportKeyFile.exists()) exportKeyFile.delete()
    }

    /**
     * This is a test that shows how to call the native matching code. This test assumes that the
     * ZIP file of diagnosis keys has been verified and the diagnosis keys have been extracted
     * to the file referenced by [exportKeyFile].
     */
    @Test
    fun testGenerateAndMatchRPI() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        // Required parameter, but is unused in this test.
        val testBleDatabaseWriter = TestBleDatabaseWriter()

        // In order to match a key provided in the file, we've hardcoded one of the provided
        // diagnosis keys here (0x2b714d3ec93c14e5d345003e59027aea).
        //
        // We inject this key into a SelfTemporaryExposureKeyDataStore and then use the code
        // that generates Rolling Proximity Ids (RPIs) to create an RPI from the diagnosis key
        // above.
        val testDataStore =
            TestSelfTemporaryExposureKeyDataStore("2b714d3ec93c14e5d345003e59027aea", 2646801)
        val rpiManager = RollingProximityIdManager(
            testDataStore,
            testBleDatabaseWriter,
            Supplier { rollingPeriodStartToMillis(2646801) }
        )

        // Here we use the RPI manager created above to get an RPI
        val seenRpi = rpiManager.currentRollingProximityId.get()

        // We take this key and say it's a key that we saw during scanning and provide that
        // to the matching code.
        val bleScanResults = arrayOf(seenRpi)
        val matchingJni = MatchingJni(appContext, bleScanResults)

        // We then call through and provide the diagnosis keys file we extracted
        val results = matchingJni.matching(listOf(exportKeyFile.toString()))

        // Because we generated an RPI from one of the diagnosis keys in the test data, we
        // expect that we have seen exactly 1 result (since we generated one RPI)
        Assert.assertEquals(results.size, 1)

        // We check the actual results and make sure that the key that's matched is the
        // one that was generated above.
        // (This also ensures that the native code which generates RPIs from a diagnosis key
        // creates the same keys as the Java code which generates them from a temporary
        // exposure key.)
        results.forEach { matchedKey ->
            Assert.assertArrayEquals(
                matchedKey.keyData,
                keyStringToByteArray("2b714d3ec93c14e5d345003e59027aea")
            )
        }
    }

    private fun rollingPeriodStartToMillis(rollingPeriod: Int) =
        rollingPeriod *
                TimeUnit.MINUTES.toMillis(ContactTracingFeature.idRollingPeriodMinutes().toLong())
}

// The rolling period _could_ be shorter if we wanted to expire it early (so that we could, for
// example, share a positive diagnosis ASAP. For testing we just set it to 144 (24 hours).
private const val TEST_ROLLING_PERIOD = 144

class TestSelfTemporaryExposureKeyDataStore(
    hexKey: String,
    rollingPeriodStart: Int
) : SelfTemporaryExposureKeyDataStore() {

    init {
        val testKey = TemporaryExposureKey.TemporaryExposureKeyBuilder()
            .setKeyData(keyStringToByteArray(hexKey))
            .setRollingStartIntervalNumber(rollingPeriodStart)
            .setRollingPeriod(TEST_ROLLING_PERIOD)
            .build()
        putKey(testKey)
    }
}

class TestBleDatabaseWriter : BleDatabaseWriter {
    override fun writeNewTemporaryExposureKey(temporaryExposureKey: TemporaryExposureKey?) {
        // Nothing to store for the test.
    }

    override fun writeBleSighting(
        rollingProximityId: ByteArray?,
        rssi: Int,
        associatedEncryptedMetadata: ByteArray?,
        previousScanEpochSeconds: Int
    ) {
        // Nothing needed for testing.
    }
}

/**
 * Helper method to take a hex string (key) and convert it to a byte array.
 */
private fun keyStringToByteArray(key: String): ByteArray = BigInteger(key, 16).toByteArray()