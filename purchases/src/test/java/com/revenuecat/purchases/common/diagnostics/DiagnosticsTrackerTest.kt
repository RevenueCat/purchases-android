package com.revenuecat.purchases.common.diagnostics

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.common.SyncDispatcher
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.playServicesVersionName
import com.revenuecat.purchases.common.playStoreVersionName
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class DiagnosticsTrackerTest {

    private val testDiagnosticsEntry = DiagnosticsEntry.Event(
        name = DiagnosticsEventName.HTTP_REQUEST_PERFORMED,
        properties = mapOf("test-key-1" to "test-value-1")
    )

    private val testAnonymizedEvent = DiagnosticsEntry.Event(
        name = DiagnosticsEventName.HTTP_REQUEST_PERFORMED,
        properties = mapOf("test-key-1" to "test-anonymized-value-1")
    )

    private lateinit var diagnosticsFileHelper: DiagnosticsFileHelper
    private lateinit var diagnosticsAnonymizer: DiagnosticsAnonymizer
    private lateinit var dispatcher: Dispatcher

    private lateinit var diagnosticsTracker: DiagnosticsTracker

    private lateinit var context: Context
    private lateinit var appConfig: AppConfig

    @Before
    fun setup() {
        mockkStatic("com.revenuecat.purchases.common.UtilsKt")
        context = mockk<Context>(relaxed = true).apply {
            every { playStoreVersionName } returns "123"
            every { playServicesVersionName } returns "456"
        }
        appConfig = AppConfig(
            context = context,
            observerMode = true,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null,
            store = Store.PLAY_STORE
        )
        diagnosticsFileHelper = mockk()
        diagnosticsAnonymizer = mockk()
        dispatcher = SyncDispatcher()

        diagnosticsTracker = DiagnosticsTracker(
            appConfig,
            diagnosticsFileHelper,
            diagnosticsAnonymizer,
            dispatcher
        )

        every { diagnosticsAnonymizer.anonymizeEntryIfNeeded(any()) } answers { firstArg() }
    }

    @After
    fun tearDown() {
        unmockkStatic("com.revenuecat.purchases.common.UtilsKt")
    }

    @Test
    fun `trackEvent performs correct calls`() {
        every { diagnosticsFileHelper.appendEntryToDiagnosticsFile(testAnonymizedEvent) } just Runs
        every { diagnosticsAnonymizer.anonymizeEntryIfNeeded(testDiagnosticsEntry) } returns testAnonymizedEvent
        diagnosticsTracker.trackEvent(testDiagnosticsEntry)
        verify(exactly = 1) { diagnosticsFileHelper.appendEntryToDiagnosticsFile(testAnonymizedEvent) }
        verify(exactly = 1) { diagnosticsAnonymizer.anonymizeEntryIfNeeded(testDiagnosticsEntry) }
    }

    @Test
    fun `trackEvent handles IOException`() {
        every { diagnosticsAnonymizer.anonymizeEntryIfNeeded(testDiagnosticsEntry) } returns testDiagnosticsEntry
        every { diagnosticsFileHelper.appendEntryToDiagnosticsFile(any()) } throws IOException()
        diagnosticsTracker.trackEvent(testDiagnosticsEntry)
    }

    @Test
    fun `trackEventInCurrentThread does not enqueue request`() {
        dispatcher.close()
        every { diagnosticsAnonymizer.anonymizeEntryIfNeeded(testDiagnosticsEntry) } returns testDiagnosticsEntry
        every { diagnosticsFileHelper.appendEntryToDiagnosticsFile(any()) } throws IOException()
        diagnosticsTracker.trackEventInCurrentThread(testDiagnosticsEntry)
    }

    @Test
    fun `trackHttpRequestPerformed tracks correct event when coming from cache`() {
        val expectedProperties = mapOf(
            "endpoint_name" to "post_receipt",
            "response_time_millis" to 1234L,
            "successful" to true,
            "response_code" to 200,
            "etag_hit" to true,
            "verification_result" to "NOT_REQUESTED"
        )
        every { diagnosticsFileHelper.appendEntryToDiagnosticsFile(any()) } just Runs
        diagnosticsTracker.trackHttpRequestPerformed(
            Endpoint.PostReceipt,
            1234L.milliseconds,
            true,
            200,
            HTTPResult.Origin.CACHE,
            VerificationResult.NOT_REQUESTED
        )
        verify(exactly = 1) {
            diagnosticsFileHelper.appendEntryToDiagnosticsFile(match { event ->
                event is DiagnosticsEntry.Event &&
                    event.name == DiagnosticsEventName.HTTP_REQUEST_PERFORMED &&
                    event.properties == expectedProperties
            })
        }
    }

    @Test
    fun `trackHttpRequestPerformed tracks correct event when coming from backend`() {
        val expectedProperties = mapOf(
            "endpoint_name" to "get_offerings",
            "response_time_millis" to 1234L,
            "successful" to true,
            "response_code" to 200,
            "etag_hit" to false,
            "verification_result" to "NOT_REQUESTED"
        )
        every { diagnosticsFileHelper.appendEntryToDiagnosticsFile(any()) } just Runs
        diagnosticsTracker.trackHttpRequestPerformed(
            Endpoint.GetOfferings("test id"),
            1234L.milliseconds,
            true,
            200,
            HTTPResult.Origin.BACKEND,
            VerificationResult.NOT_REQUESTED
        )
        verify(exactly = 1) {
            diagnosticsFileHelper.appendEntryToDiagnosticsFile(match { event ->
                event is DiagnosticsEntry.Event &&
                    event.name == DiagnosticsEventName.HTTP_REQUEST_PERFORMED &&
                    event.properties == expectedProperties
            })
        }
    }

    @Test
    fun `trackHttpRequestPerformed tracks counter`() {
        val expectedProperties = mapOf(
            "endpoint_name" to "post_receipt",
            "successful" to "true",
            "response_code" to "200",
            "etag_hit" to "true",
            "verification_result" to "NOT_REQUESTED"
        )
        every { diagnosticsFileHelper.appendEntryToDiagnosticsFile(any()) } just Runs
        diagnosticsTracker.trackHttpRequestPerformed(
            Endpoint.PostReceipt,
            1234L.milliseconds,
            true,
            200,
            HTTPResult.Origin.CACHE,
            VerificationResult.NOT_REQUESTED
        )
        verify(exactly = 1) {
            diagnosticsFileHelper.appendEntryToDiagnosticsFile(match { event ->
                event is DiagnosticsEntry.Counter &&
                    event.name == DiagnosticsCounterName.HTTP_REQUEST_PERFORMED &&
                    event.tags == expectedProperties &&
                    event.value == 1
            })
        }
    }

    @Test
    fun `trackMaxEventsStoredLimitReached tracks correct event`() {
        every { diagnosticsFileHelper.appendEntryToDiagnosticsFile(any()) } just Runs
        diagnosticsTracker.trackMaxEventsStoredLimitReached()
        verify(exactly = 1) {
            diagnosticsFileHelper.appendEntryToDiagnosticsFile(match { event ->
                event is DiagnosticsEntry.Event &&
                    event.name == DiagnosticsEventName.MAX_EVENTS_STORED_LIMIT_REACHED &&
                    event.properties == emptyMap<String, Any>()
            })
        }
    }

    @Test
    fun `trackGoogleQueryProductDetailsRequest tracks correct event`() {
        val expectedProperties = mapOf(
            "product_type_queried" to "subs",
            "billing_response_code" to 12,
            "billing_debug_message" to "test-debug-message",
            "response_time_millis" to 1234L
        )
        every { diagnosticsFileHelper.appendEntryToDiagnosticsFile(any()) } just Runs
        diagnosticsTracker.trackGoogleQueryProductDetailsRequest(
            productType = "subs",
            billingResponseCode = 12,
            billingDebugMessage = "test-debug-message",
            responseTime = 1234L.milliseconds
        )
        verify(exactly = 1) {
            diagnosticsFileHelper.appendEntryToDiagnosticsFile(match { event ->
                event is DiagnosticsEntry.Event &&
                    event.name == DiagnosticsEventName.GOOGLE_QUERY_PRODUCT_DETAILS_REQUEST &&
                    event.properties == expectedProperties
            })
        }
    }

    @Test
    fun `trackGoogleQueryPurchasesRequest tracks correct event`() {
        val expectedProperties = mapOf(
            "product_type_queried" to "subs",
            "billing_response_code" to 12,
            "billing_debug_message" to "test-debug-message",
            "response_time_millis" to 1234L
        )
        every { diagnosticsFileHelper.appendEntryToDiagnosticsFile(any()) } just Runs
        diagnosticsTracker.trackGoogleQueryPurchasesRequest(
            productType = "subs",
            billingResponseCode = 12,
            billingDebugMessage = "test-debug-message",
            responseTime = 1234L.milliseconds
        )
        verify(exactly = 1) {
            diagnosticsFileHelper.appendEntryToDiagnosticsFile(match { event ->
                event is DiagnosticsEntry.Event &&
                    event.name == DiagnosticsEventName.GOOGLE_QUERY_PURCHASES_REQUEST &&
                    event.properties == expectedProperties
            })
        }
    }

    @Test
    fun `trackGoogleQueryPurchaseHistoryRequest tracks correct event`() {
        val expectedProperties = mapOf(
            "product_type_queried" to "inapp",
            "billing_response_code" to 12,
            "billing_debug_message" to "test-debug-message",
            "response_time_millis" to 1234L
        )
        every { diagnosticsFileHelper.appendEntryToDiagnosticsFile(any()) } just Runs
        diagnosticsTracker.trackGoogleQueryPurchaseHistoryRequest(
            productType = "inapp",
            billingResponseCode = 12,
            billingDebugMessage = "test-debug-message",
            responseTime = 1234L.milliseconds
        )
        verify(exactly = 1) {
            diagnosticsFileHelper.appendEntryToDiagnosticsFile(match { event ->
                event is DiagnosticsEntry.Event &&
                    event.name == DiagnosticsEventName.GOOGLE_QUERY_PURCHASE_HISTORY_REQUEST &&
                    event.properties == expectedProperties
            })
        }
    }

    @Test
    fun `trackFeatureNotSupported tracks correct event`() {
        val expectedProperties = mapOf(
            "play_store_version" to "123",
            "play_services_version" to "456",
            "billing_response_code" to "-2",
            "billing_debug_message" to "debug message",
        )
        every { diagnosticsFileHelper.appendEntryToDiagnosticsFile(any()) } just Runs
        diagnosticsTracker.trackProductDetailsNotSupported(
            billingResponseCode = -2,
            billingDebugMessage = "debug message"
        )
        verify(exactly = 1) {
            diagnosticsFileHelper.appendEntryToDiagnosticsFile(match { event ->
                event is DiagnosticsEntry.Counter &&
                    event.name == DiagnosticsCounterName.PRODUCT_DETAILS_NOT_SUPPORTED &&
                    event.tags == expectedProperties
            })
        }
    }
}
