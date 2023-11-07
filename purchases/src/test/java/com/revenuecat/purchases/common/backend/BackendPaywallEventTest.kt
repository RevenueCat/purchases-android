package com.revenuecat.purchases.common.backend

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
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
import com.revenuecat.purchases.paywalls.events.PaywallBackendEvent
import com.revenuecat.purchases.paywalls.events.PaywallEventRequest
import com.revenuecat.purchases.paywalls.events.PaywallEventType
import com.revenuecat.purchases.paywalls.events.PaywallEventsManager
import com.revenuecat.purchases.utils.asMap
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.encodeToJsonElement
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@RunWith(AndroidJUnit4::class)
class BackendPaywallEventTest {

    private val testPaywallEventsURL = URL("http://mock-api-paywall-events-test.revenuecat.com/")
    private val paywallEventRequest = PaywallEventRequest(listOf(
        PaywallBackendEvent(
            id = "id",
            version = 1,
            type = PaywallEventType.CANCEL.value,
            appUserID = "appUserID",
            sessionID = "sessionID",
            offeringID = "offeringID",
            paywallRevision = 5,
            timestamp = 123456789,
            displayMode = "footer",
            darkMode = true,
            localeIdentifier = "en_US",
        )
    ))

    private lateinit var appConfig: AppConfig
    private lateinit var httpClient: HTTPClient

    private lateinit var backend: Backend
    private lateinit var asyncBackend: Backend

    @Before
    fun setUp() {
        appConfig = mockk<AppConfig>().apply {
            every { paywallEventsURL } returns testPaywallEventsURL
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
    fun `postPaywallEvents posts events correctly`() {
        mockHttpResult()
        backend.postPaywallEvents(
            paywallEventRequest,
            onSuccessHandler = {},
            onErrorHandler = { _, _ -> },
        )
        verifyCallWithBody(
            "{" +
                "\"events\":[" +
                    "{" +
                        "\"id\":\"id\"," +
                        "\"version\":1," +
                        "\"type\":\"paywall_cancel\"," +
                        "\"app_user_id\":\"appUserID\"," +
                        "\"session_id\":\"sessionID\"," +
                        "\"offering_id\":\"offeringID\"," +
                        "\"paywall_revision\":5," +
                        "\"timestamp\":123456789," +
                        "\"display_mode\":\"footer\"," +
                        "\"dark_mode\":true," +
                        "\"locale\":\"en_US\"" +
                    "}" +
                "]" +
            "}"
        )
    }

    @Test
    fun `postPaywallEvents calls success handler`() {
        mockHttpResult()
        var successCalled = false
        backend.postPaywallEvents(
            paywallEventRequest,
            onSuccessHandler = { successCalled = true },
            onErrorHandler = { _, _ -> fail("Expected success") },
        )
        assertThat(successCalled).isTrue
    }

    @Test
    fun `postPaywallEvents calls error handler if error response code`() {
        mockHttpResult(responseCode = RCHTTPStatusCodes.ERROR)
        var errorCalled = false
        backend.postPaywallEvents(
            paywallEventRequest,
            onSuccessHandler = { fail("Expected error") },
            onErrorHandler = { _, _ -> errorCalled = true },
        )
        assertThat(errorCalled).isTrue
    }

    @Test
    fun `postPaywallEvents calls error handler with shouldMarkAsSynced false if server error`() {
        mockHttpResult(responseCode = RCHTTPStatusCodes.ERROR)
        var errorCalled = false
        backend.postPaywallEvents(
            paywallEventRequest,
            onSuccessHandler = { fail("Expected error") },
            onErrorHandler = { _, shouldMarkAsSynced ->
                assertThat(shouldMarkAsSynced).isFalse
                errorCalled = true
            },
        )
        assertThat(errorCalled).isTrue
    }

    @Test
    fun `postPaywallEvents calls error handler with shouldMarkAsSynced false if 404`() {
        mockHttpResult(responseCode = RCHTTPStatusCodes.NOT_FOUND)
        var errorCalled = false
        backend.postPaywallEvents(
            paywallEventRequest,
            onSuccessHandler = { fail("Expected error") },
            onErrorHandler = { _, shouldMarkAsSynced ->
                assertThat(shouldMarkAsSynced).isFalse
                errorCalled = true
            },
        )
        assertThat(errorCalled).isTrue
    }

    @Test
    fun `postPaywallEvents calls error handler with shouldMarkAsSynced true if 400`() {
        mockHttpResult(responseCode = RCHTTPStatusCodes.BAD_REQUEST)
        var errorCalled = false
        backend.postPaywallEvents(
            paywallEventRequest,
            onSuccessHandler = { fail("Expected error") },
            onErrorHandler = { _, shouldMarkAsSynced ->
                assertThat(shouldMarkAsSynced).isTrue
                errorCalled = true
            },
        )
        assertThat(errorCalled).isTrue
    }

    @Test
    fun `postPaywallEvents calls error handler with shouldMarkAsSynced true if json error`() {
        mockHttpClientException()
        var errorCalled = false
        backend.postPaywallEvents(
            paywallEventRequest,
            onSuccessHandler = { fail("Expected error") },
            onErrorHandler = { _, shouldMarkAsSynced ->
                assertThat(shouldMarkAsSynced).isTrue
                errorCalled = true
            },
        )
        assertThat(errorCalled).isTrue
    }

    @Test
    fun `postPaywallEvents multiple times with same request, only one is triggered`() {
        mockHttpResult(delayMs = 10)
        val lock = CountDownLatch(2)
        asyncBackend.postPaywallEvents(
            paywallEventRequest,
            onSuccessHandler = { lock.countDown() },
            onErrorHandler = { _, _ -> },
        )
        asyncBackend.postPaywallEvents(
            paywallEventRequest,
            onSuccessHandler = { lock.countDown() },
            onErrorHandler = { _, _ -> },
        )
        lock.await(100, TimeUnit.MILLISECONDS)
        assertThat(lock.count).isEqualTo(0)
        verify(exactly = 1) {
            httpClient.performRequest(
                testPaywallEventsURL,
                Endpoint.PostPaywallEvents,
                body = any(),
                postFieldsToSign = null,
                requestHeaders = any(),
            )
        }
    }

    private fun verifyCallWithBody(body: String) {
        val expectedRequest: PaywallEventRequest = PaywallEventsManager.json.decodeFromString(body)
        val expectedBody = PaywallEventsManager.json.encodeToJsonElement(expectedRequest).asMap()
        verify(exactly = 1) {
            httpClient.performRequest(
                testPaywallEventsURL,
                Endpoint.PostPaywallEvents,
                body = expectedBody,
                postFieldsToSign = null,
                requestHeaders = any(),
            )
        }
    }

    private fun mockHttpResult(
        responseCode: Int = RCHTTPStatusCodes.SUCCESS,
        delayMs: Long? = null
    ) {
        every {
            httpClient.performRequest(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } answers {
            if (delayMs != null) {
                Thread.sleep(delayMs)
            }
            HTTPResult(
                responseCode,
                "{}",
                HTTPResult.Origin.BACKEND,
                requestDate = null,
                VerificationResult.NOT_REQUESTED
            )
        }
    }

    private fun mockHttpClientException() {
        every {
            httpClient.performRequest(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } throws IOException("Test exception")
    }

    private fun createAsyncDispatcher(): Dispatcher {
        return Dispatcher(
            ThreadPoolExecutor(
                1,
                2,
                0,
                TimeUnit.MILLISECONDS,
                LinkedBlockingQueue()
            )
        )
    }
}
