package com.revenuecat.purchases.common.backend

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.APIKeyValidator
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BackendHelper
import com.revenuecat.purchases.common.DefaultLocaleProvider
import com.revenuecat.purchases.common.HTTPClient
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.common.SyncDispatcher
import com.revenuecat.purchases.common.getLocale
import com.revenuecat.purchases.common.networking.ETagManager
import com.revenuecat.purchases.common.networking.ETagPayloadStore
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.disabledTokenAuthenticator
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import com.revenuecat.purchases.common.verification.SigningManager
import com.revenuecat.purchases.interfaces.StorefrontProvider
import com.revenuecat.purchases.subscriberattributes.SubscriberAttribute
import com.revenuecat.purchases.subscriberattributes.remoteSubscriberAttributes
import com.revenuecat.purchases.utils.Responses
import io.mockk.every
import io.mockk.mockk
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Subscriber attributes in a CustomerInfo response, served over real HTTP by a [MockWebServer] and read through
 * the real [HTTPClient], [ETagManager] and [Backend], so nothing between the bytes on the wire and the parsed
 * attributes is faked.
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class BackendSubscriberAttributesIntegrationTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val apiKey = "test-api-key"
    private val appUserID = "waldo"

    private val responseBody: String = JSONObject(Responses.validFullPurchaserResponse).apply {
        getJSONObject("subscriber").put(
            "subscriber_attributes",
            JSONObject()
                .put("\$email", JSONObject().put("value", "user@example.com").put("updated_at_ms", 1_600_000_000_000))
                .put("custom_key", JSONObject().put("value", "custom value").put("updated_at_ms", 1_650_000_000_123))
                .put("deleted", JSONObject().put("value", JSONObject.NULL).put("updated_at_ms", 1_700_000_000_000)),
        )
    }.toString()

    private val expectedAttributes = mapOf(
        "\$email" to synced("\$email", "user@example.com", 1_600_000_000_000),
        "custom_key" to synced("custom_key", "custom value", 1_650_000_000_123),
        "deleted" to synced("deleted", null, 1_700_000_000_000),
    )

    private lateinit var server: MockWebServer
    private lateinit var backend: Backend

    @Before
    fun setUp() {
        server = MockWebServer()
        backend = createBackend()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `getCustomerInfo parses the subscriber attributes in the response`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody(responseBody))

        val info = getCustomerInfo()

        assertThat(info.remoteSubscriberAttributes()!!.attributes).isEqualTo(expectedAttributes)
        assertThat(server.takeRequest(1, TimeUnit.SECONDS)?.path).isEqualTo("/v1/subscribers/$appUserID")
    }

    @Test
    fun `getCustomerInfo parses the subscriber attributes again when the response is replayed on a 304`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(responseBody).setHeader(HTTPResult.ETAG_HEADER_NAME, "etag-1"),
        )
        server.enqueue(MockResponse().setResponseCode(304).setHeader(HTTPResult.ETAG_HEADER_NAME, "etag-1"))

        getCustomerInfo()
        val replayed = getCustomerInfo()

        server.takeRequest(1, TimeUnit.SECONDS)
        val secondRequest = server.takeRequest(1, TimeUnit.SECONDS)!!
        assertThat(secondRequest.getHeader(HTTPResult.ETAG_HEADER_NAME)).isEqualTo("etag-1")
        assertThat(replayed.remoteSubscriberAttributes()!!.attributes).isEqualTo(expectedAttributes)
    }

    @Test
    fun `logIn parses the subscriber attributes in the response`() {
        server.enqueue(MockResponse().setResponseCode(201).setBody(responseBody))
        var result: Pair<CustomerInfo, Boolean>? = null

        backend.logIn(
            appUserID,
            "new_user",
            { info, created -> result = info to created },
            { error -> throw AssertionError("logIn failed: $error") },
        )

        val (info, created) = result ?: error("Expected logIn to complete synchronously")
        assertThat(created).isTrue()
        assertThat(info.remoteSubscriberAttributes()!!.attributes).isEqualTo(expectedAttributes)
    }

    @Test
    fun `getCustomerInfo has no subscriber attributes when the response has none`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody(Responses.validFullPurchaserResponse))

        assertThat(getCustomerInfo().remoteSubscriberAttributes()).isNull()
    }

    private fun getCustomerInfo(): CustomerInfo {
        var info: CustomerInfo? = null
        var error: PurchasesError? = null
        backend.getCustomerInfo(
            appUserID = appUserID,
            appInBackground = false,
            onSuccess = { info = it },
            onError = { e, _ -> error = e },
        )
        return info ?: throw AssertionError("getCustomerInfo didn't succeed: $error")
    }

    private fun createBackend(): Backend {
        val appConfig = AppConfig(
            context = mockk<Context>(relaxed = true).apply {
                every { packageName } answers { "mock-package-name" }
                every { getLocale() } returns Locale.US
            },
            purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
            showInAppMessagesAutomatically = false,
            platformInfo = PlatformInfo("native", "1.0"),
            proxyURL = server.url("/").toUrl(),
            store = Store.PLAY_STORE,
            isDebugBuild = false,
            apiKeyValidationResult = APIKeyValidator.ValidationResult.VALID,
            runningTests = true,
        )
        val preferences = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("backend_subscriber_attributes_etags", Context.MODE_PRIVATE)
            .also { it.edit().clear().commit() }
        val eTagManager = ETagManager(
            mockk(),
            lazy { preferences },
            payloadStore = ETagPayloadStore(temporaryFolder.newFolder()),
        )
        val httpClient = HTTPClient(
            appConfig,
            eTagManager,
            diagnosticsTrackerIfEnabled = null,
            SigningManager(SignatureVerificationMode.Disabled, appConfig, apiKey),
            mockk<StorefrontProvider>().apply { every { getStorefront() } returns "US" },
            apiSourceFailover = null,
            localeProvider = DefaultLocaleProvider(),
            tokenAuthenticator = disabledTokenAuthenticator(),
        )
        val dispatcher = SyncDispatcher()
        val backendHelper = BackendHelper(apiKey, dispatcher, appConfig, httpClient)
        return Backend(appConfig, dispatcher, dispatcher, httpClient, backendHelper)
    }

    private fun synced(key: String, value: String?, updatedAtMs: Long) =
        SubscriberAttribute(key, value, setTime = Date(updatedAtMs), isSynced = true)
}
