package com.revenuecat.purchases.common.diagnostics

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.SyncDispatcher
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class DiagnosticsManagerTest {

    private val testDiagnosticsEvent = DiagnosticsEvent.Metric(
        MetricEventName.ETAG_HIT_RATE,
        emptyList(),
        1
    )

    private lateinit var diagnosticsFileHelper: DiagnosticsFileHelper
    private lateinit var diagnosticsAnonymizer: DiagnosticsAnonymizer
    private lateinit var backend: Backend
    private lateinit var dispatcher: Dispatcher

    private lateinit var diagnosticsManager: DiagnosticsManager

    @Before
    fun setup() {
        diagnosticsFileHelper = mockk()
        diagnosticsAnonymizer = mockk()
        backend = mockk()
        dispatcher = SyncDispatcher()

        diagnosticsManager = DiagnosticsManager(
            diagnosticsFileHelper,
            diagnosticsAnonymizer,
            backend,
            dispatcher
        )
    }

    // region syncDiagnosticsFileIfNeeded

    @Test
    fun `syncDiagnosticsFileIfNeeded does not do anything if diagnostics file is empty`() {
        every { diagnosticsFileHelper.diagnosticsFileIsEmpty() } returns true

        diagnosticsManager.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) { diagnosticsFileHelper.diagnosticsFileIsEmpty() }
        verify(exactly = 0) { diagnosticsFileHelper.readDiagnosticsFile() }
        verify(exactly = 0) { backend.postDiagnostics(any(), any(), any()) }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded calls backend with correct parameters if file has contents`() {
        val diagnosticsEvents = listOf(JSONObject(mapOf("test-key" to "test-value")))
        every { diagnosticsFileHelper.diagnosticsFileIsEmpty() } returns false
        every { diagnosticsFileHelper.readDiagnosticsFile() } returns diagnosticsEvents
        mockBackendResponse(diagnosticsEvents)

        diagnosticsManager.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) { diagnosticsFileHelper.diagnosticsFileIsEmpty() }
        verify(exactly = 1) { diagnosticsFileHelper.readDiagnosticsFile() }
        verify(exactly = 1) { backend.postDiagnostics(diagnosticsEvents, any(), any()) }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded cleans sent events if backend request successful`() {
        val diagnosticEvents = listOf(
            JSONObject(mapOf("test-key" to "test-value")),
            JSONObject(mapOf("test-key-2" to "test-value-2"))
        )
        val diagnosticsEventsSize = diagnosticEvents.size
        every { diagnosticsFileHelper.diagnosticsFileIsEmpty() } returns false
        every { diagnosticsFileHelper.readDiagnosticsFile() } returns diagnosticEvents
        mockBackendResponse(diagnosticEvents, successReturn = JSONObject())
        every { diagnosticsFileHelper.cleanSentDiagnostics(diagnosticsEventsSize) } just Runs

        diagnosticsManager.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) { diagnosticsFileHelper.cleanSentDiagnostics(diagnosticsEventsSize)  }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded deletes file if backend request unsuccessful`() {
        val diagnosticsEvents = listOf(
            JSONObject(mapOf("test-key" to "test-value")),
            JSONObject(mapOf("test-key-2" to "test-value-2"))
        )
        every { diagnosticsFileHelper.diagnosticsFileIsEmpty() } returns false
        every { diagnosticsFileHelper.readDiagnosticsFile() } returns diagnosticsEvents
        mockBackendResponse(diagnosticsEvents, errorReturn = PurchasesError(PurchasesErrorCode.ConfigurationError))
        every { diagnosticsFileHelper.deleteDiagnosticsFile() } just Runs

        diagnosticsManager.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) { diagnosticsFileHelper.deleteDiagnosticsFile()  }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded deletes file if IOException happens`() {
        every { diagnosticsFileHelper.diagnosticsFileIsEmpty() } returns false
        every { diagnosticsFileHelper.readDiagnosticsFile() } throws IOException()
        every { diagnosticsFileHelper.deleteDiagnosticsFile() } just Runs
        diagnosticsManager.syncDiagnosticsFileIfNeeded()
        verify(exactly = 1) { diagnosticsFileHelper.deleteDiagnosticsFile()  }
    }

    // endregion

    // region trackEvent

    @Test
    fun `trackEvent performs correct calls`() {
        every { diagnosticsFileHelper.appendEventToDiagnosticsFile(testDiagnosticsEvent) } just Runs
        every { diagnosticsAnonymizer.anonymizeEventIfNeeded(testDiagnosticsEvent) } returns testDiagnosticsEvent
        diagnosticsManager.trackEvent(testDiagnosticsEvent)
        verify(exactly = 1) { diagnosticsFileHelper.appendEventToDiagnosticsFile(testDiagnosticsEvent) }
        verify(exactly = 1) { diagnosticsAnonymizer.anonymizeEventIfNeeded(testDiagnosticsEvent) }
    }

    @Test
    fun `trackEvent handles IOException`() {
        every { diagnosticsAnonymizer.anonymizeEventIfNeeded(testDiagnosticsEvent) } returns testDiagnosticsEvent
        every { diagnosticsFileHelper.appendEventToDiagnosticsFile(any()) } throws IOException()
        diagnosticsManager.trackEvent(testDiagnosticsEvent)
    }

    // endregion

    private fun mockBackendResponse(
        diagnosticsEvents: List<JSONObject>,
        successReturn: JSONObject? = null,
        errorReturn: PurchasesError? = null
    ) {
        val successCallbackSlot = slot<(JSONObject) -> Unit>()
        val errorCallbackSlot = slot<(PurchasesError) -> Unit>()
        every { backend.postDiagnostics(
            diagnosticsEvents,
            capture(successCallbackSlot),
            capture(errorCallbackSlot)
        ) } answers {
            if (successReturn != null) {
                successCallbackSlot.captured(successReturn)
            } else if (errorReturn != null) {
                errorCallbackSlot.captured(errorReturn)
            }
        }
    }
}
