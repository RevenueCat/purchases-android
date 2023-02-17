package com.revenuecat.purchases.common.diagnostics

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.SyncDispatcher
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
}
