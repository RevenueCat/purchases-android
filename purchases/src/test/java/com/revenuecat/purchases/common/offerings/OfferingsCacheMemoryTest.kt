package com.revenuecat.purchases.common.offerings

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.common.DefaultLocaleProvider
import com.revenuecat.purchases.common.SyncDispatcher
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.caching.PayloadFileStore
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.lang.reflect.Method

/**
 * Measures the allocation cost of caching an offerings-sized response, through the JDK's
 * `com.sun.management.ThreadMXBean#getThreadAllocatedBytes` (reflection: jvmTarget 1.8 hides
 * java.management at compile time). Exact counting, so unaffected by GC timing.
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class OfferingsCacheMemoryTest {

    private companion object {
        const val PAYLOAD_TARGET_BYTES = 5 * 1024 * 1024

        val managementFactoryGetThreadMXBean: Method =
            Class.forName("java.lang.management.ManagementFactory").getMethod("getThreadMXBean")
        val getThreadAllocatedBytesMethod: Method =
            Class.forName("com.sun.management.ThreadMXBean")
                .getMethod("getThreadAllocatedBytes", Long::class.javaPrimitiveType)
    }

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var offeringsCache: OfferingsCache
    private lateinit var payload: String

    @Before
    fun setUp() {
        sharedPreferences = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("offerings_cache_memory_test", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().commit()
        offeringsCache = OfferingsCache(
            DeviceCache(sharedPreferences, "api_key"),
            responseStore = PayloadFileStore(temporaryFolder.newFolder()),
            dispatcher = SyncDispatcher(),
            localeProvider = DefaultLocaleProvider(),
        )
        payload = buildOfferingsLikePayload()
    }

    @Test
    fun `caching an offerings-sized response allocates nothing payload-sized and keeps it out of prefs`() {
        val offerings = Offerings(current = null, all = emptyMap())
        offeringsCache.cacheOfferings(offerings, "{\"warm\":\"up\"}")
        val samePayload = String(payload.toCharArray())

        val storeBytes = measureAllocatedBytes { offeringsCache.cacheOfferings(offerings, payload) }
        val unchangedStoreBytes = measureAllocatedBytes { offeringsCache.cacheOfferings(offerings, samePayload) }

        // Both stream through fixed-size buffers (~1MB measured); a payload-sized ByteArray or char[] fails these.
        val maxAllowedBytes = payload.length / 2L
        assertThat(storeBytes).isLessThan(maxAllowedBytes)
        assertThat(unchangedStoreBytes).isLessThan(maxAllowedBytes)
        assertThat(sharedPreferences.all.values.maxOf { it.toString().length }).isLessThan(100)

        println("OfferingsCache memory profile (payload ${payload.length} chars)")
        println("  cacheOfferings, new response:       ${storeBytes / 1024} KB allocated")
        println("  cacheOfferings, unchanged response: ${unchangedStoreBytes / 1024} KB allocated")
    }

    private fun measureAllocatedBytes(block: () -> Unit): Long {
        val threadId = Thread.currentThread().id
        val before = getThreadAllocatedBytes(threadId)
        check(before >= 0) { "ThreadMXBean allocation tracking is unavailable; the memory gates cannot run." }
        block()
        return getThreadAllocatedBytes(threadId) - before
    }

    private fun getThreadAllocatedBytes(threadId: Long): Long {
        val threadMXBean = managementFactoryGetThreadMXBean.invoke(null)
        return getThreadAllocatedBytesMethod.invoke(threadMXBean, threadId) as Long
    }

    private fun buildOfferingsLikePayload(): String {
        val builder = StringBuilder(PAYLOAD_TARGET_BYTES + 1024)
        builder.append("{\"offerings\":[")
        var index = 0
        while (builder.length < PAYLOAD_TARGET_BYTES) {
            if (index > 0) builder.append(',')
            builder.append(
                "{\"identifier\":\"offering_$index\"," +
                    "\"packages\":[{\"identifier\":\"monthly\"," +
                    "\"platform_product_identifier\":\"prod_$index\"}]}",
            )
            index++
        }
        builder.append("]}")
        return builder.toString()
    }
}
