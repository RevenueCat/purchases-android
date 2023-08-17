package com.revenuecat.purchases.common.diagnostics

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.FileHelper
import com.revenuecat.purchases.common.SyncDispatcher
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class DiagnosticsSynchronizerFunctionalTest {

    private val testFolder = "temp_test_folder"

    private lateinit var applicationContext: Context
    private lateinit var diagnosticsTracker: DiagnosticsTracker

    private lateinit var diagnosticsSynchronizer: DiagnosticsSynchronizer

    @Before
    fun setup() {
        val tempTestFolder = File(testFolder)
        if (tempTestFolder.exists()) {
            error("Temp test folder should not exist before starting tests")
        }
        tempTestFolder.mkdirs()

        applicationContext = mockk()
        every { applicationContext.filesDir } returns tempTestFolder
        diagnosticsTracker = mockk()
        every { diagnosticsTracker.trackMaxEventsStoredLimitReached() } just Runs

        diagnosticsSynchronizer = DiagnosticsSynchronizer(
            DiagnosticsFileHelper(FileHelper(applicationContext)),
            diagnosticsTracker = diagnosticsTracker,
            backend = mockk(),
            diagnosticsDispatcher = SyncDispatcher(),
            sharedPreferences = mockk(relaxed = true),
        )
    }

    @After
    fun tearDown() {
        val tempTestFolder = File(testFolder)
        tempTestFolder.deleteRecursively()
    }

    @Test
    fun `diagnostics synchronizer file is not cleared if not too big`() {
        createDiagnosticsFile(1000)
        diagnosticsSynchronizer.clearDiagnosticsFileIfTooBig()
        verify(exactly = 0) { diagnosticsTracker.trackMaxEventsStoredLimitReached() }
    }

    @Test
    fun `diagnostics synchronizer file is cleared if too big`() {
        createDiagnosticsFile(3000)
        diagnosticsSynchronizer.clearDiagnosticsFileIfTooBig()
        verify(exactly = 1) { diagnosticsTracker.trackMaxEventsStoredLimitReached() }
    }

    private fun createDiagnosticsFile(numberOfEvents: Int) {
        val contents = (1..numberOfEvents).joinToString("\n") { createDiagnosticsEntry().toString() }
        createTestFileWithContents(contents)
    }

    private fun createDiagnosticsEntry(): DiagnosticsEntry {
        return DiagnosticsEntry.Event(
            DiagnosticsEventName.GOOGLE_QUERY_PURCHASES_REQUEST,
            mapOf(
                "test_key_1" to "test_value_1",
                "test_key_2" to Random.nextBoolean(),
                "test_key_3" to Random.nextInt(),
                "test_key_4" to "test_value_${Random.nextInt()}",
                "test_key_5" to "test_value_5",
            )
        )
    }

    private fun createTestFileWithContents(contents: String) {
        val file = File(testFolder, DiagnosticsFileHelper.DIAGNOSTICS_FILE_PATH)
        file.parentFile?.mkdirs()
        file.createNewFile()
        file.writeText(contents)
    }
}
