package com.revenuecat.purchases.common.networking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.ThreadAllocationMeter
import com.revenuecat.purchases.common.createResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date

/**
 * Measures the allocation cost of the ETag cache hot paths with large ASCII-heavy payloads.
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
        const val FIVE_MIB = 5 * 1024 * 1024
        const val TEN_MIB = 10 * 1024 * 1024
        const val TWENTY_MIB = 20 * 1024 * 1024
        const val URL = "https://api.revenuecat.com/v1/subscribers/appUserID/offerings"
    }

    private val testDate = Date(1675954145L)
    private lateinit var underTest: ETagManager

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
    }

    @Test
    fun `a 5 MiB 304 cache read allocates less than three times the payload bytes`() {
        profileAllocations(FIVE_MIB)
    }

    @Test
    fun `a 10 MiB 304 cache read allocates less than three times the payload bytes`() {
        profileAllocations(TEN_MIB)
    }

    @Test
    fun `a 20 MiB 304 cache read allocates less than three times the payload bytes`() {
        profileAllocations(TWENTY_MIB)
    }

    private fun profileAllocations(payloadTargetBytes: Int) {
        warmUpMeasuredCodePaths(payloadTargetBytes)
        val payload = buildAsciiPayload(payloadTargetBytes)
        val payloadSizeBytes = payload.toByteArray().size

        val result = HTTPResult.createResult(
            responseCode = RCHTTPStatusCodes.SUCCESS,
            payload = payload,
            origin = HTTPResult.Origin.BACKEND,
            requestDate = testDate,
        )

        val storeBytes = ThreadAllocationMeter.measure {
            underTest.storeBackendResultIfNoError(URL, result, eTagInResponse = "etag")
        }

        val headerBytes = ThreadAllocationMeter.measure {
            val headers = underTest.getETagHeaders(URL, verificationRequested = false)
            assertThat(headers[HTTPRequest.ETAG_HEADER_NAME]).isEqualTo("etag")
        }

        var cacheHit: HTTPResult? = null
        val notModifiedBytes = ThreadAllocationMeter.measure {
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
        // (the legacy combined format allocated tens of MB per operation on a 5 MiB payload; the store's
        // encoder writes through a fixed buffer). The 304 read rebuilds the payload string from its file, so
        // its cost is payload-proportional by design (the deliberate tradeoff for not retaining the payload
        // in the SharedPreferences in-memory map for the process lifetime) but bounded to a small multiple.
        val maxAllowedBytes = 1024L * 1024L
        assertThat(storeBytes).isLessThan(maxAllowedBytes)
        assertThat(headerBytes).isLessThan(maxAllowedBytes)
        // Deterministic exact allocation counting, with no timing or GC dependency. The read holds the
        // file byte[] and constructs the String directly, so it stays below three payload-sized arrays.
        assertThat(notModifiedBytes).isLessThan(3L * payloadSizeBytes)

        println("ETagManager memory profile (payload $payloadSizeBytes bytes, ~${payloadSizeBytes / (1024 * 1024)} MiB)")
        println("  storeBackendResultIfNoError: $storeBytes bytes (${storeBytes / 1024} KiB) allocated")
        println("  getETagHeaders (warm cache): $headerBytes bytes (${headerBytes / 1024} KiB) allocated")
        println("  304 cache-hit read:          $notModifiedBytes bytes (${notModifiedBytes / 1024} KiB) allocated")
    }

    /**
     * Exercises every measured code path once (the three [ETagManager] operations, AssertJ, org.json)
     * against a separate prefs file and URL, so first-use classloading/static-init allocations
     * (multi-MB) are not misattributed to whichever measured operation runs first.
     */
    private fun warmUpMeasuredCodePaths(payloadTargetBytes: Int) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val warmUpPrefs = context.getSharedPreferences(
            "etag_memory_test_warmup_$payloadTargetBytes",
            Context.MODE_PRIVATE,
        )
        warmUpPrefs.edit().clear().commit()
        val warmUpManager = ETagManager(
            context,
            lazy { warmUpPrefs },
            object : DateProvider {
                override val now: Date
                    get() = testDate
            },
        )
        val warmUpUrl = "https://api.revenuecat.com/v1/warmup/$payloadTargetBytes"
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

    private fun buildAsciiPayload(targetBytes: Int): String {
        return buildString(targetBytes) {
            append("{\"data\":\"")
            while (length < targetBytes - 2) {
                append('a')
            }
            append("\"}")
        }
    }
}
