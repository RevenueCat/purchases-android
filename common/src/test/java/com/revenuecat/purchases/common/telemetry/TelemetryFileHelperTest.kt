package com.revenuecat.purchases.common.telemetry

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.FileHelper
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class TelemetryFileHelperTest {

    private val testTelemetryEvent = TelemetryEvent.Metric(
        name = MetricEventName.ETAG_HIT_RATE,
        tags = emptyList(),
        value = 1
    )
    private val telemetryFilePath = TelemetryFileHelper.TELEMETRY_FILE_PATH

    private lateinit var fileHelper: FileHelper

    private lateinit var telemetryFileHelper: TelemetryFileHelper

    @Before
    fun setup() {
        fileHelper = mockk()
        telemetryFileHelper = TelemetryFileHelper(fileHelper)
    }

    @Test
    fun `appendEventToTelemetryFile calls are correct`() {
        val contentToAppend = "$testTelemetryEvent\n"
        every { fileHelper.appendToFile(telemetryFilePath, contentToAppend) } just Runs
        telemetryFileHelper.appendEventToTelemetryFile(testTelemetryEvent)
        verify(exactly = 1) { fileHelper.appendToFile(telemetryFilePath, contentToAppend) }
    }

    @Test
    fun `cleanSentTelemetry calls are correct`() {
        every { fileHelper.removeFirstLinesFromFile(telemetryFilePath, 2) } just Runs
        telemetryFileHelper.cleanSentTelemetry(2)
        verify(exactly = 1) { fileHelper.removeFirstLinesFromFile(telemetryFilePath, 2) }
    }

    @Test
    fun `deleteTelemetryFile calls are correct`() {
        every { fileHelper.deleteFile(telemetryFilePath) } returns true
        telemetryFileHelper.deleteTelemetryFile()
        verify(exactly = 1) { fileHelper.deleteFile(telemetryFilePath) }
    }

    @Test
    fun `telemetryFileIsEmpty calls are correct`() {
        every { fileHelper.fileIsEmpty(telemetryFilePath) } returns true
        assertTrue(telemetryFileHelper.telemetryFileIsEmpty())
        verify(exactly = 1) { fileHelper.fileIsEmpty(telemetryFilePath) }
    }

    @Test
    fun `readTelemetryFile reads content as json`() {
        every { fileHelper.readFilePerLines(telemetryFilePath) } returns listOf(
            "{}", "{\"test_key\": \"test_value\"}"
        )
        val result = telemetryFileHelper.readTelemetryFile()
        assertThat(result.size).isEqualTo(2)
        assertThat(result[0].length()).isEqualTo(0)
        assertThat(result[1]["test_key"]).isEqualTo("test_value")
        verify(exactly = 1) { fileHelper.readFilePerLines(telemetryFilePath) }
    }
}
