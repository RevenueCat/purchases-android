package com.revenuecat.purchases.common.diagnostics

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.DateProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class DiagnosticsEntryTest {

    private val testDate = Date(1675954145L) // Thursday, February 9, 2023 2:49:05 PM GMT

    private lateinit var testDateProvider: DateProvider

    @Before
    fun setup() {
        testDateProvider = object : DateProvider {
            override val now: Date
                get() = testDate
        }
    }

    @Test
    fun `toString transforms event to correct JSON`() {
        val event = DiagnosticsEntry.Event(
            name = DiagnosticsEventName.HTTP_REQUEST_PERFORMED,
            properties = mapOf("test-key-1" to "test-value-1", "test-key-2" to 123, "test-key-3" to true),
            dateProvider = testDateProvider
        )
        val eventAsString = event.toString()
        val expectedString = "{" +
            "\"version\":1," +
            "\"type\":\"event\"," +
            "\"name\":\"http_request_performed\"," +
            "\"properties\":{\"test-key-1\":\"test-value-1\",\"test-key-2\":123,\"test-key-3\":true}," +
            "\"timestamp\":1675954145" +
            "}"
        assertThat(eventAsString).isEqualTo(expectedString)
    }

    @Test
    fun `toString transforms counter to correct JSON`() {
        val event = DiagnosticsEntry.Counter(
            name = DiagnosticsCounterName.HTTP_REQUEST_PERFORMED,
            tags = mapOf("test-key-1" to "test-value-1", "test-key-2" to "test-value-2"),
            value = 2
        )
        val eventAsString = event.toString()
        val expectedString = "{" +
            "\"version\":1," +
            "\"type\":\"counter\"," +
            "\"name\":\"http_request_performed\"," +
            "\"tags\":{\"test-key-1\":\"test-value-1\",\"test-key-2\":\"test-value-2\"}," +
            "\"value\":2" +
            "}"
        assertThat(eventAsString).isEqualTo(expectedString)
    }

    @Test
    fun `toString transforms histogram to correct JSON`() {
        val event = DiagnosticsEntry.Histogram(
            name = "test_histogram_name",
            tags = mapOf("test-key-1" to "test-value-1", "test-key-2" to "test-value-2"),
            values = listOf(2.1, 3.4)
        )
        val eventAsString = event.toString()
        val expectedString = "{" +
            "\"version\":1," +
            "\"type\":\"histogram\"," +
            "\"name\":\"test_histogram_name\"," +
            "\"tags\":{\"test-key-1\":\"test-value-1\",\"test-key-2\":\"test-value-2\"}," +
            "\"values\":[2.1,3.4]" +
            "}"
        assertThat(eventAsString).isEqualTo(expectedString)
    }
}
