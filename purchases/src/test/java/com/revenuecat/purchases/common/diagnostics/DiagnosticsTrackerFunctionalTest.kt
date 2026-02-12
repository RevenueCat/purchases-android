package com.revenuecat.purchases.common.diagnostics

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.FileHelper
import com.revenuecat.purchases.common.SyncDispatcher
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class DiagnosticsTrackerFunctionalTest {

    private val testFolder = "temp_test_folder"

    private lateinit var applicationContext: Context

    private lateinit var diagnosticsTracker: DiagnosticsTracker

    @Before
    fun setup() {
        val tempTestFolder = File(testFolder)
        if (tempTestFolder.exists()) {
            error("Temp test folder should not exist before starting tests")
        }
        tempTestFolder.mkdirs()

        applicationContext = mockk()
        val appConfig = mockk<AppConfig>().apply {
            every { playStoreVersionName } returns "test-store-version-name"
            every { playServicesVersionName } returns "test-services-version-name"
            every { store } returns Store.PLAY_STORE
        }
        every { applicationContext.filesDir } returns tempTestFolder

        val diagnosticsFileHelper = DiagnosticsFileHelper(FileHelper(applicationContext))

        diagnosticsTracker = DiagnosticsTracker(
            appConfig = appConfig,
            diagnosticsFileHelper = diagnosticsFileHelper,
            diagnosticsHelper = DiagnosticsHelper(applicationContext, diagnosticsFileHelper, lazy { mockk(relaxed = true) }),
            diagnosticsDispatcher = SyncDispatcher(),
        )
    }

    @After
    fun tearDown() {
        val tempTestFolder = File(testFolder)
        tempTestFolder.deleteRecursively()
    }

    @Test
    fun `diagnostics tracker file is not cleared if not too big when tracking files`() {
        createDiagnosticsFile(1000)
        assertFalse(hasDiagnosticsFileBeenCleared())
    }

    @Test
    fun `diagnostics synchronizer file is cleared if too big`() {
        createDiagnosticsFile(3000)
        assertTrue(hasDiagnosticsFileBeenCleared())
    }

    private fun createDiagnosticsFile(numberOfEvents: Int) {
        val event = createDiagnosticsEntry()
        for (i in 0..numberOfEvents) {
            diagnosticsTracker.trackEvent(event)
        }
    }

    private fun createDiagnosticsEntry(): DiagnosticsEntry {
        return DiagnosticsEntry(
            name = DiagnosticsEntryName.GOOGLE_QUERY_PURCHASES_REQUEST,
            properties = mapOf(
                "test_key_1" to "test_value_1",
                "test_key_2" to Random.nextBoolean(),
                "test_key_3" to Random.nextInt(),
                "test_key_4" to "test_value_${Random.nextInt()}",
                "test_key_5" to "test_value_5",
            ),
            appSessionID = UUID.randomUUID(),
        )
    }

    private fun hasDiagnosticsFileBeenCleared(): Boolean {
        val file = File(testFolder, DiagnosticsFileHelper.DIAGNOSTICS_FILE_PATH)
        if (!file.exists()) return true
        return file.readText().contains(DiagnosticsEntryName.MAX_EVENTS_STORED_LIMIT_REACHED.name.lowercase())
    }
}
