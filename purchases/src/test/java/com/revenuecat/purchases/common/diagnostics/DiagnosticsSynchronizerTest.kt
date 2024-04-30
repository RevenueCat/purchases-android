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
import java.util.stream.Stream

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
            DiagnosticsHelper(mockk(), diagnosticsFileHelper, lazy { sharedPreferences }),
            diagnosticsFileHelper,
            diagnosticsTracker,
            backend,
            dispatcher,
        )
    }

    // region syncDiagnosticsFileIfNeeded

    @Test
    fun `syncDiagnosticsFileIfNeeded does not do anything if diagnostics file is empty`() {
        mockReadDiagnosticsFile(emptyList())

        diagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) { diagnosticsFileHelper.readFileAsJson(any()) }
        verify(exactly = 0) { backend.postDiagnostics(any(), any(), any()) }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded calls backend with correct parameters if file has contents`() {
        mockBackendResponse(testDiagnosticsEntryJSONs)

        diagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) { diagnosticsFileHelper.readFileAsJson(any()) }
        verify(exactly = 1) { backend.postDiagnostics(testDiagnosticsEntryJSONs, any(), any()) }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded cleans sent events if backend request successful`() {
        mockBackendResponse(testDiagnosticsEntryJSONs, successReturn = JSONObject())

        diagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) { diagnosticsFileHelper.clear(testDiagnosticsEntryJSONs.size) }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded removes consecutive failures count if request successful`() {
        mockBackendResponse(testDiagnosticsEntryJSONs, successReturn = JSONObject())

        diagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) { sharedPreferencesEditor.remove(DiagnosticsHelper.CONSECUTIVE_FAILURES_COUNT_KEY) }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded increases consecutive errors count if backend request unsuccessful`() {
        val errorCallbackResponse = Pair(PurchasesError(PurchasesErrorCode.ConfigurationError), true)
        mockBackendResponse(testDiagnosticsEntryJSONs, errorReturn = errorCallbackResponse)

        diagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) {
            sharedPreferencesEditor.putInt(DiagnosticsHelper.CONSECUTIVE_FAILURES_COUNT_KEY, 1)
        }
        verify(exactly = 0) { diagnosticsFileHelper.deleteFile() }
        verify(exactly = 0) { sharedPreferencesEditor.remove(DiagnosticsHelper.CONSECUTIVE_FAILURES_COUNT_KEY) }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded does not delete file if backend request unsuccessful once`() {
        val errorCallbackResponse = Pair(PurchasesError(PurchasesErrorCode.ConfigurationError), true)
        mockBackendResponse(testDiagnosticsEntryJSONs, errorReturn = errorCallbackResponse)

        diagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()

        verify(exactly = 0) { diagnosticsFileHelper.deleteFile() }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded deletes file if backend request unsuccessful and last retry`() {
        every { diagnosticsTracker.trackMaxDiagnosticsSyncRetriesReached() } just Runs
        val errorCallbackResponse = Pair(PurchasesError(PurchasesErrorCode.ConfigurationError), true)
        mockBackendResponse(testDiagnosticsEntryJSONs, errorReturn = errorCallbackResponse)
        every {
            sharedPreferences.getInt(DiagnosticsHelper.CONSECUTIVE_FAILURES_COUNT_KEY, 0)
        } returns DiagnosticsSynchronizer.MAX_NUMBER_POST_RETRIES - 1

        diagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) { diagnosticsFileHelper.deleteFile() }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded removes consecutive failures count if request unsuccessful and last retry`() {
        every { diagnosticsTracker.trackMaxDiagnosticsSyncRetriesReached() } just Runs
        val errorCallbackResponse = Pair(PurchasesError(PurchasesErrorCode.ConfigurationError), true)
        mockBackendResponse(testDiagnosticsEntryJSONs, errorReturn = errorCallbackResponse)
        every {
            sharedPreferences.getInt(DiagnosticsHelper.CONSECUTIVE_FAILURES_COUNT_KEY, 0)
        } returns DiagnosticsSynchronizer.MAX_NUMBER_POST_RETRIES - 1

        diagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) { sharedPreferencesEditor.remove(DiagnosticsHelper.CONSECUTIVE_FAILURES_COUNT_KEY) }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded tracks maxDiagnosticsSyncRetriesReached if request unsuccessful and last retry`() {
        every { diagnosticsTracker.trackMaxDiagnosticsSyncRetriesReached() } just Runs
        val errorCallbackResponse = Pair(PurchasesError(PurchasesErrorCode.ConfigurationError), true)
        mockBackendResponse(testDiagnosticsEntryJSONs, errorReturn = errorCallbackResponse)
        every {
            sharedPreferences.getInt(DiagnosticsHelper.CONSECUTIVE_FAILURES_COUNT_KEY, 0)
        } returns DiagnosticsSynchronizer.MAX_NUMBER_POST_RETRIES - 1

        diagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) { diagnosticsTracker.trackMaxDiagnosticsSyncRetriesReached() }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded removes file if should not retry`() {
        every { diagnosticsTracker.trackClearingDiagnosticsAfterFailedSync() } just Runs
        val errorCallbackResponse = Pair(PurchasesError(PurchasesErrorCode.ConfigurationError), false)
        mockBackendResponse(testDiagnosticsEntryJSONs, errorReturn = errorCallbackResponse)

        diagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) { diagnosticsFileHelper.deleteFile() }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded removes consecutive failures count if should not retry`() {
        every { diagnosticsTracker.trackClearingDiagnosticsAfterFailedSync() } just Runs
        val errorCallbackResponse = Pair(PurchasesError(PurchasesErrorCode.ConfigurationError), false)
        mockBackendResponse(testDiagnosticsEntryJSONs, errorReturn = errorCallbackResponse)

        diagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) { sharedPreferencesEditor.remove(DiagnosticsHelper.CONSECUTIVE_FAILURES_COUNT_KEY) }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded tracks clearingDiagnosticsAfterFailedSync if should not retry`() {
        every { diagnosticsTracker.trackClearingDiagnosticsAfterFailedSync() } just Runs
        val errorCallbackResponse = Pair(PurchasesError(PurchasesErrorCode.ConfigurationError), false)
        mockBackendResponse(testDiagnosticsEntryJSONs, errorReturn = errorCallbackResponse)

        diagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()

        verify(exactly = 1) { diagnosticsTracker.trackClearingDiagnosticsAfterFailedSync() }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded deletes file if IOException happens`() {
        every { diagnosticsFileHelper.readFileAsJson(any()) } throws IOException()
        diagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()
        verify(exactly = 1) { diagnosticsFileHelper.deleteFile() }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded deletes file if JSONException happens`() {
        every { diagnosticsFileHelper.readFileAsJson(any()) } throws JSONException("test-exception")
        diagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()
        verify(exactly = 1) { diagnosticsFileHelper.deleteFile() }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded removes consecutive failures count if IOException happens`() {
        every { diagnosticsFileHelper.readFileAsJson(any()) } throws IOException()
        diagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()
        verify(exactly = 1) { sharedPreferencesEditor.remove(DiagnosticsHelper.CONSECUTIVE_FAILURES_COUNT_KEY) }
    }

    @Test
    fun `syncDiagnosticsFileIfNeeded does not crash if IOException happens when deleting file`() {
        every { diagnosticsFileHelper.readFileAsJson(any()) } throws IOException()
        every { diagnosticsFileHelper.deleteFile() } throws IOException()
        diagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()
    }

    // endregion

    private fun mockSharedPreferences() {
        sharedPreferences = mockk()
        sharedPreferencesEditor = mockk()
        every { sharedPreferences.edit() } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.apply() } just Runs
        every {
            sharedPreferencesEditor.remove(DiagnosticsHelper.CONSECUTIVE_FAILURES_COUNT_KEY)
        } returns sharedPreferencesEditor
        every {
            sharedPreferencesEditor.putInt(DiagnosticsHelper.CONSECUTIVE_FAILURES_COUNT_KEY, any())
        } returns sharedPreferencesEditor
        every {
            sharedPreferences.getInt(DiagnosticsHelper.CONSECUTIVE_FAILURES_COUNT_KEY, 0)
        } returns 0
    }

    private fun mockDiagnosticsFileHelper() {
        diagnosticsFileHelper = mockk()
        mockReadDiagnosticsFile(testDiagnosticsEntryJSONs)
        every { diagnosticsFileHelper.deleteFile() } just Runs
        every { diagnosticsFileHelper.clear(testDiagnosticsEntryJSONs.size) } just Runs
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

    private fun mockReadDiagnosticsFile(jsons: List<JSONObject>) {
        val slot = slot<((Stream<JSONObject>) -> Unit)>()
        every { diagnosticsFileHelper.readFileAsJson(capture(slot)) } answers {
            slot.captured(jsons.stream())
        }
    }
}
