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
        warmUpMeasuredCodePaths()

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

        // Regression gates for #3628. Store and header reads must not allocate anywhere near payload size
        // (the legacy combined format allocated tens of MB per operation on this 5MB payload; the store's
        // encoder writes through a fixed buffer). The 304 read rebuilds the payload string from its file, so
        // its cost is payload-proportional by design — the deliberate tradeoff for not retaining the payload
        // in the SharedPreferences in-memory map for the process lifetime — but bounded to a small multiple.
        val maxAllowedBytes = 1024L * 1024L
        assertThat(storeBytes).isLessThan(maxAllowedBytes)
        assertThat(headerBytes).isLessThan(maxAllowedBytes)
        assertThat(notModifiedBytes).isLessThan(3L * payload.length * Char.SIZE_BYTES)

        println("ETagManager memory profile (payload ${payload.length} chars, ~${payload.length / (1024 * 1024)}MB)")
        println("  storeBackendResultIfNoError: ${storeBytes / 1024} KB allocated")
        println("  getETagHeaders (warm cache): ${headerBytes / 1024} KB allocated")
        println("  304 cache-hit read:          ${notModifiedBytes / 1024} KB allocated")
    }

    /**
     * Exercises every code path the measured blocks hit — the three [ETagManager] operations plus AssertJ
     * and `org.json`'s parsing constructor — once, against a separate URL in a separate prefs file, before
     * any measurement. The first-ever use of those libraries in the process pays a multi-MB
     * classloading/static-init allocation cost that would otherwise be misattributed to whichever measured
     * operation runs first, drowning out the KB-scale steady-state costs the regression gate is about.
     */
    private fun warmUpMeasuredCodePaths() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val warmUpPrefs = context.getSharedPreferences("etag_memory_test_warmup", Context.MODE_PRIVATE)
        warmUpPrefs.edit().clear().commit()
        val warmUpManager = ETagManager(
            context,
            lazy { warmUpPrefs },
            object : DateProvider {
                override val now: Date
                    get() = testDate
            },
        )
        val warmUpUrl = "https://api.revenuecat.com/v1/warmup"
        val warmUpResult = HTTPResult.createResult(
            responseCode = RCHTTPStatusCodes.SUCCESS,
            payload = "{}",
            origin = HTTPResult.Origin.BACKEND,
            requestDate = testDate,
        )
        warmUpManager.storeBackendResultIfNoError(warmUpUrl, warmUpResult, eTagInResponse = "etag")
        val warmUpHeaders = warmUpManager.getETagHeaders(warmUpUrl, verificationRequested = false)
        assertThat(warmUpHeaders[HTTPRequest.ETAG_HEADER_NAME]).isEqualTo("etag")
        val warmUpCacheHit = warmUpManager.getHTTPResultFromCacheOrBackend(
            responseCode = RCHTTPStatusCodes.NOT_MODIFIED,
            payload = "",
            eTagHeader = "etag",
            urlString = warmUpUrl,
            refreshETag = false,
            requestDate = testDate,
            verificationResult = VerificationResult.NOT_REQUESTED,
            isLoadShedderResponse = false,
            isFallbackURL = false,
        )
        assertThat(warmUpCacheHit).isNotNull
        assertThat(warmUpCacheHit!!.payloadText).isEqualTo("{}")
        assertThat(warmUpCacheHit.origin).isEqualTo(HTTPResult.Origin.CACHE)
        warmUpPrefs.edit().clear().commit()
    }

    private fun measureAllocatedBytes(block: () -> Unit): Long {
        val threadId = Thread.currentThread().id
        val before = getThreadAllocatedBytes(threadId)
        // -1 means allocation tracking is disabled/unsupported on this JVM, which would make every gate
        // pass vacuously (0 bytes measured). Fail loudly instead.
        check(before >= 0) { "ThreadMXBean allocation tracking is unavailable; the memory gates cannot run." }
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
