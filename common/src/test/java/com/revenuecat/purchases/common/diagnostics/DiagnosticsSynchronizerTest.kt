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
import org.json.JSONException
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class DiagnosticsSynchronizerTest {

    private val testDiagnosticsEntryJSONs = listOf(
        JSONObject(mapOf("test-key" to "test-value")),
        JSONObject(mapOf("test-key-2" to "test-value-2"))
    )

    private lateinit var diagnosticsFileHelper: DiagnosticsFileHelper
    private lateinit var diagnosticsTracker: DiagnosticsTracker
    private lateinit var backend: Backend
    private lateinit var dispatcher: Dispatcher
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sharedPreferencesEditor: SharedPreferences.Editor

    private lateinit var diagnosticsSynchronizer: DiagnosticsSynchronizer

    @Before
    fun setup() {
        diagnosticsFileHelper = mockk()
        diagnosticsTracker = mockk()
        backend = mockk()
        dispatcher = SyncDispatcher()

        mockSharedPreferences()
        mockDiagnosticsFileHelper()

        diagnosticsSynchronizer = DiagnosticsSynchronizer(
            diagnosticsFileHelper,
            diagnosticsTracker,
            backend,
            dispatcher,
            sharedPreferences
        )
    }

    // region syncDiagnosticsFileIfNeeded

    @Test
    fun `syncDiagnosticsFileIfNeeded does not do anything if diagnostics file is empty`() {
        every { diagnosticsFileHelper.readDiagnosticsFile() } returns emptyList()

        diagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) { diagnosticsFileHelper.readDiagnosticsFile() }
        verify(exactly = 0) { backend.postDiagnostics(any(), any(), any()) }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded calls backend with correct parameters if file has contents`() {
        mockBackendResponse(testDiagnosticsEntryJSONs)

        diagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) { diagnosticsFileHelper.readDiagnosticsFile() }
        verify(exactly = 1) { backend.postDiagnostics(testDiagnosticsEntryJSONs, any(), any()) }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded cleans sent events if backend request successful`() {
        mockBackendResponse(testDiagnosticsEntryJSONs, successReturn = JSONObject())

        diagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) { diagnosticsFileHelper.deleteOlderDiagnostics(testDiagnosticsEntryJSONs.size) }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded removes consecutive failures count if request successful`() {
        mockBackendResponse(testDiagnosticsEntryJSONs, successReturn = JSONObject())

        diagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) { sharedPreferencesEditor.remove(DiagnosticsSynchronizer.CONSECUTIVE_FAILURES_COUNT_KEY) }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded increases consecutive errors count if backend request unsuccessful`() {
        val errorCallbackResponse = Pair(PurchasesError(PurchasesErrorCode.ConfigurationError), true)
        mockBackendResponse(testDiagnosticsEntryJSONs, errorReturn = errorCallbackResponse)

        diagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) {
            sharedPreferencesEditor.putInt(DiagnosticsSynchronizer.CONSECUTIVE_FAILURES_COUNT_KEY, 1)
        }
        verify(exactly = 0) { diagnosticsFileHelper.deleteDiagnosticsFile() }
        verify(exactly = 0) { sharedPreferencesEditor.remove(DiagnosticsSynchronizer.CONSECUTIVE_FAILURES_COUNT_KEY) }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded does not delete file if backend request unsuccessful once`() {
        val errorCallbackResponse = Pair(PurchasesError(PurchasesErrorCode.ConfigurationError), true)
        mockBackendResponse(testDiagnosticsEntryJSONs, errorReturn = errorCallbackResponse)

        diagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()

        verify(exactly = 0) { diagnosticsFileHelper.deleteDiagnosticsFile() }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded deletes file if backend request unsuccessful and last retry`() {
        val errorCallbackResponse = Pair(PurchasesError(PurchasesErrorCode.ConfigurationError), true)
        mockBackendResponse(testDiagnosticsEntryJSONs, errorReturn = errorCallbackResponse)
        every {
            sharedPreferences.getInt(DiagnosticsSynchronizer.CONSECUTIVE_FAILURES_COUNT_KEY, 0)
        } returns DiagnosticsSynchronizer.MAX_NUMBER_POST_RETRIES - 1

        diagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) { diagnosticsFileHelper.deleteDiagnosticsFile() }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded removes consecutive failures count if request unsuccessful and last retry`() {
        val errorCallbackResponse = Pair(PurchasesError(PurchasesErrorCode.ConfigurationError), true)
        mockBackendResponse(testDiagnosticsEntryJSONs, errorReturn = errorCallbackResponse)
        every {
            sharedPreferences.getInt(DiagnosticsSynchronizer.CONSECUTIVE_FAILURES_COUNT_KEY, 0)
        } returns DiagnosticsSynchronizer.MAX_NUMBER_POST_RETRIES - 1

        diagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) { sharedPreferencesEditor.remove(DiagnosticsSynchronizer.CONSECUTIVE_FAILURES_COUNT_KEY) }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded removes file if should not retry`() {
        val errorCallbackResponse = Pair(PurchasesError(PurchasesErrorCode.ConfigurationError), false)
        mockBackendResponse(testDiagnosticsEntryJSONs, errorReturn = errorCallbackResponse)

        diagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) { diagnosticsFileHelper.deleteDiagnosticsFile() }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded removes consecutive failures count if should not retry`() {
        val errorCallbackResponse = Pair(PurchasesError(PurchasesErrorCode.ConfigurationError), false)
        mockBackendResponse(testDiagnosticsEntryJSONs, errorReturn = errorCallbackResponse)

        diagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) { sharedPreferencesEditor.remove(DiagnosticsSynchronizer.CONSECUTIVE_FAILURES_COUNT_KEY) }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded deletes file if IOException happens`() {
        every { diagnosticsFileHelper.readDiagnosticsFile() } throws IOException()
        diagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()
        verify(exactly = 1) { diagnosticsFileHelper.deleteDiagnosticsFile()  }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded deletes file if JSONException happens`() {
        every { diagnosticsFileHelper.readDiagnosticsFile() } throws JSONException("test-exception")
        diagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()
        verify(exactly = 1) { diagnosticsFileHelper.deleteDiagnosticsFile()  }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded removes consecutive failures count if IOException happens`() {
        every { diagnosticsFileHelper.readDiagnosticsFile() } throws IOException()
        diagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()
        verify(exactly = 1) { sharedPreferencesEditor.remove(DiagnosticsSynchronizer.CONSECUTIVE_FAILURES_COUNT_KEY) }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded does not crash if IOException happens when deleting file`() {
        every { diagnosticsFileHelper.readDiagnosticsFile() } throws IOException()
        every { diagnosticsFileHelper.deleteDiagnosticsFile() } throws IOException()
        diagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded removes old events if exceeding limit`() {
        val eventsOverLimit = 10
        val eventsToRemove = eventsOverLimit + 1 // Leaves space for tracking event
        val eventsInFile = (0 until DiagnosticsSynchronizer.MAX_NUMBER_EVENTS + eventsOverLimit).map {
            JSONObject(mapOf("test-key-$it" to "value-$it"))
        }
        val eventsAfterRemovingOlder = eventsInFile.subList(eventsOverLimit, eventsInFile.size)
        every { diagnosticsFileHelper.readDiagnosticsFile() } returnsMany listOf(eventsInFile, eventsAfterRemovingOlder)
        every { diagnosticsTracker.trackEventInCurrentThread(any()) } just Runs
        every { diagnosticsFileHelper.deleteOlderDiagnostics(eventsToRemove) } just Runs
        every { diagnosticsTracker.trackMaxEventsStoredLimitReached(any(), any()) } just Runs
        mockBackendResponse(eventsAfterRemovingOlder)
        diagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()
        verify(exactly = 1) { diagnosticsFileHelper.deleteOlderDiagnostics(eventsToRemove) }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded tracks max elements stored reached if syncing more than limit`() {
        val eventsOverLimit = 10
        val eventsToRemove = eventsOverLimit + 1 // Leaves space for tracking event
        val totalNumberOfEventsInFile = DiagnosticsSynchronizer.MAX_NUMBER_EVENTS + eventsOverLimit
        val eventsInFile = (0 until totalNumberOfEventsInFile).map {
            JSONObject(mapOf("test-key-$it" to "value-$it"))
        }
        val eventsAfterRemovingOlder = eventsInFile.subList(eventsOverLimit, eventsInFile.size)
        every { diagnosticsFileHelper.readDiagnosticsFile() } returnsMany listOf(eventsInFile, eventsAfterRemovingOlder)
        every { diagnosticsFileHelper.deleteOlderDiagnostics(eventsToRemove) } just Runs
        every {
            diagnosticsTracker.trackMaxEventsStoredLimitReached(totalNumberOfEventsInFile, eventsToRemove)
        } just Runs
        mockBackendResponse(eventsAfterRemovingOlder)
        diagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()
        verify(exactly = 1) {
            diagnosticsTracker.trackMaxEventsStoredLimitReached(
                totalNumberOfEventsInFile,
                eventsToRemove,
                useCurrentThread = true
            )
        }
    }

    // endregion

    private fun mockSharedPreferences() {
        sharedPreferences = mockk()
        sharedPreferencesEditor = mockk()
        every { sharedPreferences.edit() } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.apply() } just Runs
        every {
            sharedPreferencesEditor.remove(DiagnosticsSynchronizer.CONSECUTIVE_FAILURES_COUNT_KEY)
        } returns sharedPreferencesEditor
        every {
            sharedPreferencesEditor.putInt(DiagnosticsSynchronizer.CONSECUTIVE_FAILURES_COUNT_KEY, any())
        } returns sharedPreferencesEditor
        every {
            sharedPreferences.getInt(DiagnosticsSynchronizer.CONSECUTIVE_FAILURES_COUNT_KEY, 0)
        } returns 0
    }

    private fun mockDiagnosticsFileHelper() {
        diagnosticsFileHelper = mockk()
        every { diagnosticsFileHelper.readDiagnosticsFile() } returns testDiagnosticsEntryJSONs
        every { diagnosticsFileHelper.deleteDiagnosticsFile() } just Runs
        every { diagnosticsFileHelper.deleteOlderDiagnostics(testDiagnosticsEntryJSONs.size) } just Runs
    }

    private fun mockBackendResponse(
        diagnosticsEntries: List<JSONObject>,
        successReturn: JSONObject? = null,
        errorReturn: Pair<PurchasesError, Boolean>? = null
    ) {
        val successCallbackSlot = slot<(JSONObject) -> Unit>()
        val errorCallbackSlot = slot<(PurchasesError, Boolean) -> Unit>()
        every { backend.postDiagnostics(
            diagnosticsEntries,
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
