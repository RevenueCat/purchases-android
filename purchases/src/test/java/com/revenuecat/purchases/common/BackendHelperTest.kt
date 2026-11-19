package com.revenuecat.purchases.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import com.revenuecat.purchases.interfaces.StorefrontProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class BackendHelperTest {

    @Test
    fun `HTTPResult isSuccessful is true only for 2xx codes`() {
        listOf(RCHTTPStatusCodes.SUCCESS, RCHTTPStatusCodes.CREATED, RCHTTPStatusCodes.NO_CONTENT, 299).forEach { code ->
            assertThat(HTTPResult.createResult(responseCode = code).isSuccessful()).describedAs("code $code").isTrue
        }
    }

    @Test
    fun `HTTPResult isSuccessful is false for 3xx and error codes`() {
        listOf(
            RCHTTPStatusCodes.UNSUCCESSFUL,
            RCHTTPStatusCodes.NOT_MODIFIED,
            RCHTTPStatusCodes.BAD_REQUEST,
            RCHTTPStatusCodes.NOT_FOUND,
            RCHTTPStatusCodes.ERROR,
        ).forEach { code ->
            assertThat(HTTPResult.createResult(responseCode = code).isSuccessful()).describedAs("code $code").isFalse
        }
    }

    @Test
    fun `enqueue holds the request until the storefront awaiter releases it`() {
        val dispatcher = mockk<Dispatcher>(relaxed = true).also { every { it.isClosed() } returns false }
        val awaiter = StorefrontAwaiter(
            storefrontProvider = mockk<StorefrontProvider>().also { every { it.getStorefront() } returns null },
            scheduleTimeout = { _, _ -> },
        )
        val helper = BackendHelper("api_key", dispatcher, mockk(), mockk(), awaiter)
        val call = mockk<Dispatcher.AsyncCall>()

        helper.enqueue(call, dispatcher, Delay.DEFAULT)
        verify(exactly = 0) { dispatcher.enqueue(any(), any()) }

        awaiter.release()
        verify(exactly = 1) { dispatcher.enqueue(call, Delay.DEFAULT) }
    }

    @Test
    fun `enqueue sends the request immediately without a storefront awaiter`() {
        val dispatcher = mockk<Dispatcher>(relaxed = true).also { every { it.isClosed() } returns false }
        val helper = BackendHelper("api_key", dispatcher, mockk(), mockk(), storefrontAwaiter = null)
        val call = mockk<Dispatcher.AsyncCall>()

        helper.enqueue(call, dispatcher)

        verify(exactly = 1) { dispatcher.enqueue(call, Delay.NONE) }
    }

    @Test
    fun `HTTPResult isSuccessful is false below 200`() {
        listOf(HTTPClient.NO_STATUS_CODE, 0, 100, 199).forEach { code ->
            assertThat(HTTPResult.createResult(responseCode = code).isSuccessful()).describedAs("code $code").isFalse
        }
    }
}
