package com.revenuecat.purchases.common.diagnostics

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.FileHelper
import com.revenuecat.purchases.utils.DataListener
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class DiagnosticsFileHelperTest {

    private val testDiagnosticsEntry = DiagnosticsEntry.Event(
        name = DiagnosticsEventName.HTTP_REQUEST_PERFORMED,
        properties = emptyMap()
    )
    private val diagnosticsFilePath = DiagnosticsFileHelper.DIAGNOSTICS_FILE_PATH

    private val dataListenerCallParams: MutableList<JSONObject> = mutableListOf()
    private var dataListenerOnCompleteCallCount = 0

    private lateinit var fileHelper: FileHelper
    private lateinit var dataListener: DataListener<JSONObject>

    private lateinit var diagnosticsFileHelper: DiagnosticsFileHelper

    @Before
    fun setup() {
        dataListenerCallParams.clear()
        dataListenerOnCompleteCallCount = 0
        dataListener = object : DataListener<JSONObject> {
            override fun onData(data: JSONObject) {
                dataListenerCallParams.add(data)
            }

            override fun onComplete() {
                dataListenerOnCompleteCallCount++
            }
        }
        fileHelper = mockk()
        diagnosticsFileHelper = DiagnosticsFileHelper(fileHelper)
    }

    @Test
    fun `isDiagnosticsFileTooBig is true if file bigger than limit`() {
        every { fileHelper.fileSizeInKB(diagnosticsFilePath) } returns
            DiagnosticsFileHelper.DIAGNOSTICS_FILE_LIMIT_IN_KB + 1.0
        assertThat(diagnosticsFileHelper.isDiagnosticsFileTooBig()).isTrue
    }

    @Test
    fun `isDiagnosticsFileTooBig is false if file smaller than limit`() {
        every { fileHelper.fileSizeInKB(diagnosticsFilePath) } returns
            DiagnosticsFileHelper.DIAGNOSTICS_FILE_LIMIT_IN_KB - 1.0
        assertThat(diagnosticsFileHelper.isDiagnosticsFileTooBig()).isFalse
    }

    @Test
    fun `appendEntryToDiagnosticsFile calls are correct`() {
        val contentToAppend = "$testDiagnosticsEntry\n"
        every { fileHelper.appendToFile(diagnosticsFilePath, contentToAppend) } just Runs
        diagnosticsFileHelper.appendEntryToDiagnosticsFile(testDiagnosticsEntry)
        verify(exactly = 1) { fileHelper.appendToFile(diagnosticsFilePath, contentToAppend) }
    }

    @Test
    fun `deleteOlderDiagnostics calls are correct`() {
        every { fileHelper.removeFirstLinesFromFile(diagnosticsFilePath, 2) } just Runs
        diagnosticsFileHelper.deleteOlderDiagnostics(2)
        verify(exactly = 1) { fileHelper.removeFirstLinesFromFile(diagnosticsFilePath, 2) }
    }

    @Test
    fun `deleteDiagnosticsFile calls are correct`() {
        every { fileHelper.deleteFile(diagnosticsFilePath) } returns true
        diagnosticsFileHelper.deleteDiagnosticsFile()
        verify(exactly = 1) { fileHelper.deleteFile(diagnosticsFilePath) }
    }

    @Test
    fun `readDiagnosticsFile returns empty list if file is empty`() {
        every { fileHelper.fileIsEmpty(diagnosticsFilePath) } returns true
        diagnosticsFileHelper.readDiagnosticsFile(dataListener)
        verify(exactly = 1) { fileHelper.fileIsEmpty(diagnosticsFilePath) }
        assertThat(dataListenerCallParams).isEmpty()
        assertThat(dataListenerOnCompleteCallCount).isEqualTo(1)
        verify(exactly = 0) { fileHelper.readFilePerLines(diagnosticsFilePath, any()) }
    }

    @Test
    fun `readDiagnosticsFile reads content as json`() {
        every { fileHelper.fileIsEmpty(diagnosticsFilePath) } returns false
        val dataListenerSlot = slot<DataListener<Pair<String, Int>>>()
        every { fileHelper.readFilePerLines(diagnosticsFilePath, capture(dataListenerSlot)) } answers {
            dataListenerSlot.captured.onData(Pair("{}", 0))
            dataListenerSlot.captured.onData(Pair("{\"test_key\": \"test_value\"}", 1))
            dataListenerSlot.captured.onComplete()
        }
        diagnosticsFileHelper.readDiagnosticsFile(dataListener)
        assertThat(dataListenerCallParams.size).isEqualTo(2)
        assertThat(dataListenerCallParams[0].length()).isEqualTo(0)
        assertThat(dataListenerCallParams[1]["test_key"]).isEqualTo("test_value")
        assertThat(dataListenerOnCompleteCallCount).isEqualTo(1)
    }

    @Test
    fun `readDiagnosticsFile passes maxLines requirement to fileHelper as expected`() {
        every { fileHelper.fileIsEmpty(diagnosticsFilePath) } returns false
        val dataListenerSlot = slot<DataListener<Pair<String, Int>>>()
        val maxLines = 2
        every { fileHelper.readFilePerLines(diagnosticsFilePath, capture(dataListenerSlot), maxLines) } answers {
            dataListenerSlot.captured.onData(Pair("{}", 0))
            dataListenerSlot.captured.onData(Pair("{\"test_key\": \"test_value\"}", 1))
            dataListenerSlot.captured.onComplete()
        }
        diagnosticsFileHelper.readDiagnosticsFile(dataListener, maxLines)
        verify(exactly = 1) { fileHelper.readFilePerLines(diagnosticsFilePath, any(), maxLines) }
    }
}
