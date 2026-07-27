package com.revenuecat.purchases.common.backend

import android.content.Context
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.APIKeyValidator
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.DangerousSettings
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BackendHelper
import com.revenuecat.purchases.common.DefaultLocaleProvider
import com.revenuecat.purchases.common.HTTPClient
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.common.SyncDispatcher
import com.revenuecat.purchases.common.getLocale
import com.revenuecat.purchases.common.networking.APISourceFailover
import com.revenuecat.purchases.common.networking.ETagManager
import com.revenuecat.purchases.common.networking.ETagPayloadStore
import com.revenuecat.purchases.common.networking.SourceHealthChecker
import com.revenuecat.purchases.common.remoteconfig.ConfigTopic
import com.revenuecat.purchases.common.remoteconfig.DefaultRemoteConfigSourceProvider
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigSource
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import com.revenuecat.purchases.common.remoteconfig.RemoteConfiguration
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import com.revenuecat.purchases.common.verification.SigningManager
import com.revenuecat.purchases.interfaces.StorefrontProvider
import com.revenuecat.purchases.utils.Responses
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * End-to-end coverage of API source failover through a real [Backend.getCustomerInfo] request: real
 * [HTTPClient], [APISourceFailover], [DefaultRemoteConfigSourceProvider], [ETagManager] and
 * [SourceHealthChecker] (real HTTP health checks), with [MockWebServer]s standing in for the sources.
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class BackendAPISourceFailoverIntegrationTest {

    private sealed interface FetchResult {
        data class Success(val customerInfo: CustomerInfo) : FetchResult
        data class Error(val error: PurchasesError, val isServerError: Boolean) : FetchResult
    }

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val apiKey = "test-api-key"
    private val appUserID = "integration-test-user"
    private val customerInfoPath = "/v1/subscribers/$appUserID"
    private val healthPath = "/v1/health/connectivity"

    private lateinit var sourceAServer: MockWebServer
    private lateinit var sourceBServer: MockWebServer

    @Before
    fun setUp() {
        sourceAServer = MockWebServer()
        sourceBServer = MockWebServer()
    }

    @After
    fun tearDown() {
        sourceAServer.shutdown()
        sourceBServer.shutdown()
    }

    @Test
    fun `getCustomerInfo succeeds on the primary source without health checks`() {
        sourceAServer.routeResponses(endpoint = customerInfoResponse(), health = response(200))
        val backend = createBackend(sourceAServer.url("/").toString(), sourceBServer.url("/").toString())

        val result = fetchCustomerInfo(backend)

        assertThat(result).isInstanceOf(FetchResult.Success::class.java)
        assertThat(sourceAServer.recordedPaths()).containsExactly(customerInfoPath)
        assertThat(sourceBServer.requestCount).isEqualTo(0)
    }

    @Test
    fun `getCustomerInfo surfaces a 5xx without failing over when the source is healthy`() {
        sourceAServer.routeResponses(endpoint = response(500), health = response(200))
        val backend = createBackend(sourceAServer.url("/").toString(), sourceBServer.url("/").toString())

        val result = fetchCustomerInfo(backend)

        assertThat(result).isInstanceOf(FetchResult.Error::class.java)
        assertThat((result as FetchResult.Error).isServerError).isTrue()
        assertThat(sourceAServer.recordedPaths()).containsExactly(customerInfoPath, healthPath)
        assertThat(sourceBServer.requestCount).isEqualTo(0)
    }

    @Test
    fun `getCustomerInfo fails over on a 5xx when the source is unhealthy, and stays failed over`() {
        sourceAServer.routeResponses(endpoint = response(500), health = response(503))
        sourceBServer.routeResponses(endpoint = customerInfoResponse(), health = response(200))
        val backend = createBackend(sourceAServer.url("/").toString(), sourceBServer.url("/").toString())

        val result = fetchCustomerInfo(backend)

        assertThat(result).isInstanceOf(FetchResult.Success::class.java)
        assertThat(sourceAServer.recordedPaths()).containsExactly(customerInfoPath, healthPath)
        assertThat(sourceBServer.recordedPaths()).containsExactly(customerInfoPath)

        // The provider advanced, so a subsequent request goes straight to the second source.
        val secondResult = fetchCustomerInfo(backend)

        assertThat(secondResult).isInstanceOf(FetchResult.Success::class.java)
        assertThat(sourceAServer.requestCount).isEqualTo(2)
        assertThat(sourceBServer.requestCount).isEqualTo(2)
        assertThat(sourceBServer.takeRequest(1, TimeUnit.SECONDS)?.path).isEqualTo(customerInfoPath)
    }

    @Test
    fun `getCustomerInfo fails over when the source is unreachable`() {
        val unreachableUrl = unreachableSourceUrl()
        sourceBServer.routeResponses(endpoint = customerInfoResponse(), health = response(200))
        val backend = createBackend(unreachableUrl, sourceBServer.url("/").toString())

        val result = fetchCustomerInfo(backend)

        assertThat(result).isInstanceOf(FetchResult.Success::class.java)
        assertThat(sourceBServer.recordedPaths()).containsExactly(customerInfoPath)
    }

    @Test
    fun `getCustomerInfo surfaces a connection error without failing over when the source is healthy`() {
        // DISCONNECT_AFTER_REQUEST (not AT_START): the server must read the request for the path-routing
        // dispatcher to run, and it still closes the socket without a response, so the client sees an
        // IOException on the customer info request while the health endpoint keeps answering 200.
        sourceAServer.routeResponses(
            endpoint = response(200).apply { socketPolicy = SocketPolicy.DISCONNECT_AFTER_REQUEST },
            health = response(200),
        )
        val backend = createBackend(sourceAServer.url("/").toString(), sourceBServer.url("/").toString())

        val result = fetchCustomerInfo(backend)

        assertThat(result).isInstanceOf(FetchResult.Error::class.java)
        assertThat((result as FetchResult.Error).error.code).isEqualTo(PurchasesErrorCode.NetworkError)
        // The health check ran and passed, and we still never failed over to the second source.
        assertThat(sourceAServer.recordedPaths()).contains(healthPath)
        assertThat(sourceBServer.requestCount).isEqualTo(0)
    }

    @Test
    fun `getCustomerInfo surfaces the error once every source is unhealthy`() {
        sourceAServer.routeResponses(endpoint = response(500), health = response(503))
        sourceBServer.routeResponses(endpoint = response(500), health = response(503))
        val backend = createBackend(sourceAServer.url("/").toString(), sourceBServer.url("/").toString())

        val result = fetchCustomerInfo(backend)

        assertThat(result).isInstanceOf(FetchResult.Error::class.java)
        assertThat((result as FetchResult.Error).isServerError).isTrue()
        assertThat(sourceAServer.recordedPaths()).containsExactly(customerInfoPath, healthPath)
        assertThat(sourceBServer.recordedPaths()).containsExactly(customerInfoPath, healthPath)
    }

    @Test
    fun `getCustomerInfo does not fail over or health check on a 4xx`() {
        sourceAServer.routeResponses(endpoint = response(404), health = response(200))
        val backend = createBackend(sourceAServer.url("/").toString(), sourceBServer.url("/").toString())

        val result = fetchCustomerInfo(backend)

        assertThat(result).isInstanceOf(FetchResult.Error::class.java)
        assertThat((result as FetchResult.Error).isServerError).isFalse()
        assertThat(sourceAServer.recordedPaths()).containsExactly(customerInfoPath)
        assertThat(sourceBServer.requestCount).isEqualTo(0)
    }

    // region Helpers

    private fun fetchCustomerInfo(backend: Backend): FetchResult {
        var result: FetchResult? = null
        backend.getCustomerInfo(
            appUserID = appUserID,
            appInBackground = false,
            onSuccess = { result = FetchResult.Success(it) },
            onError = { error, isServerError -> result = FetchResult.Error(error, isServerError) },
        )
        return result ?: error("Expected the request to complete synchronously")
    }

    /** Wires the full real stack: sources come from a `sources` topic pointing at [sourceUrls] in order. */
    private fun createBackend(vararg sourceUrls: String): Backend {
        val appConfig = createAppConfig()
        val topic = sourcesTopic(sourceUrls.toList())
        val sourceProvider = DefaultRemoteConfigSourceProvider(
            { requestedTopic -> topic.takeIf { requestedTopic == RemoteConfigTopic.Sources } },
        )
        val apiSourceFailover = APISourceFailover(appConfig, sourceProvider, SourceHealthChecker())
        val sharedPreferencesEditor = mockk<SharedPreferences.Editor>().apply {
            every { putString(any(), any()) } returns this
            every { remove(any()) } returns this
            every { apply() } just Runs
        }
        val sharedPreferences = mockk<SharedPreferences>().apply {
            every { getString(any(), any()) } answers { secondArg() as String? }
            every { edit() } returns sharedPreferencesEditor
        }
        val eTagManager = ETagManager(
            mockk(),
            lazy { sharedPreferences },
            payloadStore = ETagPayloadStore(temporaryFolder.newFolder()),
        )
        val signingManager = SigningManager(SignatureVerificationMode.Disabled, appConfig, apiKey)
        val storefrontProvider = mockk<StorefrontProvider>().apply {
            every { getStorefront() } returns "JP"
        }
        val httpClient = HTTPClient(
            appConfig,
            eTagManager,
            diagnosticsTrackerIfEnabled = null,
            signingManager,
            storefrontProvider,
            apiSourceFailover,
            localeProvider = DefaultLocaleProvider(),
        )
        val dispatcher = SyncDispatcher()
        val backendHelper = BackendHelper(apiKey, dispatcher, appConfig, httpClient)
        return Backend(appConfig, dispatcher, dispatcher, httpClient, backendHelper)
    }

    private fun createAppConfig(): AppConfig = AppConfig(
        context = createContext(),
        purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
        showInAppMessagesAutomatically = false,
        platformInfo = PlatformInfo("native", "1.0"),
        proxyURL = null,
        store = Store.PLAY_STORE,
        isDebugBuild = false,
        apiKeyValidationResult = APIKeyValidator.ValidationResult.VALID,
        dangerousSettings = DangerousSettings(usesRemoteConfigAPISources = true),
        runningTests = true,
    )

    private fun createContext(): Context = mockk<Context>(relaxed = true).apply {
        every { packageName } answers { "mock-package-name" }
        every { getLocale() } returns Locale.US
    }

    /** Builds a `sources` ConfigTopic whose api entries point at [urls], highest priority first. */
    private fun sourcesTopic(urls: List<String>): ConfigTopic {
        val item = RemoteConfiguration.ConfigItem(
            metadata = buildJsonObject {
                putJsonArray("sources") {
                    urls.forEachIndexed { index, url ->
                        addJsonObject {
                            put("url", url)
                            put("priority", index * 10)
                            put("weight", 1)
                        }
                    }
                }
            },
        )
        return ConfigTopic(mapOf("api" to item))
    }

    /** Routes requests by path so a server answers both its API endpoint and its health endpoint. */
    private fun MockWebServer.routeResponses(endpoint: MockResponse, health: MockResponse) {
        dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                if (request.path == healthPath) health else endpoint
        }
    }

    private fun MockWebServer.recordedPaths(): List<String> =
        (0 until requestCount).mapNotNull { takeRequest(1, TimeUnit.SECONDS)?.path }

    private fun response(responseCode: Int): MockResponse = MockResponse()
        .setResponseCode(responseCode)
        .setBody("{}")

    private fun customerInfoResponse(): MockResponse = MockResponse()
        .setResponseCode(200)
        .setBody(Responses.validFullPurchaserResponse)

    /** A url whose port refuses connections, to simulate an unreachable source. */
    private fun unreachableSourceUrl(): String {
        val downServer = MockWebServer()
        val url = downServer.url("/").toString()
        downServer.shutdown()
        return url
    }

    // endregion
}
