package com.revenuecat.purchases.backend_integration_tests

import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class GoldenFileRecorderTest {

    private val testFolder = File("temp_golden_file_recorder_test_folder")

    @Before
    fun setUp() {
        testFolder.deleteRecursively()
        testFolder.mkdirs()
    }

    @After
    fun tearDown() {
        testFolder.deleteRecursively()
    }

    @Test
    fun `masks manifest in config request bodies`() {
        val recorder = GoldenFileRecorder(
            className = "GoldenFileRecorderTest",
            testName = "masks manifest in config request bodies",
            baseDirectory = testFolder,
        )

        recorder.onRequestResponse(
            url = "https://api.revenuecat.com/v1/config/app",
            method = "POST",
            requestHeaders = emptyMap(),
            requestBody = """{"app_user_id":"user","manifest":"opaque-manifest"}""",
            responseCode = 200,
            responseHeaders = emptyMap(),
            responseBody = """{"manifest":"response-manifest"}""",
        )

        val request = readRecordedJSON("masks manifest in config request bodies", "request_001.json")
        assertThat(request.getJSONObject("body").getString("manifest")).isEqualTo("TEST_STATIC_VALUE")
        assertThat(request.getJSONObject("body").getString("app_user_id")).isEqualTo("user")

        val response = readRecordedJSON("masks manifest in config request bodies", "response_001.json")
        assertThat(response.getJSONObject("body").getString("manifest")).isEqualTo("response-manifest")
    }

    @Test
    fun `does not mask manifest in non config request bodies`() {
        val recorder = GoldenFileRecorder(
            className = "GoldenFileRecorderTest",
            testName = "does not mask manifest in non config request bodies",
            baseDirectory = testFolder,
        )

        recorder.onRequestResponse(
            url = "https://api.revenuecat.com/v1/subscribers/user/offerings",
            method = "POST",
            requestHeaders = emptyMap(),
            requestBody = """{"manifest":"meaningful-manifest"}""",
            responseCode = 200,
            responseHeaders = emptyMap(),
            responseBody = "{}",
        )

        val request = readRecordedJSON("does not mask manifest in non config request bodies", "request_001.json")
        assertThat(request.getJSONObject("body").getString("manifest")).isEqualTo("meaningful-manifest")
    }

    private fun readRecordedJSON(testName: String, fileName: String): JSONObject {
        val file = File(File(File(testFolder, "GoldenFileRecorderTest"), testName), fileName)
        return JSONObject(file.readText())
    }
}
