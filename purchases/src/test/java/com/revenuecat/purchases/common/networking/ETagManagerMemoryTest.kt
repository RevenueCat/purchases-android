package com.revenuecat.purchases.common.networking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.createResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.lang.reflect.Method
import java.util.Date

/**
 * Measures the allocation cost of the ETag cache hot paths with an offerings-sized payload.
 * Allocation is tracked per-thread through the JDK's `com.sun.management.ThreadMXBean#getThreadAllocatedBytes`,
 * which counts cumulative allocated bytes and is unaffected by GC timing. Accessed via reflection because this
 * module's Kotlin JVM target (1.8) restricts the compile-time JDK API surface to `java.base`, which doesn't
 * expose the `java.management`/`jdk.management` modules that this API lives in. Documents the memory behavior
 * behind https://github.com/RevenueCat/purchases-android/issues/3628.
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ETagManagerMemoryTest {

    private companion object {
        const val PAYLOAD_TARGET_BYTES = 5 * 1024 * 1024
        const val URL = "https://api.revenuecat.com/v1/subscribers/appUserID/offerings"

        val managementFactoryGetThreadMXBean: Method =
            Class.forName("java.lang.management.ManagementFactory").getMethod("getThreadMXBean")
        val getThreadAllocatedBytesMethod: Method =
            Class.forName("com.sun.management.ThreadMXBean")
                .getMethod("getThreadAllocatedBytes", Long::class.javaPrimitiveType)
    }

    private val testDate = Date(1675954145L)
    private lateinit var underTest: ETagManager
    private lateinit var payload: String

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("etag_memory_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        underTest = ETagManager(
            context,
            lazy { prefs },
            object : DateProvider {
                override val now: Date
                    get() = testDate
            },
        )
        payload = buildOfferingsLikePayload()
    }

    @Test
    fun `measure allocations of store and read paths with an offerings-sized payload`() {
        val result = HTTPResult.createResult(
            responseCode = RCHTTPStatusCodes.SUCCESS,
            payload = payload,
            origin = HTTPResult.Origin.BACKEND,
            requestDate = testDate,
        )

        val storeBytes = measureAllocatedBytes {
            underTest.storeBackendResultIfNoError(URL, result, eTagInResponse = "etag")
        }

        val headerBytes = measureAllocatedBytes {
            val headers = underTest.getETagHeaders(URL, verificationRequested = false)
            assertThat(headers[HTTPRequest.ETAG_HEADER_NAME]).isEqualTo("etag")
        }

        var cacheHit: HTTPResult? = null
        val notModifiedBytes = measureAllocatedBytes {
            cacheHit = underTest.getHTTPResultFromCacheOrBackend(
                responseCode = RCHTTPStatusCodes.NOT_MODIFIED,
                payload = "",
                eTagHeader = "etag",
                urlString = URL,
                refreshETag = false,
                requestDate = testDate,
                verificationResult = VerificationResult.NOT_REQUESTED,
                isLoadShedderResponse = false,
                isFallbackURL = false,
            )
        }
        assertThat(cacheHit).isNotNull
        assertThat(cacheHit!!.payloadText).isEqualTo(payload)
        assertThat(cacheHit!!.origin).isEqualTo(HTTPResult.Origin.CACHE)

        println("ETagManager memory profile (payload ${payload.length} chars, ~${payload.length / (1024 * 1024)}MB)")
        println("  storeBackendResultIfNoError: ${storeBytes / 1024} KB allocated")
        println("  getETagHeaders (warm cache): ${headerBytes / 1024} KB allocated")
        println("  304 cache-hit read:          ${notModifiedBytes / 1024} KB allocated")
    }

    private fun measureAllocatedBytes(block: () -> Unit): Long {
        val threadId = Thread.currentThread().id
        val before = getThreadAllocatedBytes(threadId)
        block()
        return getThreadAllocatedBytes(threadId) - before
    }

    // Reflection is used here (instead of importing java.lang.management.ManagementFactory /
    // com.sun.management.ThreadMXBean directly) because this module compiles Kotlin with jvmTarget 1.8 on a
    // newer JDK, which makes Kotlin restrict the compile-time JDK API surface to java.base only, hiding the
    // java.management/jdk.management modules these classes live in. The classes themselves are present at
    // runtime, so reflection resolves them without needing a build-wide compiler flag change.
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
                    "\"description\":\"paywall \\\"v2\\\" config\"," +
                    "\"metadata\":{\"title\":\"Premium\",\"cta\":\"Subscribe now\"}," +
                    "\"packages\":[{\"identifier\":\"monthly\"," +
                    "\"platform_product_identifier\":\"prod_$index\"}]}",
            )
            index++
        }
        builder.append("]}")
        return builder.toString()
    }
}
