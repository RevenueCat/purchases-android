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
        val result = HTTPResult(
            200,
            "{\"test-key\":\"test-value\"}",
            HTTPResult.Origin.BACKEND,
            HTTPResult.VerificationStatus.SUCCESS
        )
        assertThat(result.serialize()).isEqualTo("{" +
            "\"responseCode\":200," +
            "\"payload\":\"{\\\"test-key\\\":\\\"test-value\\\"}\"," +
            "\"origin\":\"BACKEND\"," +
            "\"verificationStatus\":\"SUCCESS\"}"
        )
    }

    @Test
    fun `result is deserialized correctly`() {
        val expectedResult = HTTPResult(
            200,
            "{\"test-key\":\"test-value\"}",
            HTTPResult.Origin.BACKEND,
            HTTPResult.VerificationStatus.ERROR
        )
        val result = HTTPResult.deserialize("{" +
            "\"responseCode\":200," +
            "\"payload\":\"{\\\"test-key\\\":\\\"test-value\\\"}\"," +
            "\"origin\":\"BACKEND\"," +
            "\"verificationStatus\":\"ERROR\"}")
        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `result defaults to CACHE origin and NOT_VERIFIED verification status if not part of serialized string`() {
        val expectedResult = HTTPResult(
            200,
            "{\"test-key\":\"test-value\"}",
            HTTPResult.Origin.CACHE,
            HTTPResult.VerificationStatus.NOT_VERIFIED
        )
        val result = HTTPResult.deserialize("{" +
            "\"responseCode\":200," +
            "\"payload\":\"{\\\"test-key\\\":\\\"test-value\\\"}\"}")
        assertThat(result).isEqualTo(expectedResult)
    }
}
