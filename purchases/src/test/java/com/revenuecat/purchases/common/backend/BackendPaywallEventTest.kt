package com.revenuecat.purchases.common.backend

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BackendHelper
import com.revenuecat.purchases.common.Delay
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.HTTPClient
import com.revenuecat.purchases.common.SyncDispatcher
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.common.events.BackendEvent
import com.revenuecat.purchases.common.events.BackendStoredEvent
import com.revenuecat.purchases.common.events.EventsRequest
import com.revenuecat.purchases.common.events.toBackendEvent
import com.revenuecat.purchases.common.events.toBackendStoredEvent
import com.revenuecat.purchases.paywalls.events.PaywallEvent
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import com.revenuecat.purchases.common.JsonProvider
import com.revenuecat.purchases.paywalls.events.PaywallEventType
import com.revenuecat.purchases.utils.asMap
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.Date
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class BackendPaywallEventTest {

    private val paywallEventRequest = EventsRequest(listOf(
        BackendStoredEvent.Paywalls(
            BackendEvent.Paywalls(
                id = "id",
                version = 1,
                type = PaywallEventType.CANCEL.value,
                appUserID = "appUserID",
                sessionID = "sessionID",
                offeringID = "offeringID",
                paywallID = "paywallID",
                paywallRevision = 5,
                timestamp = 123456789,
                displayMode = "footer",
                darkMode = true,
                localeIdentifier = "en_US",
            )
        )
    ).map { it.toBackendEvent() })

    private val placementTargetingEventRequest = EventsRequest(listOf(
        BackendStoredEvent.Paywalls(
            BackendEvent.Paywalls(
                id = "placement-id",
                version = 1,
                type = PaywallEventType.IMPRESSION.value,
                appUserID = "appUserID",
                sessionID = "sessionID",
                offeringID = "offeringID",
                paywallID = "paywallID",
                paywallRevision = 5,
                timestamp = 123456789,
                displayMode = "full_screen",
                darkMode = true,
                localeIdentifier = "es_ES",
                presentedOfferingContext = BackendEvent.PresentedOfferingContextData(
                    placementIdentifier = "home_banner",
                    targetingRevision = 3,
                    targetingRuleId = "rule_abc123",
                ),
            )
        )
    ).map { it.toBackendEvent() })

    private val exitOfferEventRequest = EventsRequest(listOf(
        BackendStoredEvent.Paywalls(
            BackendEvent.Paywalls(
                id = "exit-offer-id",
                version = 1,
                type = PaywallEventType.EXIT_OFFER.value,
                appUserID = "appUserID",
                sessionID = "sessionID",
                offeringID = "offeringID",
                paywallID = "paywallID",
                paywallRevision = 3,
                timestamp = 123456789,
                displayMode = "fullscreen",
                darkMode = false,
                localeIdentifier = "en_US",
                exitOfferType = "dismiss",
                exitOfferingID = "exit-offering-id",
            )
        )
    ).map { it.toBackendEvent() })

    private lateinit var appConfig: AppConfig
    private lateinit var httpClient: HTTPClient

    private lateinit var backend: Backend
    private lateinit var asyncBackend: Backend

    @Before
    fun setUp() {
        appConfig = mockk()
        every { appConfig.fallbackBaseURLs } returns emptyList()
        httpClient = mockk()
        unmockkObject(JsonProvider)
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
        backend.postEvents(
            paywallEventRequest,
            baseURL = AppConfig.paywallEventsURL,
            delay = Delay.DEFAULT,
            onSuccessHandler = {},
            onErrorHandler = { _, _ -> },
        )
        verifyCallWithBody(
            "{" +
                "\"events\":[" +
                    "{" +
                        "\"discriminator\":\"paywalls\"," +
                         "\"id\":\"id\"," +
                        "\"version\":1," +
                        "\"type\":\"paywall_cancel\"," +
                        "\"app_user_id\":\"appUserID\"," +
                        "\"session_id\":\"sessionID\"," +
                        "\"offering_id\":\"offeringID\"," +
                        "\"paywall_id\":\"paywallID\"," +
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
    fun `postPaywallEvents posts events with placement and targeting correctly`() {
        mockHttpResult()
        backend.postEvents(
            placementTargetingEventRequest,
            baseURL = AppConfig.paywallEventsURL,
            delay = Delay.DEFAULT,
            onSuccessHandler = {},
            onErrorHandler = { _, _ -> },
        )
        verifyCallWithBody(
            "{" +
                "\"events\":[" +
                    "{" +
                        "\"discriminator\":\"paywalls\"," +
                        "\"id\":\"placement-id\"," +
                        "\"version\":1," +
                        "\"type\":\"paywall_impression\"," +
                        "\"app_user_id\":\"appUserID\"," +
                        "\"session_id\":\"sessionID\"," +
                        "\"offering_id\":\"offeringID\"," +
                        "\"paywall_id\":\"paywallID\"," +
                        "\"paywall_revision\":5," +
                        "\"timestamp\":123456789," +
                        "\"display_mode\":\"full_screen\"," +
                        "\"dark_mode\":true," +
                        "\"locale\":\"es_ES\"," +
                        "\"presented_offering_context\":{" +
                            "\"placement_identifier\":\"home_banner\"," +
                            "\"targeting_revision\":3," +
                            "\"targeting_rule_id\":\"rule_abc123\"" +
                        "}" +
                    "}" +
                "]" +
            "}"
        )
    }

    @Test
    fun `postPaywallEvents posts exit offer events correctly`() {
        mockHttpResult()
        backend.postEvents(
            exitOfferEventRequest,
            baseURL = AppConfig.paywallEventsURL,
            delay = Delay.DEFAULT,
            onSuccessHandler = {},
            onErrorHandler = { _, _ -> },
        )
        verifyCallWithBody(
            "{" +
                "\"events\":[" +
                    "{" +
                        "\"discriminator\":\"paywalls\"," +
                        "\"id\":\"exit-offer-id\"," +
                        "\"version\":1," +
                        "\"type\":\"paywall_exit_offer\"," +
                        "\"app_user_id\":\"appUserID\"," +
                        "\"session_id\":\"sessionID\"," +
                        "\"offering_id\":\"offeringID\"," +
                        "\"paywall_id\":\"paywallID\"," +
                        "\"paywall_revision\":3," +
                        "\"timestamp\":123456789," +
                        "\"display_mode\":\"fullscreen\"," +
                        "\"dark_mode\":false," +
                        "\"locale\":\"en_US\"," +
                        "\"exit_offer_type\":\"dismiss\"," +
                        "\"exit_offering_id\":\"exit-offering-id\"" +
                    "}" +
                "]" +
            "}"
        )
    }

    @Test
    fun `postPaywallEvents calls success handler`() {
        mockHttpResult()
        var successCalled = false
        backend.postEvents(
            paywallEventRequest,
            baseURL = AppConfig.paywallEventsURL,
            delay = Delay.DEFAULT,
            onSuccessHandler = { successCalled = true },
            onErrorHandler = { _, _ -> fail("Expected success") },
        )
        assertThat(successCalled).isTrue
    }

    @Test
    fun `postPaywallEvents calls error handler if error response code`() {
        mockHttpResult(responseCode = RCHTTPStatusCodes.ERROR)
        var errorCalled = false
        backend.postEvents(
            paywallEventRequest,
            baseURL = AppConfig.paywallEventsURL,
            delay = Delay.DEFAULT,
            onSuccessHandler = { fail("Expected error") },
            onErrorHandler = { _, _ -> errorCalled = true },
        )
        assertThat(errorCalled).isTrue
    }

    @Test
    fun `postPaywallEvents calls error handler with shouldMarkAsSynced false if server error`() {
        mockHttpResult(responseCode = RCHTTPStatusCodes.ERROR)
        var errorCalled = false
        backend.postEvents(
            paywallEventRequest,
            baseURL = AppConfig.paywallEventsURL,
            delay = Delay.DEFAULT,
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
        backend.postEvents(
            paywallEventRequest,
            baseURL = AppConfig.paywallEventsURL,
            delay = Delay.DEFAULT,
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
        backend.postEvents(
            paywallEventRequest,
            baseURL = AppConfig.paywallEventsURL,
            delay = Delay.DEFAULT,
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
        mockkObject(JsonProvider)
        val mockJson = mockk<Json>()
        every {
            JsonProvider.defaultJson
        } returns mockJson

        every {
            mockJson.encodeToJsonElement(paywallEventRequest)
        } answers {
            JsonPrimitive(123)
        }

        var errorCalled = false
        backend.postEvents(
            paywallEventRequest,
            baseURL = AppConfig.paywallEventsURL,
            delay = Delay.DEFAULT,
            onSuccessHandler = { fail("Expected error") },
            onErrorHandler = { _, shouldMarkAsSynced ->
                assertThat(shouldMarkAsSynced).isTrue
                errorCalled = true
            },
        )
        assertThat(errorCalled).isTrue
        unmockkObject(EventsRequest)
    }

    @Test
    fun `postPaywallEvents calls error handler with shouldMarkAsSynced false if a network error is raised`() {
        mockHttpClientException()
        var errorCalled = false
        backend.postEvents(
            paywallEventRequest,
            baseURL = AppConfig.paywallEventsURL,
            delay = Delay.DEFAULT,
            onSuccessHandler = { fail("Expected error") },
            onErrorHandler = { _, shouldMarkAsSynced ->
                assertThat(shouldMarkAsSynced).isFalse()
                errorCalled = true
            },
        )
        assertThat(errorCalled).isTrue
    }

    @Test
    fun `postPaywallEvents multiple times with same request, only one is triggered`() {
        mockHttpResult(delayMs = 10)
        val lock = CountDownLatch(2)
        asyncBackend.postEvents(
            paywallEventRequest,
            baseURL = AppConfig.paywallEventsURL,
            delay = Delay.DEFAULT,
            onSuccessHandler = { lock.countDown() },
            onErrorHandler = { _, _ -> },
        )
        asyncBackend.postEvents(
            paywallEventRequest,
            baseURL = AppConfig.paywallEventsURL,
            delay = Delay.DEFAULT,
            onSuccessHandler = { lock.countDown() },
            onErrorHandler = { _, _ -> },
        )
        lock.await(100, TimeUnit.MILLISECONDS)
        assertThat(lock.count).isEqualTo(0)
        verify(exactly = 1) {
            httpClient.performRequest(
                AppConfig.paywallEventsURL,
                Endpoint.PostEvents,
                body = any(),
                postFieldsToSign = null,
                requestHeaders = any(),
            )
        }
    }

    @Test
    fun `toBackendStoredEvent preserves placement and targeting from PresentedOfferingContext`() {
        val paywallEvent = PaywallEvent(
            creationData = PaywallEvent.CreationData(
                id = UUID.fromString("298207f4-87af-4b57-a581-eb27bcc6e009"),
                date = Date(1699270688884)
            ),
            data = PaywallEvent.Data(
                paywallIdentifier = "paywallID",
                presentedOfferingContext = PresentedOfferingContext(
                    offeringIdentifier = "offeringID",
                    placementIdentifier = "home_banner",
                    targetingContext = PresentedOfferingContext.TargetingContext(
                        revision = 3,
                        ruleId = "rule_abc123",
                    ),
                ),
                paywallRevision = 5,
                sessionIdentifier = UUID.fromString("315107f4-98bf-4b68-a582-eb27bcb6e111"),
                displayMode = "footer",
                localeIdentifier = "es_ES",
                darkMode = true
            ),
            type = PaywallEventType.IMPRESSION,
        )

        val storedEvent = paywallEvent.toBackendStoredEvent("testAppUserId")
        assertThat(storedEvent).isNotNull
        assertThat(storedEvent).isInstanceOf(BackendStoredEvent.Paywalls::class.java)

        val backendEvent = (storedEvent as BackendStoredEvent.Paywalls).event
        assertThat(backendEvent.offeringID).isEqualTo("offeringID")
        assertThat(backendEvent.presentedOfferingContext).isEqualTo(
            BackendEvent.PresentedOfferingContextData(
                placementIdentifier = "home_banner",
                targetingRevision = 3,
                targetingRuleId = "rule_abc123",
            )
        )
    }

    @Test
    fun `old stored BackendStoredEvent without presentedOfferingContext deserializes correctly`() {
        val oldJson = """
            {"discriminator":"paywalls","event":{"discriminator":"paywalls","id":"test-id","version":1,"type":"paywall_impression","app_user_id":"appUserID","session_id":"sessionID","offering_id":"offeringID","paywall_id":"paywallID","paywall_revision":5,"timestamp":123456789,"display_mode":"full_screen","dark_mode":true,"locale":"es_ES"}}
        """.trimIndent()
        val deserialized = JsonProvider.defaultJson.decodeFromString<BackendStoredEvent>(oldJson)
        assertThat(deserialized).isInstanceOf(BackendStoredEvent.Paywalls::class.java)
        val event = (deserialized as BackendStoredEvent.Paywalls).event
        assertThat(event.presentedOfferingContext).isNull()
        assertThat(event.offeringID).isEqualTo("offeringID")
    }

    private fun verifyCallWithBody(body: String) {
        val expectedRequest: EventsRequest = JsonProvider.defaultJson.decodeFromString(body)
        val expectedBody = JsonProvider.defaultJson.encodeToJsonElement(expectedRequest).asMap()
        verify(exactly = 1) {
            httpClient.performRequest(
                AppConfig.paywallEventsURL,
                Endpoint.PostEvents,
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
                fallbackBaseURLs = any(),
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
                VerificationResult.NOT_REQUESTED,
                isLoadShedderResponse = false,
                isFallbackURL = false,
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
