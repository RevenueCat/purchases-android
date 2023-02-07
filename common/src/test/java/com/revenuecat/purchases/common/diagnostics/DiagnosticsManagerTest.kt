package com.revenuecat.purchases.common.diagnostics

import android.content.SharedPreferences
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

    private val testDiagnosticsEventJSONs = listOf(
        JSONObject(mapOf("test-key" to "test-value")),
        JSONObject(mapOf("test-key-2" to "test-value-2"))
    )

    private lateinit var diagnosticsFileHelper: DiagnosticsFileHelper
    private lateinit var diagnosticsAnonymizer: DiagnosticsAnonymizer
    private lateinit var backend: Backend
    private lateinit var dispatcher: Dispatcher
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sharedPreferencesEditor: SharedPreferences.Editor

    private lateinit var diagnosticsManager: DiagnosticsManager

    @Before
    fun setup() {
        diagnosticsFileHelper = mockk()
        diagnosticsAnonymizer = mockk()
        backend = mockk()
        dispatcher = SyncDispatcher()

        mockSharedPreferences()
        mockDiagnosticsFileHelper()

        diagnosticsManager = DiagnosticsManager(
            diagnosticsFileHelper,
            diagnosticsAnonymizer,
            backend,
            dispatcher,
            sharedPreferences
        )
    }

    // region syncDiagnosticsFileIfNeeded

    @Test
    fun `syncDiagnosticsFileIfNeeded does not do anything if diagnostics file is empty`() {
        every { diagnosticsFileHelper.readDiagnosticsFile() } returns emptyList()

        diagnosticsManager.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) { diagnosticsFileHelper.readDiagnosticsFile() }
        verify(exactly = 0) { backend.postDiagnostics(any(), any(), any()) }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded calls backend with correct parameters if file has contents`() {
        mockBackendResponse(testDiagnosticsEventJSONs)

        diagnosticsManager.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) { diagnosticsFileHelper.readDiagnosticsFile() }
        verify(exactly = 1) { backend.postDiagnostics(testDiagnosticsEventJSONs, any(), any()) }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded cleans sent events if backend request successful`() {
        mockBackendResponse(testDiagnosticsEventJSONs, successReturn = JSONObject())

        diagnosticsManager.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) { diagnosticsFileHelper.cleanSentDiagnostics(testDiagnosticsEventJSONs.size) }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded removes consecutive failures count if request successful`() {
        mockBackendResponse(testDiagnosticsEventJSONs, successReturn = JSONObject())

        diagnosticsManager.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) { sharedPreferencesEditor.remove(DiagnosticsManager.CONSECUTIVE_FAILURES_COUNT_KEY) }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded increases consecutive errors count if backend request unsuccessful`() {
        val errorCallbackResponse = Pair(PurchasesError(PurchasesErrorCode.ConfigurationError), true)
        mockBackendResponse(testDiagnosticsEventJSONs, errorReturn = errorCallbackResponse)

        diagnosticsManager.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) {
            sharedPreferencesEditor.putInt(DiagnosticsManager.CONSECUTIVE_FAILURES_COUNT_KEY, 1)
        }
        verify(exactly = 0) { diagnosticsFileHelper.deleteDiagnosticsFile() }
        verify(exactly = 0) { sharedPreferencesEditor.remove(DiagnosticsManager.CONSECUTIVE_FAILURES_COUNT_KEY) }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded does not delete file if backend request unsuccessful once`() {
        val errorCallbackResponse = Pair(PurchasesError(PurchasesErrorCode.ConfigurationError), true)
        mockBackendResponse(testDiagnosticsEventJSONs, errorReturn = errorCallbackResponse)

        diagnosticsManager.syncDiagnosticsFileIfNeeded()

        verify(exactly = 0) { diagnosticsFileHelper.deleteDiagnosticsFile() }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded deletes file if backend request unsuccessful and last retry`() {
        val errorCallbackResponse = Pair(PurchasesError(PurchasesErrorCode.ConfigurationError), true)
        mockBackendResponse(testDiagnosticsEventJSONs, errorReturn = errorCallbackResponse)
        every {
            sharedPreferences.getInt(DiagnosticsManager.CONSECUTIVE_FAILURES_COUNT_KEY, 0)
        } returns DiagnosticsManager.MAX_NUMBER_RETRIES - 1

        diagnosticsManager.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) { diagnosticsFileHelper.deleteDiagnosticsFile() }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded removes consecutive failures count if request unsuccessful and last retry`() {
        val errorCallbackResponse = Pair(PurchasesError(PurchasesErrorCode.ConfigurationError), true)
        mockBackendResponse(testDiagnosticsEventJSONs, errorReturn = errorCallbackResponse)
        every {
            sharedPreferences.getInt(DiagnosticsManager.CONSECUTIVE_FAILURES_COUNT_KEY, 0)
        } returns DiagnosticsManager.MAX_NUMBER_RETRIES - 1

        diagnosticsManager.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) { sharedPreferencesEditor.remove(DiagnosticsManager.CONSECUTIVE_FAILURES_COUNT_KEY) }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded removes file if should not retry`() {
        val errorCallbackResponse = Pair(PurchasesError(PurchasesErrorCode.ConfigurationError), false)
        mockBackendResponse(testDiagnosticsEventJSONs, errorReturn = errorCallbackResponse)

        diagnosticsManager.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) { diagnosticsFileHelper.deleteDiagnosticsFile() }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded removes consecutive failures count if should not retry`() {
        val errorCallbackResponse = Pair(PurchasesError(PurchasesErrorCode.ConfigurationError), false)
        mockBackendResponse(testDiagnosticsEventJSONs, errorReturn = errorCallbackResponse)

        diagnosticsManager.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) { sharedPreferencesEditor.remove(DiagnosticsManager.CONSECUTIVE_FAILURES_COUNT_KEY) }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded deletes file if IOException happens`() {
        every { diagnosticsFileHelper.readDiagnosticsFile() } throws IOException()
        diagnosticsManager.syncDiagnosticsFileIfNeeded()
        verify(exactly = 1) { diagnosticsFileHelper.deleteDiagnosticsFile()  }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded removes consecutive failures count if IOException happens`() {
        every { diagnosticsFileHelper.readDiagnosticsFile() } throws IOException()
        diagnosticsManager.syncDiagnosticsFileIfNeeded()
        verify(exactly = 1) { sharedPreferencesEditor.remove(DiagnosticsManager.CONSECUTIVE_FAILURES_COUNT_KEY) }
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

    private fun mockSharedPreferences() {
        sharedPreferences = mockk()
        sharedPreferencesEditor = mockk()
        every { sharedPreferences.edit() } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.apply() } just Runs
        every {
            sharedPreferencesEditor.remove(DiagnosticsManager.CONSECUTIVE_FAILURES_COUNT_KEY)
        } returns sharedPreferencesEditor
        every {
            sharedPreferencesEditor.putInt(DiagnosticsManager.CONSECUTIVE_FAILURES_COUNT_KEY, any())
        } returns sharedPreferencesEditor
        every {
            sharedPreferences.getInt(DiagnosticsManager.CONSECUTIVE_FAILURES_COUNT_KEY, 0)
        } returns 0
    }

    private fun mockDiagnosticsFileHelper() {
        diagnosticsFileHelper = mockk()
        every { diagnosticsFileHelper.readDiagnosticsFile() } returns testDiagnosticsEventJSONs
        every { diagnosticsFileHelper.deleteDiagnosticsFile() } just Runs
        every { diagnosticsFileHelper.cleanSentDiagnostics(testDiagnosticsEventJSONs.size) } just Runs
    }

    private fun mockBackendResponse(
        diagnosticsEvents: List<JSONObject>,
        successReturn: JSONObject? = null,
        errorReturn: Pair<PurchasesError, Boolean>? = null
    ) {
        val successCallbackSlot = slot<(JSONObject) -> Unit>()
        val errorCallbackSlot = slot<(PurchasesError, Boolean) -> Unit>()
        every { backend.postDiagnostics(
            diagnosticsEvents,
            capture(successCallbackSlot),
            capture(errorCallbackSlot)
        ) } answers {
            if (successReturn != null) {
                successCallbackSlot.captured(successReturn)
            } else if (errorReturn != null) {
                errorCallbackSlot.captured(errorReturn.first, errorReturn.second)
            }
        }
    }
}
