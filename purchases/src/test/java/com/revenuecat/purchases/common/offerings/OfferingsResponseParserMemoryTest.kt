package com.revenuecat.purchases.common.offerings

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.lang.reflect.Method

/**
 * Allocation is tracked through `com.sun.management.ThreadMXBean#getThreadAllocatedBytes`, which counts
 * cumulative bytes per thread and is unaffected by GC timing. Reflection is needed because this module's Kotlin
 * JVM target (1.8) limits the compile-time JDK surface to `java.base`, which excludes `java.management`.
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class OfferingsResponseParserMemoryTest {

    private companion object {
        const val OFFERING_COUNT = 100
        // components_config dominates a real components-heavy response.
        const val COMPONENTS_CONFIG_TARGET_CHARS = 20_000

        val managementFactoryGetThreadMXBean: Method =
            Class.forName("java.lang.management.ManagementFactory").getMethod("getThreadMXBean")
        val getThreadAllocatedBytesMethod: Method =
            Class.forName("com.sun.management.ThreadMXBean")
                .getMethod("getThreadAllocatedBytes", Long::class.javaPrimitiveType)
    }

    @Test
    fun `parsing a components-heavy response allocates a small multiple of the elided tree, not the payload`() {
        val payload = buildComponentsHeavyPayload()
        warmUp()

        var elidedSize = 0
        val elidedBytes = measureAllocatedBytes {
            val result = OfferingsResponseParser.parse(payload)
            elidedSize = result.json.toString().length
        }

        var fullSize = 0
        val fullTreeBytes = measureAllocatedBytes {
            fullSize = JSONObject(payload).toString().length
        }

        println(
            "OfferingsResponseParser memory profile (payload ${payload.length} chars, " +
                "$OFFERING_COUNT offerings with components)",
        )
        println("  elided parse:    ${elidedBytes / 1024} KB allocated, elided tree $elidedSize chars")
        println("  full tree parse: ${fullTreeBytes / 1024} KB allocated, full tree $fullSize chars")

        assertThat(elidedSize).isLessThan(fullSize)
        // The scan walks the payload once, so its cost is bounded by payload size; a full tree build scales
        // with it instead (~8.5x measured). Without the allocation-free skip path this measured ~3x, not ~0.2x.
        assertThat(elidedBytes).isLessThan(payload.length.toLong())
        assertThat(elidedBytes).isLessThan(fullTreeBytes / 10)
    }

    private fun warmUp() {
        val warmUpPayload = buildComponentsHeavyPayload(offeringCount = 1, componentsConfigTargetChars = 100)
        OfferingsResponseParser.parse(warmUpPayload)
        JSONObject(warmUpPayload).toString()
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

    private fun buildComponentsHeavyPayload(
        offeringCount: Int = OFFERING_COUNT,
        componentsConfigTargetChars: Int = COMPONENTS_CONFIG_TARGET_CHARS,
    ): String {
        val componentsConfig = buildComponentsConfig(componentsConfigTargetChars)
        val builder = StringBuilder()
        builder.append("{\"offerings\":[")
        for (index in 0 until offeringCount) {
            if (index > 0) builder.append(',')
            builder.append(
                "{\"identifier\":\"offering_$index\"," +
                    "\"description\":\"paywall \\\"v2\\\" config\"," +
                    "\"packages\":[{\"identifier\":\"monthly\",\"platform_product_identifier\":\"prod_$index\"}]," +
                    "\"paywall_components\":{" +
                    "\"template_name\":\"template_1\"," +
                    "\"asset_base_url\":\"https://example.com\"," +
                    "\"components_config\":$componentsConfig," +
                    "\"components_localizations\":{\"en_US\":{}}," +
                    "\"default_locale\":\"en_US\"}}",
            )
        }
        builder.append("],\"current_offering_id\":null}")
        return builder.toString()
    }

    private fun buildComponentsConfig(targetChars: Int): String {
        val builder = StringBuilder("{\"type\":\"stack\",\"children\":[")
        var index = 0
        while (builder.length < targetChars) {
            if (index > 0) builder.append(',')
            builder.append(
                "{\"type\":\"text\",\"value\":\"Billed at {{ product.price_per_month }}/mo, item $index\"}",
            )
            index++
        }
        builder.append("]}")
        return builder.toString()
    }
}
