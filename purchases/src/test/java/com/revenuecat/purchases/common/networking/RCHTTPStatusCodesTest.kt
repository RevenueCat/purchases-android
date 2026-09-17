package com.revenuecat.purchases.common.networking

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.HTTPClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class RCHTTPStatusCodesTest {

    @Test
    fun `isSuccessful is true for 2xx and 3xx codes`() {
        listOf(
            RCHTTPStatusCodes.SUCCESS,
            RCHTTPStatusCodes.CREATED,
            RCHTTPStatusCodes.NO_CONTENT,
            RCHTTPStatusCodes.UNSUCCESSFUL,
            RCHTTPStatusCodes.NOT_MODIFIED,
            399,
        ).forEach { code ->
            assertThat(RCHTTPStatusCodes.isSuccessful(code)).describedAs("code $code").isTrue
        }
    }

    @Test
    fun `isSuccessful is false for 4xx and 5xx codes`() {
        listOf(
            RCHTTPStatusCodes.BAD_REQUEST,
            RCHTTPStatusCodes.UNAUTHORIZED,
            RCHTTPStatusCodes.FORBIDDEN,
            RCHTTPStatusCodes.NOT_FOUND,
            RCHTTPStatusCodes.ERROR,
        ).forEach { code ->
            assertThat(RCHTTPStatusCodes.isSuccessful(code)).describedAs("code $code").isFalse
        }
    }

    @Test
    fun `isSuccessful is false below 200`() {
        listOf(HTTPClient.NO_STATUS_CODE, 0, 100, 199).forEach { code ->
            assertThat(RCHTTPStatusCodes.isSuccessful(code)).describedAs("code $code").isFalse
        }
    }

    @Test
    fun `isServerError is true only for 5xx codes`() {
        assertThat(RCHTTPStatusCodes.isServerError(RCHTTPStatusCodes.ERROR)).isTrue
        assertThat(RCHTTPStatusCodes.isServerError(503)).isTrue
        assertThat(RCHTTPStatusCodes.isServerError(499)).isFalse
        assertThat(RCHTTPStatusCodes.isServerError(RCHTTPStatusCodes.SUCCESS)).isFalse
    }

    @Test
    fun `isSynced is true for successful codes and client errors other than 404`() {
        listOf(
            RCHTTPStatusCodes.SUCCESS,
            RCHTTPStatusCodes.NOT_MODIFIED,
            RCHTTPStatusCodes.BAD_REQUEST,
            RCHTTPStatusCodes.UNAUTHORIZED,
            RCHTTPStatusCodes.FORBIDDEN,
        ).forEach { code ->
            assertThat(RCHTTPStatusCodes.isSynced(code)).describedAs("code $code").isTrue
        }
    }

    @Test
    fun `isSynced is false for 404 and server errors`() {
        listOf(RCHTTPStatusCodes.NOT_FOUND, RCHTTPStatusCodes.ERROR, 503).forEach { code ->
            assertThat(RCHTTPStatusCodes.isSynced(code)).describedAs("code $code").isFalse
        }
    }
}
