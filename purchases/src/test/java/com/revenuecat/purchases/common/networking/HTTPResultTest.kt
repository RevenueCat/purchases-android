package com.revenuecat.purchases.common.networking

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.VerificationResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
public class HTTPResultTest {

    @Test
    fun `result is serialized correctly`() {
        val result = HTTPResult(
            200,
            "{\"test-key\":\"test-value\"}",
            HTTPResult.Origin.BACKEND,
            Date(1678180617000), // Tuesday, March 7, 2023 9:16:57 AM GMT,
            VerificationResult.VERIFIED,
            isLoadShedderResponse = false,
            isFallbackURL = false,
        )
        assertThat(result.serialize()).isEqualTo("{" +
            "\"responseCode\":200," +
            "\"payload\":\"{\\\"test-key\\\":\\\"test-value\\\"}\"," +
            "\"origin\":\"BACKEND\"," +
            "\"requestDate\":1678180617000," +
            "\"verificationResult\":\"VERIFIED\"," +
            "\"isLoadShedderResponse\":false," +
            "\"isFallbackURL\":false}"
        )
    }

    @Test
    fun `result with null requestDate is serialized correctly`() {
        val result = HTTPResult(
            200,
            "{\"test-key\":\"test-value\"}",
            HTTPResult.Origin.BACKEND,
            null,
            VerificationResult.VERIFIED,
            isLoadShedderResponse = false,
            isFallbackURL = false,
        )
        assertThat(result.serialize()).isEqualTo("{" +
            "\"responseCode\":200," +
            "\"payload\":\"{\\\"test-key\\\":\\\"test-value\\\"}\"," +
            "\"origin\":\"BACKEND\"," +
            "\"verificationResult\":\"VERIFIED\"," +
            "\"isLoadShedderResponse\":false," +
            "\"isFallbackURL\":false}"
        )
    }

    @Test
    fun `result is deserialized correctly`() {
        val expectedResult = HTTPResult(
            200,
            "{\"test-key\":\"test-value\"}",
            HTTPResult.Origin.BACKEND,
            Date(1678180617000),
            VerificationResult.FAILED,
            isLoadShedderResponse = false,
            isFallbackURL = false,
        )
        val result = HTTPResult.deserialize("{" +
            "\"responseCode\":200," +
            "\"payload\":\"{\\\"test-key\\\":\\\"test-value\\\"}\"," +
            "\"origin\":\"BACKEND\"," +
            "\"requestDate\":1678180617000," +
            "\"verificationResult\":\"FAILED\"}")
        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `result defaults when deserializing are expected if data not part of serialized string`() {
        val expectedResult = HTTPResult(
            200,
            "{\"test-key\":\"test-value\"}",
            HTTPResult.Origin.CACHE,
            null,
            VerificationResult.NOT_REQUESTED,
            isLoadShedderResponse = false,
            isFallbackURL = false,
        )
        val result = HTTPResult.deserialize("{" +
            "\"responseCode\":200," +
            "\"payload\":\"{\\\"test-key\\\":\\\"test-value\\\"}\"}")
        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `result with load shedder header is serialized correctly`() {
        val result = HTTPResult(
            200,
            "{\"test-key\":\"test-value\"}",
            HTTPResult.Origin.BACKEND,
            Date(1678180617000),
            VerificationResult.VERIFIED,
            isLoadShedderResponse = true,
            isFallbackURL = false,
        )
        val serialized = result.serialize()
        assertThat(serialized).contains("\"isLoadShedderResponse\":true")
        assertThat(serialized).contains("\"responseCode\":200")
    }

    @Test
    fun `result with fallback url is serialized correctly`() {
        val result = HTTPResult(
            200,
            "{\"test-key\":\"test-value\"}",
            HTTPResult.Origin.BACKEND,
            Date(1678180617000),
            VerificationResult.VERIFIED,
            isLoadShedderResponse = false,
            isFallbackURL = true,
        )
        val serialized = result.serialize()
        assertThat(serialized).contains("\"isFallbackURL\":true")
        assertThat(serialized).contains("\"responseCode\":200")
    }

    @Test
    fun `result with load shedder header is deserialized correctly`() {
        val expectedResult = HTTPResult(
            200,
            "{\"test-key\":\"test-value\"}",
            HTTPResult.Origin.BACKEND,
            Date(1678180617000),
            VerificationResult.VERIFIED,
            isLoadShedderResponse = true,
            isFallbackURL = false
        )
        val result = HTTPResult.deserialize("{" +
            "\"responseCode\":200," +
            "\"payload\":\"{\\\"test-key\\\":\\\"test-value\\\"}\"," +
            "\"origin\":\"BACKEND\"," +
            "\"requestDate\":1678180617000," +
            "\"verificationResult\":\"VERIFIED\"," +
            "\"isLoadShedderResponse\":true}")
        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `result with fallback URL is serialized correctly`() {
        val result = HTTPResult(
            200,
            "{\"test-key\":\"test-value\"}",
            HTTPResult.Origin.BACKEND,
            Date(1678180617000),
            VerificationResult.VERIFIED,
            isLoadShedderResponse = false,
            isFallbackURL = true,
        )
        val serialized = result.serialize()
        assertThat(serialized).contains("\"isFallbackURL\":true")
        assertThat(serialized).contains("\"responseCode\":200")
    }

    @Test
    fun `result with fallback URL is deserialized correctly`() {
        val expectedResult = HTTPResult(
            200,
            "{\"test-key\":\"test-value\"}",
            HTTPResult.Origin.BACKEND,
            Date(1678180617000),
            VerificationResult.VERIFIED,
            isLoadShedderResponse = false,
            isFallbackURL = true,
        )
        val result = HTTPResult.deserialize("{" +
            "\"responseCode\":200," +
            "\"payload\":\"{\\\"test-key\\\":\\\"test-value\\\"}\"," +
            "\"origin\":\"BACKEND\"," +
            "\"requestDate\":1678180617000," +
            "\"verificationResult\":\"VERIFIED\"," +
            "\"isLoadShedderResponse\":false," +
            "\"isFallbackURL\":true}")
        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `result with both load shedder and fallback flags serialized correctly`() {
        val result = HTTPResult(
            200,
            "{\"test-key\":\"test-value\"}",
            HTTPResult.Origin.BACKEND,
            Date(1678180617000),
            VerificationResult.VERIFIED,
            isLoadShedderResponse = true,
            isFallbackURL = true,
        )
        val serialized = result.serialize()
        assertThat(serialized).contains("\"isLoadShedderResponse\":true")
        assertThat(serialized).contains("\"isFallbackURL\":true")
    }
}
