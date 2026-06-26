package com.revenuecat.purchases.common.backend

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BackendHelper
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.HTTPClient
import com.revenuecat.purchases.common.SyncDispatcher
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.RCContainer
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class BackendGetRemoteConfigTest {

    private val mockBaseURL = URL("http://mock-api-test.revenuecat.com/")

    private val testDomain = "app"
    private val testAppUserID = "test-app-user-id"
    private val testManifest = "v1.1710000100.sources:etag1"
    private val testPrefetchedBlobs = listOf("blobRefA")

    private lateinit var appConfig: AppConfig
    private lateinit var httpClient: HTTPClient

    private lateinit var backend: Backend
    private lateinit var asyncBackend: Backend

    @Before
    fun setUp() {
        appConfig = mockk<AppConfig>().apply {
            every { baseURL } returns mockBaseURL
            every { customEntitlementComputation } returns false
            every { fallbackBaseURLs } returns emptyList()
            every { isDebugBuild } returns false
        }
        httpClient = mockk()
        val backendHelper = BackendHelper("TEST_API_KEY", SyncDispatcher(), appConfig, httpClient)

        val asyncDispatcher1 = createAsyncDispatcher()
        val asyncDispatcher2 = createAsyncDispatcher()
        val asyncBackendHelper = BackendHelper("TEST_API_KEY", asyncDispatcher1, appConfig, httpClient)

        backend = Backend(
            appConfig,
            SyncDispatcher(),
            SyncDispatcher(),
            httpClient,
            backendHelper,
        )

        asyncBackend = Backend(
            appConfig,
            asyncDispatcher1,
            asyncDispatcher2,
            httpClient,
            asyncBackendHelper,
        )
    }

    @Test
    fun `getRemoteConfig parses successful RC Format response into an RCContainer`() {
        val config = "{\"hello\":\"world\"}".toByteArray()
        val element = "blob-bytes".toByteArray()
        mockHttpResult(
            payload = HTTPResult.Payload.RCFormat(buildContainer(config = config, elements = listOf(element))),
            verificationResult = VerificationResult.VERIFIED,
        )

        var container: RCContainer? = null
        var verification: VerificationResult? = null
        backend.getRemoteConfig(
            appInBackground = false,
            appUserID = testAppUserID,
            domain = testDomain,
            manifest = testManifest,
            prefetchedBlobs = testPrefetchedBlobs,
            onSuccess = { result, verificationResult ->
                container = result
                verification = verificationResult
            },
            onError = { error -> fail("Expected success. Got error: $error") },
        )

        val parsed = container ?: fail("Expected container to be non-null").let { return }
        assertThat(parsed.version).isEqualTo(1)
        assertThat(parsed.config.data.let { buf -> ByteArray(buf.remaining()).also { buf.duplicate().get(it) } })
            .isEqualTo(config)
        assertThat(parsed.contentElements).hasSize(1)
        assertThat(verification).isEqualTo(VerificationResult.VERIFIED)
    }

    @Test
    fun `getRemoteConfig surfaces malformed RC Format payload as PurchasesError`() {
        mockHttpResult(payload = HTTPResult.Payload.RCFormat("not a container".toByteArray()))

        var obtainedError: PurchasesError? = null
        backend.getRemoteConfig(
            appInBackground = false,
            appUserID = testAppUserID,
            domain = testDomain,
            manifest = testManifest,
            prefetchedBlobs = testPrefetchedBlobs,
            onSuccess = { _, _ -> fail("Expected error. Got success") },
            onError = { error -> obtainedError = error },
        )
        assertThat(obtainedError).isNotNull
    }

    @Test
    fun `getRemoteConfig surfaces a non-RC-Format payload as PurchasesError`() {
        mockHttpResult(payload = HTTPResult.Payload.Text("{}"))

        var obtainedError: PurchasesError? = null
        backend.getRemoteConfig(
            appInBackground = false,
            appUserID = testAppUserID,
            domain = testDomain,
            manifest = testManifest,
            prefetchedBlobs = testPrefetchedBlobs,
            onSuccess = { _, _ -> fail("Expected error. Got success") },
            onError = { error -> obtainedError = error },
        )
        assertThat(obtainedError).isNotNull
    }

    @Test
    fun `getRemoteConfig surfaces a 204 No Content response as a null container`() {
        var callbackCount = 0
        var container: RCContainer? = mockk()
        mockHttpResult(
            responseCode = RCHTTPStatusCodes.NO_CONTENT,
            payload = HTTPResult.Payload.RCFormat(ByteArray(0)),
            verificationResult = VerificationResult.VERIFIED,
        )

        var verification: VerificationResult? = null
        backend.getRemoteConfig(
            appInBackground = false,
            appUserID = testAppUserID,
            domain = testDomain,
            manifest = testManifest,
            prefetchedBlobs = testPrefetchedBlobs,
            onSuccess = { result, verificationResult ->
                callbackCount++
                container = result
                verification = verificationResult
            },
            onError = { error -> fail("Expected success. Got error: $error") },
        )

        assertThat(callbackCount).isEqualTo(1)
        assertThat(container).isNull()
        assertThat(verification).isEqualTo(VerificationResult.VERIFIED)
    }

    @Test
    fun `getRemoteConfig sends app_user_id, opaque manifest and prefetched_blobs as the request body`() {
        val bodySlot = mutableListOf<Map<String, Any?>?>()
        mockNoContentRequest(bodySlot)

        backend.getRemoteConfig(
            appInBackground = false,
            appUserID = testAppUserID,
            domain = testDomain,
            manifest = testManifest,
            prefetchedBlobs = testPrefetchedBlobs,
            onSuccess = { _, _ -> },
            onError = { error -> fail("Expected success. Got error: $error") },
        )

        val body = bodySlot.firstOrNull()
        assertThat(body).isNotNull
        assertThat(body?.keys).containsExactlyInAnyOrder("app_user_id", "manifest", "prefetched_blobs")
        assertThat(body?.get("app_user_id")).isEqualTo(testAppUserID)
        // The manifest is replayed verbatim as the opaque string the SDK received.
        assertThat(body?.get("manifest")).isEqualTo(testManifest)
        assertThat(body?.get("prefetched_blobs")).isEqualTo(listOf("blobRefA"))
    }

    @Test
    fun `getRemoteConfig sends the domain as a path segment`() {
        val endpointSlot = slot<Endpoint>()
        every {
            httpClient.performRequest(
                any(),
                capture(endpointSlot),
                body = any(),
                postFieldsToSign = any(),
                requestHeaders = any(),
                fallbackBaseURLs = any(),
            )
        } returns HTTPResult(
            RCHTTPStatusCodes.NO_CONTENT,
            HTTPResult.Payload.RCFormat(ByteArray(0)),
            HTTPResult.Origin.BACKEND,
            requestDate = null,
            VerificationResult.NOT_REQUESTED,
            isLoadShedderResponse = false,
            isFallbackURL = false,
        )

        backend.getRemoteConfig(
            appInBackground = false,
            appUserID = testAppUserID,
            domain = testDomain,
            manifest = testManifest,
            prefetchedBlobs = testPrefetchedBlobs,
            onSuccess = { _, _ -> },
            onError = { error -> fail("Expected success. Got error: $error") },
        )

        assertThat(endpointSlot.captured).isEqualTo(Endpoint.GetRemoteConfig(testDomain))
        assertThat(endpointSlot.captured.getPath()).isEqualTo("/v1/config/app")
    }

    @Test
    fun `getRemoteConfig omits the manifest on the first run`() {
        val bodySlot = mutableListOf<Map<String, Any?>?>()
        mockNoContentRequest(bodySlot)

        backend.getRemoteConfig(
            appInBackground = false,
            appUserID = testAppUserID,
            domain = testDomain,
            manifest = null,
            prefetchedBlobs = emptyList(),
            onSuccess = { _, _ -> },
            onError = { error -> fail("Expected success. Got error: $error") },
        )

        val body = bodySlot.firstOrNull()
        assertThat(body?.keys).containsExactlyInAnyOrder("app_user_id", "prefetched_blobs")
        assertThat(body).doesNotContainKey("manifest")
    }

    @Test
    fun `getRemoteConfig propagates HTTP errors`() {
        mockHttpResult(
            responseCode = RCHTTPStatusCodes.ERROR,
            payload = HTTPResult.Payload.Text("""{"code": 7000, "message": "internal error"}"""),
        )
        var obtainedError: PurchasesError? = null
        backend.getRemoteConfig(
            appInBackground = false,
            appUserID = testAppUserID,
            domain = testDomain,
            manifest = testManifest,
            prefetchedBlobs = testPrefetchedBlobs,
            onSuccess = { _, _ -> fail("Expected error. Got success") },
            onError = { error -> obtainedError = error },
        )
        assertThat(obtainedError).isNotNull
    }

    @Test
    fun `getRemoteConfig dedups concurrent calls`() {
        mockHttpResult(payload = HTTPResult.Payload.RCFormat(buildContainer()), delayMs = 200)
        val lock = CountDownLatch(2)
        asyncBackend.getRemoteConfig(
            appInBackground = false,
            appUserID = testAppUserID,
            domain = testDomain,
            manifest = testManifest,
            prefetchedBlobs = testPrefetchedBlobs,
            onSuccess = { _, _ -> lock.countDown() },
            onError = { fail("Expected success. Got error: $it") },
        )
        asyncBackend.getRemoteConfig(
            appInBackground = false,
            appUserID = testAppUserID,
            domain = testDomain,
            manifest = testManifest,
            prefetchedBlobs = testPrefetchedBlobs,
            onSuccess = { _, _ -> lock.countDown() },
            onError = { fail("Expected success. Got error: $it") },
        )
        lock.await(5.seconds.inWholeSeconds, TimeUnit.SECONDS)
        assertThat(lock.count).isEqualTo(0)
        verify(exactly = 1) {
            httpClient.performRequest(
                mockBaseURL,
                Endpoint.GetRemoteConfig(testDomain),
                body = any(),
                postFieldsToSign = null,
                requestHeaders = any(),
            )
        }
    }

    @Test
    fun `getRemoteConfig does not dedup concurrent calls for different app user ids`() {
        mockHttpResult(payload = HTTPResult.Payload.RCFormat(buildContainer()), delayMs = 200)
        val lock = CountDownLatch(2)
        asyncBackend.getRemoteConfig(
            appInBackground = false,
            appUserID = "user-a",
            domain = testDomain,
            manifest = testManifest,
            prefetchedBlobs = testPrefetchedBlobs,
            onSuccess = { _, _ -> lock.countDown() },
            onError = { fail("Expected success. Got error: $it") },
        )
        asyncBackend.getRemoteConfig(
            appInBackground = false,
            appUserID = "user-b",
            domain = testDomain,
            manifest = testManifest,
            prefetchedBlobs = testPrefetchedBlobs,
            onSuccess = { _, _ -> lock.countDown() },
            onError = { fail("Expected success. Got error: $it") },
        )
        lock.await(5.seconds.inWholeSeconds, TimeUnit.SECONDS)
        assertThat(lock.count).isEqualTo(0)
        // Different users must each get their own request rather than sharing the first caller's response.
        verify(exactly = 2) {
            httpClient.performRequest(
                mockBaseURL,
                Endpoint.GetRemoteConfig(testDomain),
                body = any(),
                postFieldsToSign = null,
                requestHeaders = any(),
            )
        }
    }

    private fun mockNoContentRequest(bodySlot: MutableList<Map<String, Any?>?>) {
        every {
            httpClient.performRequest(
                any(),
                any(),
                body = captureNullable(bodySlot),
                postFieldsToSign = any(),
                requestHeaders = any(),
                fallbackBaseURLs = any(),
            )
        } returns HTTPResult(
            RCHTTPStatusCodes.NO_CONTENT,
            HTTPResult.Payload.RCFormat(ByteArray(0)),
            HTTPResult.Origin.BACKEND,
            requestDate = null,
            VerificationResult.NOT_REQUESTED,
            isLoadShedderResponse = false,
            isFallbackURL = false,
        )
    }

    private fun mockHttpResult(
        responseCode: Int = RCHTTPStatusCodes.SUCCESS,
        payload: HTTPResult.Payload,
        delayMs: Long? = null,
        verificationResult: VerificationResult = VerificationResult.NOT_REQUESTED,
    ) {
        every {
            httpClient.performRequest(
                any(),
                any(),
                any(),
                any(),
                any(),
                fallbackBaseURLs = any(),
            )
        } answers {
            if (delayMs != null) {
                Thread.sleep(delayMs)
            }
            HTTPResult(
                responseCode,
                payload,
                HTTPResult.Origin.BACKEND,
                requestDate = null,
                verificationResult,
                isLoadShedderResponse = false,
                isFallbackURL = false,
            )
        }
    }

    private fun createAsyncDispatcher(): Dispatcher {
        return Dispatcher(
            ThreadPoolExecutor(
                1,
                2,
                0,
                TimeUnit.MILLISECONDS,
                LinkedBlockingQueue(),
            ),
        )
    }

    /** Builds a minimal valid RC Container Format v1 payload (header + config + content elements). */
    @Suppress("MagicNumber")
    private fun buildContainer(
        version: Int = 1,
        flags: Int = 0,
        config: ByteArray = ByteArray(0),
        elements: List<ByteArray> = emptyList(),
    ): ByteArray {
        val out = ByteArrayOutputStream()
        out.write('R'.code)
        out.write('C'.code)
        out.write(version and 0xFF)
        out.write(flags and 0xFF)
        repeat(4) { out.write(0) } // header reserved

        val allElements = listOf(config) + elements
        allElements.forEach { element ->
            out.write(MessageDigest.getInstance("SHA-256").digest(element).copyOf(24))
            out.writeUInt32(element.size)
            out.writeUInt32(0) // element reserved
            out.write(element)
            while (out.size() % 8 != 0) out.write(0)
        }
        return out.toByteArray()
    }

    @Suppress("MagicNumber")
    private fun ByteArrayOutputStream.writeUInt32(value: Int) {
        write(value and 0xFF)
        write((value ushr 8) and 0xFF)
        write((value ushr 16) and 0xFF)
        write((value ushr 24) and 0xFF)
    }
}
