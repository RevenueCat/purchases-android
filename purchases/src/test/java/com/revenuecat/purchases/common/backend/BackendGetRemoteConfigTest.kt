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
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import com.revenuecat.purchases.common.networking.RemoteConfigResponse
import com.revenuecat.purchases.common.networking.Topic
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class BackendGetRemoteConfigTest {

    private val mockBaseURL = URL("http://mock-api-test.revenuecat.com/")
    private val mockResponseFilename = "get_remote_config_success.json"
    private val testUserID = "test-user-id"

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
    fun `getRemoteConfig parses successful response`() {
        mockHttpResult(payload = loadJSON(mockResponseFilename))
        var response: RemoteConfigResponse? = null
        backend.getRemoteConfig(
            appUserID = testUserID,
            appInBackground = false,
            onSuccess = { response = it },
            onError = { error -> fail("Expected success. Got error: $error") },
        )

        val parsed = response ?: fail("Expected response to be non-null").let { return }
        assertThat(parsed.configVersion).isEqualTo("2026-04-15T18:09:12Z:8f3c7a12")
        assertThat(parsed.apiSources).hasSize(2)
        assertThat(parsed.apiSources[0].id).isEqualTo("primary")
        assertThat(parsed.apiSources[0].urlPrefix).isEqualTo("https://api.revenuecat.com")
        assertThat(parsed.apiSources[0].priority).isEqualTo(0)
        assertThat(parsed.apiSources[0].weight).isEqualTo(100)
        assertThat(parsed.apiSources[0].blacklistTimeSeconds).isEqualTo(300L)
        assertThat(parsed.assetSources).hasSize(1)
        assertThat(parsed.assetSources[0].id).isEqualTo("cloudfront-primary")
        assertThat(parsed.assetSources[0].urlFormat)
            .isEqualTo("https://assets.revenuecat.com/rc_app_1234/{blob_ref}")
        assertThat(parsed.assetSources[0].testUrl).isEqualTo("/health")

        val pemTopic = parsed.manifest.topics[Topic.PRODUCT_ENTITLEMENT_MAPPING]
        assertThat(pemTopic).isNotNull
        val defaultEntry = pemTopic?.get("DEFAULT")
        assertThat(defaultEntry?.assetBlobRef)
            .isEqualTo("6a4d0f53d9f6b8e2f4dca0fd1c7c4f5e3e1b1ef0f45d989e2f8f8d0d91ec1b6a")
        assertThat(defaultEntry?.contentType).isEqualTo("application/json")
        assertThat(defaultEntry?.prefetch).isTrue
    }

    @Test
    fun `getRemoteConfig drops unknown topic names from manifest`() {
        val payloadWithUnknownTopic = """
            {
              "config_version": "v1",
              "api_sources": [],
              "asset_sources": [],
              "manifest": {
                "topics": {
                  "product_entitlement_mapping": {
                    "DEFAULT": {
                      "asset_blob_ref": "abc",
                      "content_type": "application/json",
                      "prefetch": false
                    }
                  },
                  "future_unknown_topic": {
                    "DEFAULT": {
                      "asset_blob_ref": "def",
                      "content_type": "application/json"
                    }
                  }
                }
              }
            }
        """.trimIndent()
        mockHttpResult(payload = payloadWithUnknownTopic)

        var response: RemoteConfigResponse? = null
        backend.getRemoteConfig(
            appUserID = testUserID,
            appInBackground = false,
            onSuccess = { response = it },
            onError = { error -> fail("Expected success. Got error: $error") },
        )

        val topics = response?.manifest?.topics ?: fail("Expected response to be non-null").let { return }
        assertThat(topics).hasSize(1)
        assertThat(topics).containsKey(Topic.PRODUCT_ENTITLEMENT_MAPPING)
    }

    @Test
    fun `getRemoteConfig ignores unknown sibling fields`() {
        val payloadWithExtraFields = """
            {
              "config_version": "v1",
              "extra_top_level": "ignored",
              "api_sources": [
                {
                  "id": "primary",
                  "url_prefix": "https://api.revenuecat.com",
                  "priority": 0,
                  "weight": 100,
                  "blacklist_time_seconds": 300,
                  "future_field": true
                }
              ],
              "asset_sources": [],
              "manifest": {
                "topics": {}
              }
            }
        """.trimIndent()
        mockHttpResult(payload = payloadWithExtraFields)

        var response: RemoteConfigResponse? = null
        backend.getRemoteConfig(
            appUserID = testUserID,
            appInBackground = false,
            onSuccess = { response = it },
            onError = { error -> fail("Expected success. Got error: $error") },
        )

        assertThat(response).isNotNull
        assertThat(response?.apiSources).hasSize(1)
    }

    @Test
    fun `getRemoteConfig propagates HTTP errors`() {
        mockHttpResult(
            responseCode = RCHTTPStatusCodes.ERROR,
            payload = """{"code": 7000, "message": "internal error"}""",
        )
        var obtainedError: PurchasesError? = null
        backend.getRemoteConfig(
            appUserID = testUserID,
            appInBackground = false,
            onSuccess = { fail("Expected error. Got success") },
            onError = { error -> obtainedError = error },
        )
        assertThat(obtainedError).isNotNull
    }

    @Test
    fun `getRemoteConfig surfaces serialization errors as PurchasesError`() {
        mockHttpResult(payload = """{"config_version": 12345}""")
        var obtainedError: PurchasesError? = null
        backend.getRemoteConfig(
            appUserID = testUserID,
            appInBackground = false,
            onSuccess = { fail("Expected error. Got success") },
            onError = { error -> obtainedError = error },
        )
        assertThat(obtainedError).isNotNull
    }

    @Test
    fun `getRemoteConfig dedups concurrent calls with same user`() {
        mockHttpResult(payload = loadJSON(mockResponseFilename), delayMs = 200)
        val lock = CountDownLatch(2)
        asyncBackend.getRemoteConfig(
            appUserID = testUserID,
            appInBackground = false,
            onSuccess = { lock.countDown() },
            onError = { fail("Expected success. Got error: $it") },
        )
        asyncBackend.getRemoteConfig(
            appUserID = testUserID,
            appInBackground = false,
            onSuccess = { lock.countDown() },
            onError = { fail("Expected success. Got error: $it") },
        )
        lock.await(5.seconds.inWholeSeconds, TimeUnit.SECONDS)
        assertThat(lock.count).isEqualTo(0)
        verify(exactly = 1) {
            httpClient.performRequest(
                mockBaseURL,
                Endpoint.GetRemoteConfig(testUserID),
                body = null,
                postFieldsToSign = null,
                requestHeaders = any(),
            )
        }
    }

    private fun mockHttpResult(
        responseCode: Int = RCHTTPStatusCodes.SUCCESS,
        payload: String,
        delayMs: Long? = null,
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
                VerificationResult.NOT_REQUESTED,
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

    private fun loadJSON(jsonFileName: String) =
        File(javaClass.classLoader!!.getResource(jsonFileName).file).readText()
}
