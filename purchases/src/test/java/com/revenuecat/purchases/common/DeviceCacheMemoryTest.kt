package com.revenuecat.purchases.common

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.caching.DeviceCache
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class DeviceCacheMemoryTest {
    @Test
    fun `caching a large raw offerings response allocates less than one MiB`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("device_cache_memory_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val cache = DeviceCache(prefs, "test_api_key")
        val response = LargeOfferingsResponseGenerator.generateAtLeast(10 * 1024 * 1024)

        cache.cacheOfferingsResponse("{}", HTTPResponseOriginalSource.MAIN)
        val allocated = ThreadAllocationMeter.measure {
            cache.cacheOfferingsResponse(response.text, HTTPResponseOriginalSource.FALLBACK)
        }

        assertThat(allocated).isLessThan(1024L * 1024L)
        assertThat(
            prefs.getString(
                "com.revenuecat.purchases.test_api_key.offeringsResponse",
                null,
            ),
        ).isSameAs(response.text)
    }
}
