package com.revenuecat.purchases.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class RateLimiterTest {
    @Test
    fun `test allows correct number of accesses`() {
        val rateLimiter = RateLimiter(5, 60.seconds)

        repeat(5) {
            assertThat(rateLimiter.shouldProceed()).isTrue()
        }
    }

    @Test
    fun `test blocks access when limit exceeded`() {
        val rateLimiter = RateLimiter(5, 60.seconds)

        repeat(5) {
            rateLimiter.shouldProceed()
        }

        assertThat(rateLimiter.shouldProceed()).isFalse()
    }

    @Test
    fun `test resets after rate limit period`() {
        val rateLimiter = RateLimiter(1, 1.seconds)

        assertThat(rateLimiter.shouldProceed()).isTrue()
        assertThat(rateLimiter.shouldProceed()).isFalse()

        Thread.sleep(1100)

        assertThat(rateLimiter.shouldProceed()).isTrue()
    }
}