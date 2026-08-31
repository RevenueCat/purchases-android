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

        fun advanceTime(millis: Long) {
            currentTime.addAndGet(millis)
        }
    }

    private companion object {
        const val HOST_A = "a.example.com"
        const val HOST_B = "b.example.com"
    }

    private lateinit var appConfig: AppConfig
    private lateinit var dateProvider: FakeDateProvider
    private lateinit var timeoutManager: HTTPTimeoutManager

    @Before
    fun setUp() {
        appConfig = mockk<AppConfig>().apply {
            every { runningTests } returns false
        }
        dateProvider = FakeDateProvider()
        timeoutManager = HTTPTimeoutManager(appConfig, dateProvider)
    }

    private fun timeout(
        host: String?,
        isFallback: Boolean = false,
        fallbackAvailable: Boolean = false,
        isProxied: Boolean = false,
        reTieredTimeoutsEnabled: Boolean = true,
    ): Long = timeoutManager.getTimeoutForRequest(
        host = host,
        isFallback = isFallback,
        fallbackAvailable = fallbackAvailable,
        isProxied = isProxied,
        reTieredTimeoutsEnabled = reTieredTimeoutsEnabled,
    )

    // region Base tiers

    @Test
    fun `main-source no-fallback endpoint uses base timeout`() {
        assertThat(timeout(host = HOST_A, fallbackAvailable = false))
            .isEqualTo(HTTPTimeoutManager.MAIN_SOURCE_NO_FALLBACK_TIMEOUT_MS)
    }

    @Test
    fun `main-source fallback-supporting endpoint uses base timeout`() {
        assertThat(timeout(host = HOST_A, fallbackAvailable = true))
            .isEqualTo(HTTPTimeoutManager.SUPPORTED_FALLBACK_TIMEOUT_MS)
    }

    // endregion

    // region Reduced tiers after a recent timeout

    @Test
    fun `main-source no-fallback endpoint uses reduced timeout after a recent timeout`() {
        timeoutManager.recordRequestResult(HOST_A, HTTPTimeoutManager.RequestResult.MAIN_SOURCE_TIMED_OUT)

        assertThat(timeout(host = HOST_A, fallbackAvailable = false))
            .isEqualTo(HTTPTimeoutManager.MAIN_SOURCE_NO_FALLBACK_REDUCED_TIMEOUT_MS)
    }

    @Test
    fun `main-source fallback-supporting endpoint uses reduced timeout after a recent timeout`() {
        timeoutManager.recordRequestResult(HOST_A, HTTPTimeoutManager.RequestResult.MAIN_SOURCE_TIMED_OUT)

        assertThat(timeout(host = HOST_A, fallbackAvailable = true))
            .isEqualTo(HTTPTimeoutManager.REDUCED_TIMEOUT_MS)
    }

    // endregion

    // region Flat tiers (fallback-host and proxied)

    @Test
    fun `fallback-host request uses flat timeout`() {
        assertThat(timeout(host = HOST_A, isFallback = true, fallbackAvailable = true))
            .isEqualTo(HTTPTimeoutManager.FALLBACK_HOST_TIMEOUT_MS)
    }

    @Test
    fun `fallback-host request uses flat timeout even after a recent timeout`() {
        timeoutManager.recordRequestResult(HOST_A, HTTPTimeoutManager.RequestResult.MAIN_SOURCE_TIMED_OUT)

        assertThat(timeout(host = HOST_A, isFallback = true, fallbackAvailable = true))
            .isEqualTo(HTTPTimeoutManager.FALLBACK_HOST_TIMEOUT_MS)
    }

    @Test
    fun `proxied request uses flat timeout`() {
        assertThat(timeout(host = HOST_A, fallbackAvailable = true, isProxied = true))
            .isEqualTo(HTTPTimeoutManager.DEFAULT_TIMEOUT_MS)
    }

    @Test
    fun `proxied request never consults the per-host memory`() {
        timeoutManager.recordRequestResult(HOST_A, HTTPTimeoutManager.RequestResult.MAIN_SOURCE_TIMED_OUT)

        assertThat(timeout(host = HOST_A, fallbackAvailable = true, isProxied = true))
            .isEqualTo(HTTPTimeoutManager.DEFAULT_TIMEOUT_MS)
    }

    // endregion

    // region Per-host isolation

    @Test
    fun `timeout on one host does not affect another host`() {
        timeoutManager.recordRequestResult(HOST_A, HTTPTimeoutManager.RequestResult.MAIN_SOURCE_TIMED_OUT)

        assertThat(timeout(host = HOST_A, fallbackAvailable = true))
            .isEqualTo(HTTPTimeoutManager.REDUCED_TIMEOUT_MS)
        assertThat(timeout(host = HOST_B, fallbackAvailable = true))
            .isEqualTo(HTTPTimeoutManager.SUPPORTED_FALLBACK_TIMEOUT_MS)
    }

    @Test
    fun `success on one host does not reset another host`() {
        timeoutManager.recordRequestResult(HOST_A, HTTPTimeoutManager.RequestResult.MAIN_SOURCE_TIMED_OUT)
        timeoutManager.recordRequestResult(HOST_B, HTTPTimeoutManager.RequestResult.SUCCESS_ON_MAIN_BACKEND)

        assertThat(timeout(host = HOST_A, fallbackAvailable = true))
            .isEqualTo(HTTPTimeoutManager.REDUCED_TIMEOUT_MS)
    }

    @Test
    fun `multiple hosts retain independent reduced state`() {
        timeoutManager.recordRequestResult(HOST_A, HTTPTimeoutManager.RequestResult.MAIN_SOURCE_TIMED_OUT)
        timeoutManager.recordRequestResult(HOST_B, HTTPTimeoutManager.RequestResult.MAIN_SOURCE_TIMED_OUT)

        // Both hosts are reduced at the same time, each resolving to its own endpoint tier.
        assertThat(timeout(host = HOST_A, fallbackAvailable = false))
            .isEqualTo(HTTPTimeoutManager.MAIN_SOURCE_NO_FALLBACK_REDUCED_TIMEOUT_MS)
        assertThat(timeout(host = HOST_B, fallbackAvailable = true))
            .isEqualTo(HTTPTimeoutManager.REDUCED_TIMEOUT_MS)
    }

    @Test
    fun `success on main backend clears only that host entry`() {
        timeoutManager.recordRequestResult(HOST_A, HTTPTimeoutManager.RequestResult.MAIN_SOURCE_TIMED_OUT)
        assertThat(timeout(host = HOST_A, fallbackAvailable = true))
            .isEqualTo(HTTPTimeoutManager.REDUCED_TIMEOUT_MS)

        timeoutManager.recordRequestResult(HOST_A, HTTPTimeoutManager.RequestResult.SUCCESS_ON_MAIN_BACKEND)
        assertThat(timeout(host = HOST_A, fallbackAvailable = true))
            .isEqualTo(HTTPTimeoutManager.SUPPORTED_FALLBACK_TIMEOUT_MS)
    }

    // endregion

    // region Per-host expiry

    @Test
    fun `per-host entry does not expire before the reset interval`() {
        timeoutManager.recordRequestResult(HOST_A, HTTPTimeoutManager.RequestResult.MAIN_SOURCE_TIMED_OUT)

        dateProvider.advanceTime(HTTPTimeoutManager.TIMEOUT_RESET_INTERVAL_MS - 60000)

        assertThat(timeout(host = HOST_A, fallbackAvailable = true))
            .isEqualTo(HTTPTimeoutManager.REDUCED_TIMEOUT_MS)
    }

    @Test
    fun `per-host entry expires after the reset interval`() {
        timeoutManager.recordRequestResult(HOST_A, HTTPTimeoutManager.RequestResult.MAIN_SOURCE_TIMED_OUT)

        dateProvider.advanceTime(HTTPTimeoutManager.TIMEOUT_RESET_INTERVAL_MS + 1000)

        assertThat(timeout(host = HOST_A, fallbackAvailable = true))
            .isEqualTo(HTTPTimeoutManager.SUPPORTED_FALLBACK_TIMEOUT_MS)
    }

    @Test
    fun `multiple timeouts refresh expiry per host`() {
        timeoutManager.recordRequestResult(HOST_A, HTTPTimeoutManager.RequestResult.MAIN_SOURCE_TIMED_OUT)

        dateProvider.advanceTime(300000)

        timeoutManager.recordRequestResult(HOST_A, HTTPTimeoutManager.RequestResult.MAIN_SOURCE_TIMED_OUT)

        dateProvider.advanceTime(360000)

        // 11 minutes since the first timeout, but only 6 since the second: still reduced
        assertThat(timeout(host = HOST_A, fallbackAvailable = true))
            .isEqualTo(HTTPTimeoutManager.REDUCED_TIMEOUT_MS)
    }

    @Test
    fun `per-host expiry is independent`() {
        timeoutManager.recordRequestResult(HOST_A, HTTPTimeoutManager.RequestResult.MAIN_SOURCE_TIMED_OUT)

        dateProvider.advanceTime(300000)
        timeoutManager.recordRequestResult(HOST_B, HTTPTimeoutManager.RequestResult.MAIN_SOURCE_TIMED_OUT)

        // 6 more minutes: host A is 11 minutes old (expired), host B is 6 minutes old (still valid).
        dateProvider.advanceTime(360000)

        assertThat(timeout(host = HOST_A, fallbackAvailable = true))
            .isEqualTo(HTTPTimeoutManager.SUPPORTED_FALLBACK_TIMEOUT_MS)
        assertThat(timeout(host = HOST_B, fallbackAvailable = true))
            .isEqualTo(HTTPTimeoutManager.REDUCED_TIMEOUT_MS)
    }

    @Test
    fun `host becomes reduced again after entry expires and times out again`() {
        timeoutManager.recordRequestResult(HOST_A, HTTPTimeoutManager.RequestResult.MAIN_SOURCE_TIMED_OUT)

        // Let the entry expire: back to base.
        dateProvider.advanceTime(HTTPTimeoutManager.TIMEOUT_RESET_INTERVAL_MS + 60000)
        assertThat(timeout(host = HOST_A, fallbackAvailable = true))
            .isEqualTo(HTTPTimeoutManager.SUPPORTED_FALLBACK_TIMEOUT_MS)

        // A fresh timeout re-arms the reduced tier.
        timeoutManager.recordRequestResult(HOST_A, HTTPTimeoutManager.RequestResult.MAIN_SOURCE_TIMED_OUT)
        assertThat(timeout(host = HOST_A, fallbackAvailable = true))
            .isEqualTo(HTTPTimeoutManager.REDUCED_TIMEOUT_MS)
    }

    // endregion

    // region Nil host

    @Test
    fun `nil host never uses reduced timeout`() {
        // Recording with a null host is a no-op
        timeoutManager.recordRequestResult(null, HTTPTimeoutManager.RequestResult.MAIN_SOURCE_TIMED_OUT)

        assertThat(timeout(host = null, fallbackAvailable = true))
            .isEqualTo(HTTPTimeoutManager.SUPPORTED_FALLBACK_TIMEOUT_MS)
        assertThat(timeout(host = null, fallbackAvailable = false))
            .isEqualTo(HTTPTimeoutManager.MAIN_SOURCE_NO_FALLBACK_TIMEOUT_MS)
    }

    // endregion

    // region Other result

    @Test
    fun `OTHER_RESULT does not change state`() {
        timeoutManager.recordRequestResult(HOST_A, HTTPTimeoutManager.RequestResult.MAIN_SOURCE_TIMED_OUT)
        assertThat(timeout(host = HOST_A, fallbackAvailable = true))
            .isEqualTo(HTTPTimeoutManager.REDUCED_TIMEOUT_MS)

        timeoutManager.recordRequestResult(HOST_A, HTTPTimeoutManager.RequestResult.OTHER_RESULT)
        assertThat(timeout(host = HOST_A, fallbackAvailable = true))
            .isEqualTo(HTTPTimeoutManager.REDUCED_TIMEOUT_MS)
    }

    // endregion

    // region Test divider

    @Test
    fun `timeouts are divided by TEST_DIVIDER when running tests`() {
        val testAppConfig = mockk<AppConfig>().apply {
            every { runningTests } returns true
        }
        val testManager = HTTPTimeoutManager(testAppConfig, dateProvider)

        assertThat(
            testManager.getTimeoutForRequest(
                host = HOST_A,
                isFallback = false,
                fallbackAvailable = true,
                isProxied = false,
                reTieredTimeoutsEnabled = true,
            )
        ).isEqualTo(HTTPTimeoutManager.SUPPORTED_FALLBACK_TIMEOUT_MS / HTTPTimeoutManager.TEST_DIVIDER)
    }

    // endregion

    // region Re-tiered timeouts disabled (e.g. main-API requests with API sources off)

    @Test
    fun `no-fallback endpoint uses the legacy flat timeout when re-tiered timeouts are disabled`() {
        assertThat(timeout(host = HOST_A, fallbackAvailable = false, reTieredTimeoutsEnabled = false))
            .isEqualTo(HTTPTimeoutManager.DEFAULT_TIMEOUT_MS)
    }

    @Test
    fun `no-fallback endpoint stays flat after a recent timeout when re-tiered timeouts are disabled`() {
        timeoutManager.recordRequestResult(HOST_A, HTTPTimeoutManager.RequestResult.MAIN_SOURCE_TIMED_OUT)

        assertThat(timeout(host = HOST_A, fallbackAvailable = false, reTieredTimeoutsEnabled = false))
            .isEqualTo(HTTPTimeoutManager.DEFAULT_TIMEOUT_MS)
    }

    @Test
    fun `fallback-supporting endpoint tiers are unaffected by the re-tiered flag`() {
        assertThat(timeout(host = HOST_A, fallbackAvailable = true, reTieredTimeoutsEnabled = false))
            .isEqualTo(HTTPTimeoutManager.SUPPORTED_FALLBACK_TIMEOUT_MS)

        timeoutManager.recordRequestResult(HOST_A, HTTPTimeoutManager.RequestResult.MAIN_SOURCE_TIMED_OUT)

        assertThat(timeout(host = HOST_A, fallbackAvailable = true, reTieredTimeoutsEnabled = false))
            .isEqualTo(HTTPTimeoutManager.REDUCED_TIMEOUT_MS)
    }

    @Test
    fun `no-fallback endpoint uses the re-tiered timeout when enabled (e_g_ blob-source downloads)`() {
        assertThat(timeout(host = HOST_A, fallbackAvailable = false, reTieredTimeoutsEnabled = true))
            .isEqualTo(HTTPTimeoutManager.MAIN_SOURCE_NO_FALLBACK_TIMEOUT_MS)

        timeoutManager.recordRequestResult(HOST_A, HTTPTimeoutManager.RequestResult.MAIN_SOURCE_TIMED_OUT)

        assertThat(timeout(host = HOST_A, fallbackAvailable = false, reTieredTimeoutsEnabled = true))
            .isEqualTo(HTTPTimeoutManager.MAIN_SOURCE_NO_FALLBACK_REDUCED_TIMEOUT_MS)
    }

    // endregion
}
