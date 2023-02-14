package com.revenuecat.purchases.common.diagnostics

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.SyncDispatcher
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.ResultOrigin
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

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class DiagnosticsTrackerTest {

    private val testDiagnosticsEvent = DiagnosticsEvent.Log(
        name = DiagnosticsLogEventName.ENDPOINT_HIT,
        properties = mapOf("test-key-1" to "test-value-1")
    )

    private val testAnonymizedEvent = DiagnosticsEvent.Log(
        name = DiagnosticsLogEventName.ENDPOINT_HIT,
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

        every { diagnosticsAnonymizer.anonymizeEventIfNeeded(any()) } answers { firstArg() }
    }

    @Test
    fun `trackEvent performs correct calls`() {
        every { diagnosticsFileHelper.appendEventToDiagnosticsFile(testAnonymizedEvent) } just Runs
        every { diagnosticsAnonymizer.anonymizeEventIfNeeded(testDiagnosticsEvent) } returns testAnonymizedEvent
        diagnosticsTracker.trackEvent(testDiagnosticsEvent)
        verify(exactly = 1) { diagnosticsFileHelper.appendEventToDiagnosticsFile(testAnonymizedEvent) }
        verify(exactly = 1) { diagnosticsAnonymizer.anonymizeEventIfNeeded(testDiagnosticsEvent) }
    }

    @Test
    fun `trackEvent handles IOException`() {
        every { diagnosticsAnonymizer.anonymizeEventIfNeeded(testDiagnosticsEvent) } returns testDiagnosticsEvent
        every { diagnosticsFileHelper.appendEventToDiagnosticsFile(any()) } throws IOException()
        diagnosticsTracker.trackEvent(testDiagnosticsEvent)
    }

    @Test
    fun `trackEventInCurrentThread does not enqueue request`() {
        dispatcher.close()
        every { diagnosticsAnonymizer.anonymizeEventIfNeeded(testDiagnosticsEvent) } returns testDiagnosticsEvent
        every { diagnosticsFileHelper.appendEventToDiagnosticsFile(any()) } throws IOException()
        diagnosticsTracker.trackEventInCurrentThread(testDiagnosticsEvent)
    }

    @Test
    fun `trackEndpointHit tracks correct event when coming from cache`() {
        val expectedProperties = mapOf(
            "endpoint_name" to "post_receipt",
            "response_time_millis" to 1234L,
            "successful" to true,
            "response_code" to 200,
            "etag_hit" to true
        )
        every { diagnosticsFileHelper.appendEventToDiagnosticsFile(any()) } just Runs
        diagnosticsTracker.trackEndpointHit(Endpoint.PostReceipt, 1234, true, 200, ResultOrigin.CACHE)
        verify(exactly = 1) {
            diagnosticsFileHelper.appendEventToDiagnosticsFile(match { event ->
                event is DiagnosticsEvent.Log &&
                    event.name == DiagnosticsLogEventName.ENDPOINT_HIT &&
                    event.properties == expectedProperties
            })
        }
    }

    @Test
    fun `trackEndpointHit tracks correct event when coming from backend`() {
        val expectedProperties = mapOf(
            "endpoint_name" to "get_offerings",
            "response_time_millis" to 1234L,
            "successful" to true,
            "response_code" to 200,
            "etag_hit" to false
        )
        every { diagnosticsFileHelper.appendEventToDiagnosticsFile(any()) } just Runs
        diagnosticsTracker.trackEndpointHit(Endpoint.GetOfferings("test id"), 1234, true, 200, ResultOrigin.BACKEND)
        verify(exactly = 1) {
            diagnosticsFileHelper.appendEventToDiagnosticsFile(match { event ->
                event is DiagnosticsEvent.Log &&
                    event.name == DiagnosticsLogEventName.ENDPOINT_HIT &&
                    event.properties == expectedProperties
            })
        }
    }
}
