//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common.networking

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.DateProvider
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date
import java.util.concurrent.atomic.AtomicLong

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class HTTPTimeoutManagerTest {

    private class FakeDateProvider(private val currentTime: AtomicLong = AtomicLong(System.currentTimeMillis())) : DateProvider {
        override val now: Date
            get() = Date(currentTime.get())

        public fun advanceTime(millis: Long) {
            currentTime.addAndGet(millis)
        }
    }

    private lateinit var appConfig: AppConfig
    private lateinit var dateProvider: FakeDateProvider
    private lateinit var timeoutManager: HTTPTimeoutManager

    @Before
    public fun setUp() {
        appConfig = mockk<AppConfig>().apply {
            every { runningTests } returns false
        }
        dateProvider = FakeDateProvider()
        timeoutManager = HTTPTimeoutManager(appConfig, dateProvider)
    }

    @Test
    fun `getTimeoutForRequest returns SUPPORTED_FALLBACK_TIMEOUT_MS for endpoints with fallback support when no timeout occurred`() {
        val endpoint = Endpoint.GetOfferings("test_user_id")
        assert(endpoint.supportsFallbackBaseURLs)

        val timeout = timeoutManager.getTimeoutForRequest(endpoint, isFallback = false)
        assertThat(timeout).isEqualTo(HTTPTimeoutManager.SUPPORTED_FALLBACK_TIMEOUT_MS)
    }

    @Test
    fun `getTimeoutForRequest returns REDUCED_TIMEOUT_MS for endpoints with fallback support after timeout occurred`() {
        val endpoint = Endpoint.GetOfferings("test_user_id")

        // Record timeout
        timeoutManager.recordRequestResult(
            HTTPTimeoutManager.RequestResult.TIMEOUT_ON_MAIN_BACKEND_FOR_FALLBACK_SUPPORTED_ENDPOINT
        )

        val timeout = timeoutManager.getTimeoutForRequest(endpoint, isFallback = false)
        assertThat(timeout).isEqualTo(HTTPTimeoutManager.REDUCED_TIMEOUT_MS)
    }

    @Test
    fun `getTimeoutForRequest returns DEFAULT_TIMEOUT_MS for fallback requests`() {
        val endpoint = Endpoint.GetOfferings("test_user_id")

        val timeout = timeoutManager.getTimeoutForRequest(endpoint, isFallback = true)
        assertThat(timeout).isEqualTo(HTTPTimeoutManager.DEFAULT_TIMEOUT_MS)
    }

    @Test
    fun `getTimeoutForRequest returns DEFAULT_TIMEOUT_MS for endpoints without fallback support`() {
        val endpoint = Endpoint.LogIn
        assert(!endpoint.supportsFallbackBaseURLs)

        val timeout = timeoutManager.getTimeoutForRequest(endpoint, isFallback = false)
        assertThat(timeout).isEqualTo(HTTPTimeoutManager.DEFAULT_TIMEOUT_MS)
    }

    @Test
    fun `recordRequestResult SUCCESS_ON_MAIN_BACKEND resets timeout state`() {
        val endpoint = Endpoint.GetOfferings("test_user_id")

        // Record timeout first
        timeoutManager.recordRequestResult(
            HTTPTimeoutManager.RequestResult.TIMEOUT_ON_MAIN_BACKEND_FOR_FALLBACK_SUPPORTED_ENDPOINT
        )
        assertThat(timeoutManager.getTimeoutForRequest(endpoint, isFallback = false))
            .isEqualTo(HTTPTimeoutManager.REDUCED_TIMEOUT_MS)

        // Record success - should reset timeout state
        timeoutManager.recordRequestResult(HTTPTimeoutManager.RequestResult.SUCCESS_ON_MAIN_BACKEND)
        assertThat(timeoutManager.getTimeoutForRequest(endpoint, isFallback = false))
            .isEqualTo(HTTPTimeoutManager.SUPPORTED_FALLBACK_TIMEOUT_MS)
    }

    @Test
    fun `recordRequestResult TIMEOUT_ON_MAIN_BACKEND_FOR_FALLBACK_SUPPORTED_ENDPOINT sets timeout state`() {
        val endpoint = Endpoint.GetOfferings("test_user_id")

        // Initially no timeout
        assertThat(timeoutManager.getTimeoutForRequest(endpoint, isFallback = false))
            .isEqualTo(HTTPTimeoutManager.SUPPORTED_FALLBACK_TIMEOUT_MS)

        // Record timeout
        timeoutManager.recordRequestResult(
            HTTPTimeoutManager.RequestResult.TIMEOUT_ON_MAIN_BACKEND_FOR_FALLBACK_SUPPORTED_ENDPOINT
        )
        assertThat(timeoutManager.getTimeoutForRequest(endpoint, isFallback = false))
            .isEqualTo(HTTPTimeoutManager.REDUCED_TIMEOUT_MS)
    }

    @Test
    fun `recordRequestResult OTHER_RESULT does not change timeout state`() {
        val endpoint = Endpoint.GetOfferings("test_user_id")

        // Record timeout first
        timeoutManager.recordRequestResult(
            HTTPTimeoutManager.RequestResult.TIMEOUT_ON_MAIN_BACKEND_FOR_FALLBACK_SUPPORTED_ENDPOINT
        )
        assertThat(timeoutManager.getTimeoutForRequest(endpoint, isFallback = false))
            .isEqualTo(HTTPTimeoutManager.REDUCED_TIMEOUT_MS)

        // Record OTHER_RESULT - should not change state
        timeoutManager.recordRequestResult(HTTPTimeoutManager.RequestResult.OTHER_RESULT)
        assertThat(timeoutManager.getTimeoutForRequest(endpoint, isFallback = false))
            .isEqualTo(HTTPTimeoutManager.REDUCED_TIMEOUT_MS)
    }

    @Test
    fun `timeout resets after 10 minutes`() {
        val endpoint = Endpoint.GetOfferings("test_user_id")

        // Record timeout
        timeoutManager.recordRequestResult(
            HTTPTimeoutManager.RequestResult.TIMEOUT_ON_MAIN_BACKEND_FOR_FALLBACK_SUPPORTED_ENDPOINT
        )
        assertThat(timeoutManager.getTimeoutForRequest(endpoint, isFallback = false))
            .isEqualTo(HTTPTimeoutManager.REDUCED_TIMEOUT_MS)

        // Advance time by 10 minutes + 1 second
        dateProvider.advanceTime(HTTPTimeoutManager.TIMEOUT_RESET_INTERVAL_MS + 1000)

        // getTimeoutForRequest should reset timeout automatically
        assertThat(timeoutManager.getTimeoutForRequest(endpoint, isFallback = false))
            .isEqualTo(HTTPTimeoutManager.SUPPORTED_FALLBACK_TIMEOUT_MS)
    }

    @Test
    fun `timeout does not reset before 10 minutes`() {
        val endpoint = Endpoint.GetOfferings("test_user_id")

        // Record timeout
        timeoutManager.recordRequestResult(
            HTTPTimeoutManager.RequestResult.TIMEOUT_ON_MAIN_BACKEND_FOR_FALLBACK_SUPPORTED_ENDPOINT
        )
        assertThat(timeoutManager.getTimeoutForRequest(endpoint, isFallback = false))
            .isEqualTo(HTTPTimeoutManager.REDUCED_TIMEOUT_MS)

        // Advance time by 9 minutes (less than 10 minutes)
        dateProvider.advanceTime(HTTPTimeoutManager.TIMEOUT_RESET_INTERVAL_MS - 60000)

        // Timeout should still be reduced
        assertThat(timeoutManager.getTimeoutForRequest(endpoint, isFallback = false))
            .isEqualTo(HTTPTimeoutManager.REDUCED_TIMEOUT_MS)
    }

    @Test
    fun `timeout does not reset if no timeout has occurred`() {
        val endpoint = Endpoint.GetOfferings("test_user_id")

        // No timeout recorded
        assertThat(timeoutManager.getTimeoutForRequest(endpoint, isFallback = false))
            .isEqualTo(HTTPTimeoutManager.SUPPORTED_FALLBACK_TIMEOUT_MS)

        // Advance time by 10 minutes
        dateProvider.advanceTime(HTTPTimeoutManager.TIMEOUT_RESET_INTERVAL_MS + 1000)

        // Should still be default since no timeout occurred
        assertThat(timeoutManager.getTimeoutForRequest(endpoint, isFallback = false))
            .isEqualTo(HTTPTimeoutManager.SUPPORTED_FALLBACK_TIMEOUT_MS)
    }

    @Test
    fun `multiple timeouts update timeout state correctly`() {
        val endpoint = Endpoint.GetOfferings("test_user_id")

        // First timeout
        timeoutManager.recordRequestResult(
            HTTPTimeoutManager.RequestResult.TIMEOUT_ON_MAIN_BACKEND_FOR_FALLBACK_SUPPORTED_ENDPOINT
        )
        assertThat(timeoutManager.getTimeoutForRequest(endpoint, isFallback = false))
            .isEqualTo(HTTPTimeoutManager.REDUCED_TIMEOUT_MS)

        // Advance time by 5 minutes
        dateProvider.advanceTime(300000)

        // Second timeout - should update timestamp
        timeoutManager.recordRequestResult(
            HTTPTimeoutManager.RequestResult.TIMEOUT_ON_MAIN_BACKEND_FOR_FALLBACK_SUPPORTED_ENDPOINT
        )
        assertThat(timeoutManager.getTimeoutForRequest(endpoint, isFallback = false))
            .isEqualTo(HTTPTimeoutManager.REDUCED_TIMEOUT_MS)

        // Advance time by 5 minutes more (10 minutes total from first timeout, but only 5 from second)
        dateProvider.advanceTime(300000)

        // Should still be reduced because last timeout was only 5 minutes ago
        assertThat(timeoutManager.getTimeoutForRequest(endpoint, isFallback = false))
            .isEqualTo(HTTPTimeoutManager.REDUCED_TIMEOUT_MS)
    }

    @Test
    fun `SUCCESS_ON_MAIN_BACKEND resets timeout even if timeout occurred recently`() {
        val endpoint = Endpoint.GetOfferings("test_user_id")

        // Record timeout
        timeoutManager.recordRequestResult(
            HTTPTimeoutManager.RequestResult.TIMEOUT_ON_MAIN_BACKEND_FOR_FALLBACK_SUPPORTED_ENDPOINT
        )
        assertThat(timeoutManager.getTimeoutForRequest(endpoint, isFallback = false))
            .isEqualTo(HTTPTimeoutManager.REDUCED_TIMEOUT_MS)

        // Advance time by only 1 minute
        dateProvider.advanceTime(60000)

        // Record success - should reset immediately regardless of time
        timeoutManager.recordRequestResult(HTTPTimeoutManager.RequestResult.SUCCESS_ON_MAIN_BACKEND)
        assertThat(timeoutManager.getTimeoutForRequest(endpoint, isFallback = false))
            .isEqualTo(HTTPTimeoutManager.SUPPORTED_FALLBACK_TIMEOUT_MS)
    }

    @Test
    fun `timeout state persists across multiple getTimeoutForRequest calls`() {
        val endpoint = Endpoint.GetOfferings("test_user_id")

        // Record timeout
        timeoutManager.recordRequestResult(
            HTTPTimeoutManager.RequestResult.TIMEOUT_ON_MAIN_BACKEND_FOR_FALLBACK_SUPPORTED_ENDPOINT
        )

        // Multiple calls should all return reduced timeout
        assertThat(timeoutManager.getTimeoutForRequest(endpoint, isFallback = false))
            .isEqualTo(HTTPTimeoutManager.REDUCED_TIMEOUT_MS)
        assertThat(timeoutManager.getTimeoutForRequest(endpoint, isFallback = false))
            .isEqualTo(HTTPTimeoutManager.REDUCED_TIMEOUT_MS)
        assertThat(timeoutManager.getTimeoutForRequest(endpoint, isFallback = false))
            .isEqualTo(HTTPTimeoutManager.REDUCED_TIMEOUT_MS)
    }

    @Test
    fun `fallback requests always use DEFAULT_TIMEOUT_MS regardless of timeout state`() {
        val endpoint = Endpoint.GetOfferings("test_user_id")

        // Record timeout
        timeoutManager.recordRequestResult(
            HTTPTimeoutManager.RequestResult.TIMEOUT_ON_MAIN_BACKEND_FOR_FALLBACK_SUPPORTED_ENDPOINT
        )

        // Fallback requests should always use DEFAULT_TIMEOUT_MS
        assertThat(timeoutManager.getTimeoutForRequest(endpoint, isFallback = true))
            .isEqualTo(HTTPTimeoutManager.DEFAULT_TIMEOUT_MS)

        // Even after success, fallback should still use DEFAULT_TIMEOUT_MS
        timeoutManager.recordRequestResult(HTTPTimeoutManager.RequestResult.SUCCESS_ON_MAIN_BACKEND)
        assertThat(timeoutManager.getTimeoutForRequest(endpoint, isFallback = true))
            .isEqualTo(HTTPTimeoutManager.DEFAULT_TIMEOUT_MS)
    }

    @Test
    fun `endpoints without fallback support always use DEFAULT_TIMEOUT_MS`() {
        val endpoint = Endpoint.LogIn
        assert(!endpoint.supportsFallbackBaseURLs)

        // Initially
        assertThat(timeoutManager.getTimeoutForRequest(endpoint, isFallback = false))
            .isEqualTo(HTTPTimeoutManager.DEFAULT_TIMEOUT_MS)

        // Even after recording timeout
        timeoutManager.recordRequestResult(
            HTTPTimeoutManager.RequestResult.TIMEOUT_ON_MAIN_BACKEND_FOR_FALLBACK_SUPPORTED_ENDPOINT
        )
        assertThat(timeoutManager.getTimeoutForRequest(endpoint, isFallback = false))
            .isEqualTo(HTTPTimeoutManager.DEFAULT_TIMEOUT_MS)

        // After success
        timeoutManager.recordRequestResult(HTTPTimeoutManager.RequestResult.SUCCESS_ON_MAIN_BACKEND)
        assertThat(timeoutManager.getTimeoutForRequest(endpoint, isFallback = false))
            .isEqualTo(HTTPTimeoutManager.DEFAULT_TIMEOUT_MS)
    }
}
