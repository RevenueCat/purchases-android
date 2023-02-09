package com.revenuecat.purchases.common.diagnostics

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class DiagnosticsEventTest {

    @Test
    fun `toString transforms metrics event to correct JSON`() {
        val event = DiagnosticsEvent.Metric(
            name = MetricEventName.ETAG_HIT_RATE,
            tags = listOf("test-1", "test-2"),
            value = 2
        )
        val eventAsString = event.toString()
        val expectedString = "{" +
            "\"version\":1," +
            "\"type\":\"metric\"," +
            "\"name\":\"etag_hit_rate\"," +
            "\"tags\":[\"test-1\",\"test-2\"]," +
            "\"value\":2" +
            "}"
        assertThat(eventAsString).isEqualTo(expectedString)
    }

    @Test
    fun `toString transforms exception event to correct JSON`() {
        val event = DiagnosticsEvent.Exception(
            exceptionClass = "TestClass.kt",
            message = "test message",
            location = "DiagnosticsEvent:121",
            timestamp = "timestamp"
        )
        val eventAsString = event.toString()
        val expectedString = "{" +
            "\"version\":1," +
            "\"type\":\"exception\"," +
            "\"exc_class\":\"TestClass.kt\"," +
            "\"message\":\"test message\"," +
            "\"location\":\"DiagnosticsEvent:121\"," +
            "\"timestamp\":\"timestamp\"" +
            "}"
        assertThat(eventAsString).isEqualTo(expectedString)
    }
}
