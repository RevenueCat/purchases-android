package com.revenuecat.purchases.common.telemetry

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

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class TelemetryManagerTest {

    private val testTelemetryEvent = TelemetryEvent.Metric(
        MetricEventName.ETAG_HIT_RATE,
        emptyList(),
        1
    )

    private lateinit var telemetryFileHelper: TelemetryFileHelper
    private lateinit var telemetryAnonymizer: TelemetryAnonymizer
    private lateinit var backend: Backend
    private lateinit var dispatcher: Dispatcher

    private lateinit var telemetryManager: TelemetryManager

    @Before
    fun setup() {
        telemetryFileHelper = mockk()
        telemetryAnonymizer = mockk()
        backend = mockk()
        dispatcher = SyncDispatcher()

        telemetryManager = createTelemetryManager()
    }

    // region syncTelemetryFileIfNeeded

    @Test
    fun `syncTelemetryFileIfNeeded does not do anything if telemetry disabled`() {
        telemetryManager = createTelemetryManager(false)

        telemetryManager.syncTelemetryFileIfNeeded()

        verify(exactly = 0) { telemetryFileHelper.telemetryFileIsEmpty() }
        verify(exactly = 0) { telemetryFileHelper.readTelemetryFile() }
        verify(exactly = 0) { backend.postTelemetry(any(), any(), any()) }
    }

    @Test
    fun `syncTelemetryFileIfNeeded does not do anything if telemetry file is empty`() {
        every { telemetryFileHelper.telemetryFileIsEmpty() } returns true

        telemetryManager.syncTelemetryFileIfNeeded()

        verify(exactly = 1) { telemetryFileHelper.telemetryFileIsEmpty() }
        verify(exactly = 0) { telemetryFileHelper.readTelemetryFile() }
        verify(exactly = 0) { backend.postTelemetry(any(), any(), any()) }
    }

    @Test
    fun `syncTelemetryFileIfNeeded calls backend with correct parameters if file has contents`() {
        val telemetryEvents = listOf(JSONObject(mapOf("test-key" to "test-value")))
        every { telemetryFileHelper.telemetryFileIsEmpty() } returns false
        every { telemetryFileHelper.readTelemetryFile() } returns telemetryEvents
        mockBackendResponse(telemetryEvents)

        telemetryManager.syncTelemetryFileIfNeeded()

        verify(exactly = 1) { telemetryFileHelper.telemetryFileIsEmpty() }
        verify(exactly = 1) { telemetryFileHelper.readTelemetryFile() }
        verify(exactly = 1) { backend.postTelemetry(telemetryEvents, any(), any()) }
    }

    @Test
    fun `syncTelemetryFileIfNeeded cleans sent events if backend request successful`() {
        val telemetryEvents = listOf(
            JSONObject(mapOf("test-key" to "test-value")),
            JSONObject(mapOf("test-key-2" to "test-value-2"))
        )
        val telemetryEventsSize = telemetryEvents.size
        every { telemetryFileHelper.telemetryFileIsEmpty() } returns false
        every { telemetryFileHelper.readTelemetryFile() } returns telemetryEvents
        mockBackendResponse(telemetryEvents, successReturn = JSONObject())
        every { telemetryFileHelper.cleanSentTelemetry(telemetryEventsSize) } just Runs

        telemetryManager.syncTelemetryFileIfNeeded()

        verify(exactly = 1) { telemetryFileHelper.cleanSentTelemetry(telemetryEventsSize)  }
    }

    @Test
    fun `syncTelemetryFileIfNeeded deletes file if backend request unsuccessful`() {
        val telemetryEvents = listOf(
            JSONObject(mapOf("test-key" to "test-value")),
            JSONObject(mapOf("test-key-2" to "test-value-2"))
        )
        every { telemetryFileHelper.telemetryFileIsEmpty() } returns false
        every { telemetryFileHelper.readTelemetryFile() } returns telemetryEvents
        mockBackendResponse(telemetryEvents, errorReturn = PurchasesError(PurchasesErrorCode.ConfigurationError))
        every { telemetryFileHelper.deleteTelemetryFile() } just Runs

        telemetryManager.syncTelemetryFileIfNeeded()

        verify(exactly = 1) { telemetryFileHelper.deleteTelemetryFile()  }
    }

    // endregion

    // region trackEvent

    @Test
    fun `trackEvent does nothing if telemetry disabled`() {
        telemetryManager = createTelemetryManager(false)
        telemetryManager.trackEvent(testTelemetryEvent)
        verify(exactly = 0) { telemetryFileHelper.appendEventToTelemetryFile(any()) }
        verify(exactly = 0) { telemetryAnonymizer.anonymizeEventIfNeeded(any()) }
    }

    @Test
    fun `trackEvent performs correct calls`() {
        every { telemetryFileHelper.appendEventToTelemetryFile(testTelemetryEvent) } just Runs
        every { telemetryAnonymizer.anonymizeEventIfNeeded(testTelemetryEvent) } returns testTelemetryEvent
        telemetryManager.trackEvent(testTelemetryEvent)
        verify(exactly = 1) { telemetryFileHelper.appendEventToTelemetryFile(testTelemetryEvent) }
        verify(exactly = 1) { telemetryAnonymizer.anonymizeEventIfNeeded(testTelemetryEvent) }
    }

    // endregion

    private fun mockBackendResponse(
        telemetryEvents: List<JSONObject>,
        successReturn: JSONObject? = null,
        errorReturn: PurchasesError? = null
    ) {
        val successCallbackSlot = slot<(JSONObject) -> Unit>()
        val errorCallbackSlot = slot<(PurchasesError) -> Unit>()
        every { backend.postTelemetry(
            telemetryEvents,
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

    private fun createTelemetryManager(telemetryEnabled: Boolean = true): TelemetryManager {
        return TelemetryManager(
            telemetryFileHelper,
            telemetryAnonymizer,
            backend,
            dispatcher,
            telemetryEnabled
        )
    }
}
