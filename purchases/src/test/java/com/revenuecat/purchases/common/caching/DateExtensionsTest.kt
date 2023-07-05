package com.revenuecat.purchases.common.caching

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.utils.subtract
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class DateExtensionsTest {

    @Test
    fun `cache date is stale if more than 5 minutes old and app in foreground`() {
        val date = Date().subtract(6.minutes)
        assertThat(date.isCacheStale(appInBackground = false)).isTrue
    }

    @Test
    fun `cache date is not stale if less than 5 minutes old and app in foreground`() {
        val date = Date().subtract(4.minutes)
        assertThat(date.isCacheStale(appInBackground = false)).isFalse
    }

    @Test
    fun `cache date is not stale if more than 5 minutes old and app in background`() {
        val date = Date().subtract(6.minutes)
        assertThat(date.isCacheStale(appInBackground = true)).isFalse
    }

    @Test
    fun `cache date is stale if more than 25 hours old and app in background`() {
        val date = Date().subtract(26.hours)
        assertThat(date.isCacheStale(appInBackground = true)).isTrue
    }

    @Test
    fun `cache date is stale if more than cache duration time has passed`() {
        val date = Date().subtract(6.minutes)
        assertThat(date.isCacheStale(cacheDuration = 5.minutes)).isTrue
    }

    @Test
    fun `cache date is not stale if less than cache duration time has passed`() {
        val date = Date().subtract(4.minutes)
        assertThat(date.isCacheStale(cacheDuration = 5.minutes)).isFalse
    }
}
