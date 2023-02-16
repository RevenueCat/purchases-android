package com.revenuecat.purchases.common.networking

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class HTTPResultTest {

    @Test
    fun `result is serialized correctly`() {
        val result = HTTPResult(200, "{\"test-key\":\"test-value\"}", ResultOrigin.BACKEND)
        assertThat(result.serialize()).isEqualTo("{" +
            "\"responseCode\":200," +
            "\"payload\":\"{\\\"test-key\\\":\\\"test-value\\\"}\"," +
            "\"origin\":\"BACKEND\"}"
        )
    }

    @Test
    fun `result is deserialized correctly`() {
        val expectedResult = HTTPResult(200, "{\"test-key\":\"test-value\"}", ResultOrigin.BACKEND)
        val result = HTTPResult.deserialize("{" +
            "\"responseCode\":200," +
            "\"payload\":\"{\\\"test-key\\\":\\\"test-value\\\"}\"," +
            "\"origin\":\"BACKEND\"}")
        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `result is defaults to CACHE origin if not part of serialized string`() {
        val expectedResult = HTTPResult(200, "{\"test-key\":\"test-value\"}", ResultOrigin.CACHE)
        val result = HTTPResult.deserialize("{" +
            "\"responseCode\":200," +
            "\"payload\":\"{\\\"test-key\\\":\\\"test-value\\\"}\"}")
        assertThat(result).isEqualTo(expectedResult)
    }
}
