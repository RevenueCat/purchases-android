package com.revenuecat.purchases.common.diagnostics

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.FileHelper
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
import java.util.stream.Stream

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class DiagnosticsFileHelperTest {

    private val testDiagnosticsEntry = DiagnosticsEntry(
        name = DiagnosticsEntryName.HTTP_REQUEST_PERFORMED,
        properties = emptyMap()
    )
    private val diagnosticsFilePath = DiagnosticsFileHelper.DIAGNOSTICS_FILE_PATH

    private lateinit var fileHelper: FileHelper

    private lateinit var diagnosticsFileHelper: DiagnosticsFileHelper

    @Before
    fun setup() {
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
        diagnosticsFileHelper.appendEvent(testDiagnosticsEntry)
        verify(exactly = 1) { fileHelper.appendToFile(diagnosticsFilePath, contentToAppend) }
    }

    @Test
    fun `deleteOlderDiagnostics calls are correct`() {
        every { fileHelper.removeFirstLinesFromFile(diagnosticsFilePath, 2) } just Runs
        diagnosticsFileHelper.clear(2)
        verify(exactly = 1) { fileHelper.removeFirstLinesFromFile(diagnosticsFilePath, 2) }
    }

    @Test
    fun `deleteDiagnosticsFile calls are correct`() {
        every { fileHelper.deleteFile(diagnosticsFilePath) } returns true
        diagnosticsFileHelper.deleteFile()
        verify(exactly = 1) { fileHelper.deleteFile(diagnosticsFilePath) }
    }

    @Test
    fun `readDiagnosticsFile returns empty list if file is empty`() {
        every { fileHelper.fileIsEmpty(diagnosticsFilePath) } returns true
        var resultList: List<JSONObject>? = null
        diagnosticsFileHelper.readFileAsJson { stream ->
            resultList = stream.toList()
        }
        verify(exactly = 1) { fileHelper.fileIsEmpty(diagnosticsFilePath) }
        assertThat(resultList).isNotNull
        assertThat(resultList).isEmpty()
        verify(exactly = 0) { fileHelper.readFilePerLines(diagnosticsFilePath, any()) }
    }

    @Test
    fun `readDiagnosticsFile reads content as json`() {
        every { fileHelper.fileIsEmpty(diagnosticsFilePath) } returns false
        val streamBlockSlot = slot<((Stream<String>) -> Unit)>()
        every { fileHelper.readFilePerLines(diagnosticsFilePath, capture(streamBlockSlot)) } answers {
            streamBlockSlot.captured(Stream.of("{}", "{\"test_key\": \"test_value\"}"))
        }
        var resultList: List<JSONObject>? = null
        diagnosticsFileHelper.readFileAsJson { stream ->
            resultList = stream.toList()
        }
        assertThat(resultList).isNotNull
        assertThat(resultList?.size).isEqualTo(2)
        assertThat(resultList?.get(0)?.length()).isEqualTo(0)
        assertThat(resultList?.get(1)?.get("test_key")).isEqualTo("test_value")
    }
}
