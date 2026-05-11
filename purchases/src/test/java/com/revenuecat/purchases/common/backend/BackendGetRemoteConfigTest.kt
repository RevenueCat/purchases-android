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
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigResponse
import com.revenuecat.purchases.common.remoteconfig.Topic
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
    fun `getRemoteConfig parses successful response`() {
        mockHttpResult(payload = loadJSON(mockResponseFilename))
        var response: RemoteConfigResponse? = null
        backend.getRemoteConfig(
            appInBackground = false,
            onSuccess = { response = it },
            onError = { error -> fail("Expected success. Got error: $error") },
        )

        val parsed = response ?: fail("Expected response to be non-null").let { return }
        assertThat(parsed.apiSources).hasSize(1)
        assertThat(parsed.apiSources[0].id).isEqualTo("primary")
        assertThat(parsed.apiSources[0].url).isEqualTo("https://api.revenuecat.com/")
        assertThat(parsed.apiSources[0].priority).isEqualTo(0)
        assertThat(parsed.apiSources[0].weight).isEqualTo(100)

        assertThat(parsed.blobSources).hasSize(1)
        assertThat(parsed.blobSources[0].id).isEqualTo("cloudfront-primary")
        assertThat(parsed.blobSources[0].urlFormat)
            .isEqualTo("https://assets.revenuecat.com/rc_app_1234/{blob_ref}")
        assertThat(parsed.blobSources[0].priority).isEqualTo(0)
        assertThat(parsed.blobSources[0].weight).isEqualTo(100)

        val pemTopic = parsed.manifest.topics[Topic.PRODUCT_ENTITLEMENT_MAPPING]
        assertThat(pemTopic).isNotNull
        val defaultEntry = pemTopic?.get("DEFAULT")
        assertThat(defaultEntry?.blobRef)
            .isEqualTo("6a4d0f53d9f6b8e2f4dca0fd1c7c4f5e3e1b1ef0f45d989e2f8f8d0d91ec1b6a")
    }

    @Test
    fun `getRemoteConfig drops unknown topic names from manifest`() {
        val payloadWithUnknownTopic = """
            {
              "api_sources": [],
              "blob_sources": [],
              "manifest": {
                "topics": {
                  "product_entitlement_mapping": {
                    "DEFAULT": {
                      "blob_ref": "abc"
                    }
                  },
                  "future_unknown_topic": {
                    "DEFAULT": {
                      "blob_ref": "def"
                    }
                  }
                }
              }
            }
        """.trimIndent()
        mockHttpResult(payload = payloadWithUnknownTopic)

        var response: RemoteConfigResponse? = null
        backend.getRemoteConfig(
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
              "extra_top_level": "ignored",
              "api_sources": [
                {
                  "id": "primary",
                  "url": "https://api.revenuecat.com/",
                  "priority": 0,
                  "weight": 100,
                  "future_field": true
                }
              ],
              "blob_sources": [
                {
                  "id": "primary",
                  "url_format": "https://assets.example/{blob_ref}",
                  "priority": 0,
                  "weight": 100,
                  "future_field": true
                }
              ],
              "manifest": {
                "topics": {}
              }
            }
        """.trimIndent()
        mockHttpResult(payload = payloadWithExtraFields)

        var response: RemoteConfigResponse? = null
        backend.getRemoteConfig(
            appInBackground = false,
            onSuccess = { response = it },
            onError = { error -> fail("Expected success. Got error: $error") },
        )

        assertThat(response).isNotNull
        assertThat(response?.apiSources).hasSize(1)
        assertThat(response?.blobSources).hasSize(1)
    }

    @Test
    fun `getRemoteConfig propagates HTTP errors`() {
        mockHttpResult(
            responseCode = RCHTTPStatusCodes.ERROR,
            payload = """{"code": 7000, "message": "internal error"}""",
        )
        var obtainedError: PurchasesError? = null
        backend.getRemoteConfig(
            appInBackground = false,
            onSuccess = { fail("Expected error. Got success") },
            onError = { error -> obtainedError = error },
        )
        assertThat(obtainedError).isNotNull
    }

    @Test
    fun `getRemoteConfig surfaces serialization errors as PurchasesError`() {
        mockHttpResult(payload = """{"blob_sources": "not-an-array"}""")
        var obtainedError: PurchasesError? = null
        backend.getRemoteConfig(
            appInBackground = false,
            onSuccess = { fail("Expected error. Got success") },
            onError = { error -> obtainedError = error },
        )
        assertThat(obtainedError).isNotNull
    }

    @Test
    fun `getRemoteConfig dedups concurrent calls`() {
        mockHttpResult(payload = loadJSON(mockResponseFilename), delayMs = 200)
        val lock = CountDownLatch(2)
        asyncBackend.getRemoteConfig(
            appInBackground = false,
            onSuccess = { lock.countDown() },
            onError = { fail("Expected success. Got error: $it") },
        )
        asyncBackend.getRemoteConfig(
            appInBackground = false,
            onSuccess = { lock.countDown() },
            onError = { fail("Expected success. Got error: $it") },
        )
        lock.await(5.seconds.inWholeSeconds, TimeUnit.SECONDS)
        assertThat(lock.count).isEqualTo(0)
        verify(exactly = 1) {
            httpClient.performRequest(
                mockBaseURL,
                Endpoint.GetRemoteConfig,
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
