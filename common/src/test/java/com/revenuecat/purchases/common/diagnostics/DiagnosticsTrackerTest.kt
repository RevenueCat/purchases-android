package com.revenuecat.purchases.common.diagnostics

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.SyncDispatcher
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
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

    @Before
    fun setup() {
        diagnosticsFileHelper = mockk()
        diagnosticsAnonymizer = mockk()
        dispatcher = SyncDispatcher()

        diagnosticsTracker = DiagnosticsTracker(
            diagnosticsFileHelper,
            diagnosticsAnonymizer,
            dispatcher
        )

        every { diagnosticsAnonymizer.anonymizeEntryIfNeeded(any()) } answers { firstArg() }
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
            "etag_hit" to true
        )
        every { diagnosticsFileHelper.appendEntryToDiagnosticsFile(any()) } just Runs
        diagnosticsTracker.trackHttpRequestPerformed(Endpoint.PostReceipt, 1234L.milliseconds, true, 200, HTTPResult.Origin.CACHE)
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
            "etag_hit" to false
        )
        every { diagnosticsFileHelper.appendEntryToDiagnosticsFile(any()) } just Runs
        diagnosticsTracker.trackHttpRequestPerformed(Endpoint.GetOfferings("test id"), 1234L.milliseconds, true, 200, HTTPResult.Origin.BACKEND)
        verify(exactly = 1) {
            diagnosticsFileHelper.appendEntryToDiagnosticsFile(match { event ->
                event is DiagnosticsEntry.Event &&
                    event.name == DiagnosticsEventName.HTTP_REQUEST_PERFORMED &&
                    event.properties == expectedProperties
            })
        }
    }

    @Test
    fun `trackMaxEventsStoredLimitReached tracks correct event`() {
        val expectedProperties = mapOf(
            "total_number_events_stored" to 1234,
            "events_removed" to 234
        )
        every { diagnosticsFileHelper.appendEntryToDiagnosticsFile(any()) } just Runs
        diagnosticsTracker.trackMaxEventsStoredLimitReached(totalEventsStored = 1234, eventsRemoved = 234)
        verify(exactly = 1) {
            diagnosticsFileHelper.appendEntryToDiagnosticsFile(match { event ->
                event is DiagnosticsEntry.Event &&
                    event.name == DiagnosticsEventName.MAX_EVENTS_STORED_LIMIT_REACHED &&
                    event.properties == expectedProperties
            })
        }
    }

    @Test
    fun `trackGoogleQuerySkuDetailsRequest tracks correct event`() {
        val expectedProperties = mapOf(
            "sku_type_queried" to "subs",
            "billing_response_code" to 12,
            "billing_debug_message" to "test-debug-message",
            "response_time_millis" to 1234L
        )
        every { diagnosticsFileHelper.appendEntryToDiagnosticsFile(any()) } just Runs
        diagnosticsTracker.trackGoogleQuerySkuDetailsRequest(
            skuType = "subs",
            billingResponseCode = 12,
            billingDebugMessage = "test-debug-message",
            responseTime = 1234L.milliseconds
        )
        verify(exactly = 1) {
            diagnosticsFileHelper.appendEntryToDiagnosticsFile(match { event ->
                event is DiagnosticsEntry.Event &&
                    event.name == DiagnosticsEventName.GOOGLE_QUERY_SKU_DETAILS_REQUEST &&
                    event.properties == expectedProperties
            })
        }
    }

    @Test
    fun `trackGoogleQueryPurchasesRequest tracks correct event`() {
        val expectedProperties = mapOf(
            "sku_type_queried" to "subs",
            "billing_response_code" to 12,
            "billing_debug_message" to "test-debug-message",
            "response_time_millis" to 1234L
        )
        every { diagnosticsFileHelper.appendEntryToDiagnosticsFile(any()) } just Runs
        diagnosticsTracker.trackGoogleQueryPurchasesRequest(
            skuType = "subs",
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
            "billing_response_code" to 12,
            "billing_debug_message" to "test-debug-message",
            "response_time_millis" to 1234L
        )
        every { diagnosticsFileHelper.appendEntryToDiagnosticsFile(any()) } just Runs
        diagnosticsTracker.trackGoogleQueryPurchaseHistoryRequest(
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
}
