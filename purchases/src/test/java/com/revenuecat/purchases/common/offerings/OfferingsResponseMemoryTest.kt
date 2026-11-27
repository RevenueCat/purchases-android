package com.revenuecat.purchases.common.offerings

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BackendHelper
import com.revenuecat.purchases.common.BaseHTTPClientTest
import com.revenuecat.purchases.common.DefaultLocaleProvider
import com.revenuecat.purchases.common.SyncDispatcher
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.caching.PayloadFileStore
import com.revenuecat.purchases.common.networking.ETagManager
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.verification.IntermediateSignatureHelper
import com.revenuecat.purchases.common.verification.Signature
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import com.revenuecat.purchases.common.verification.SignatureVerifier
import com.revenuecat.purchases.common.verification.SigningManager
import com.revenuecat.purchases.utils.Result
import io.mockk.every
import io.mockk.mockk
import okhttp3.mockwebserver.MockResponse
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.lang.reflect.Method

/**
 * Measures what handling an offerings response dominated by `paywall_components` allocates, as the
 * fallback host serves it, through `com.sun.management.ThreadMXBean#getThreadAllocatedBytes`
 * (reflection: jvmTarget 1.8 hides java.management at compile time). Exact counting on the calling
 * thread, so MockWebServer's own buffers and GC timing don't affect it.
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class OfferingsResponseMemoryTest : BaseHTTPClientTest() {

    private companion object {
        const val PAYLOAD_TARGET_BYTES = 5 * 1024 * 1024
        const val API_KEY = "test_api_key"
        const val APP_USER_ID = "appUserID"
        const val WARM_UP_PARSES = 3

        val managementFactoryGetThreadMXBean: Method =
            Class.forName("java.lang.management.ManagementFactory").getMethod("getThreadMXBean")
        val getThreadAllocatedBytesMethod: Method =
            Class.forName("com.sun.management.ThreadMXBean")
                .getMethod("getThreadAllocatedBytes", Long::class.javaPrimitiveType)
    }

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val payload: String by lazy { OfferingsResponsePayloads.withHeavyPaywallComponents(PAYLOAD_TARGET_BYTES) }

    @Test
    fun `reading the cached offerings response`() {
        val offeringsCache = createOfferingsCache()
        val offerings = Offerings(current = null, all = emptyMap())
        offeringsCache.cacheOfferings(offerings, OfferingsResponsePayloads.withHeavyPaywallComponents(1))
        assertThat(offeringsCache.cachedOfferingsResponse).isNotNull
        offeringsCache.cacheOfferings(offerings, payload)

        lateinit var cachedResponse: JSONObject
        val allocatedBytes = measureAllocatedBytesBesidesParsing {
            cachedResponse = offeringsCache.cachedOfferingsResponse!!
        }

        assertThat(cachedResponse.getJSONArray("offerings").length())
            .isEqualTo(OfferingsResponsePayloads.OFFERING_COUNT)
        report("disk cache read", allocatedBytes)
        // Streamed from the file: 0.1x the payload measured on top of the parsing, against 2x when the
        // payload was loaded whole.
        assertThat(allocatedBytes).isLessThan(payload.length / 2L)
    }

    @Test
    fun `fetching offerings without signature verification`() {
        val signingManager = SigningManager(SignatureVerificationMode.Disabled, createAppConfig(), API_KEY)

        val allocatedBytes = measureFetch(signingManager) { MockResponse().setBody(it) }

        report("network fetch, verification off", allocatedBytes)
        // The body bytes, with no String of the whole body: 1x the payload measured on top of the parsing,
        // against 5x with a growing read buffer and the decoded String.
        assertThat(allocatedBytes).isLessThan(payload.length * 7L / 4)
    }

    @Test
    fun `fetching offerings with signature verification`() {
        val acceptingVerifier = object : SignatureVerifier {
            override fun verify(signatureToVerify: ByteArray, messageToVerify: ByteArray) = true
        }
        val intermediateSignatureHelper = mockk<IntermediateSignatureHelper> {
            every { canVerify() } returns true
            every { createIntermediateKeyVerifierIfVerified(any()) } returns Result.Success(acceptingVerifier)
        }
        val signingManager = SigningManager(
            SignatureVerificationMode.Informational(intermediateSignatureHelper),
            createAppConfig(),
            API_KEY,
        )
        val signature = Base64.encodeToString(ByteArray(Signature.Component.totalSize), Base64.NO_WRAP)

        val allocatedBytes = measureFetch(signingManager) {
            MockResponse().setBody(it)
                .setHeader(HTTPResult.SIGNATURE_HEADER_NAME, signature)
                .setHeader(HTTPResult.REQUEST_TIME_HEADER_NAME, 1234567890L)
        }

        report("network fetch, verification on", allocatedBytes)
        // One more payload-sized array than without verification, the signed message: 2x measured, against
        // 7x.
        assertThat(allocatedBytes).isLessThan(payload.length * 11L / 4)
    }

    private fun measureFetch(signingManager: SigningManager, response: (body: String) -> MockResponse): Long {
        val backend = createBackend(signingManager)
        server.enqueue(response(OfferingsResponsePayloads.withHeavyPaywallComponents(1)))
        assertThat(fetchOfferings(backend)).isNotNull
        server.enqueue(response(payload))

        var fetchedResponse: JSONObject? = null
        val allocatedBytes = measureAllocatedBytesBesidesParsing { fetchedResponse = fetchOfferings(backend) }

        assertThat(fetchedResponse!!.getJSONArray("offerings").length())
            .isEqualTo(OfferingsResponsePayloads.OFFERING_COUNT)
        return allocatedBytes
    }

    private fun fetchOfferings(backend: Backend): JSONObject? {
        var response: JSONObject? = null
        backend.getOfferings(
            APP_USER_ID,
            appInBackground = false,
            onSuccess = { body, _, _ -> response = body },
            onError = { error, _ -> error("Unexpected error fetching offerings: $error") },
        )
        return response
    }

    private fun createBackend(signingManager: SigningManager): Backend {
        val appConfig = createAppConfig()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("offerings_response_memory_test_etags", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val eTagManager = ETagManager(
            context,
            lazy { prefs },
            payloadStore = PayloadFileStore(temporaryFolder.newFolder()),
        )
        val httpClient = createClient(appConfig = appConfig, eTagManager = eTagManager, signingManager = signingManager)
        val dispatcher = SyncDispatcher()
        return Backend(
            appConfig,
            dispatcher,
            dispatcher,
            httpClient,
            BackendHelper(API_KEY, dispatcher, appConfig, httpClient),
        )
    }

    private fun createOfferingsCache(): OfferingsCache {
        val sharedPreferences = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("offerings_response_memory_test", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().commit()
        return OfferingsCache(
            DeviceCache(sharedPreferences, API_KEY),
            responseStore = PayloadFileStore(temporaryFolder.newFolder()),
            ioDispatcher = SyncDispatcher(),
            localeProvider = DefaultLocaleProvider(),
        )
    }

    /**
     * What [block] allocates on top of parsing [payload] once. The parsing can't be a constant to allow
     * for: android.util.JsonReader alone allocates from 0.1x to over 3x the payload just to skip through
     * it, depending on what ran earlier in the test JVM, so it is measured around [block].
     */
    private fun measureAllocatedBytesBesidesParsing(block: () -> Unit): Long {
        repeat(WARM_UP_PARSES) { OfferingsResponseReader.read(payload) }
        val parsingBytesBefore = measureAllocatedBytes { OfferingsResponseReader.read(payload) }
        val allocatedBytes = measureAllocatedBytes(block)
        val parsingBytesAfter = measureAllocatedBytes { OfferingsResponseReader.read(payload) }
        return allocatedBytes - maxOf(parsingBytesBefore, parsingBytesAfter)
    }

    private fun report(scenario: String, allocatedBytes: Long) {
        val payloadMultiple = "%.2f".format(allocatedBytes.toDouble() / payload.length)
        println(
            "OfferingsResponseMemory [$scenario]: ${allocatedBytes / 1024} KB allocated besides parsing, " +
                "${payloadMultiple}x the ${payload.length / 1024} KB payload",
        )
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
}
