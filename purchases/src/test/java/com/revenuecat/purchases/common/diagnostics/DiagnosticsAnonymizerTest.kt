package com.revenuecat.purchases.common.diagnostics

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.Anonymizer
import com.revenuecat.purchases.common.DateProvider
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class DiagnosticsAnonymizerTest {

    private val testDate = Date(1675954145L) // Thursday, February 9, 2023 2:49:05 PM GMT

    private lateinit var anonymizer: Anonymizer

    private lateinit var diagnosticsAnonymizer: DiagnosticsAnonymizer

    private lateinit var testDateProvider: DateProvider

    @Before
    fun setup() {
        testDateProvider = object : DateProvider {
            override val now: Date
                get() = testDate
        }

        anonymizer = mockk()

        diagnosticsAnonymizer = DiagnosticsAnonymizer(anonymizer)
    }

    @Test
    fun `anonymizeEntryIfNeeded anonymizes event properties`() {
        val originalPropertiesMap = mapOf("key-1" to "value-1")
        val expectedPropertiesMap = mapOf("key-1" to "anonymized-value-1")
        val eventToAnonymize = DiagnosticsEntry.Event(
            name = DiagnosticsEventName.HTTP_REQUEST_PERFORMED,
            properties = originalPropertiesMap,
            dateProvider = testDateProvider
        )
        val expectedEvent = DiagnosticsEntry.Event(
            name = DiagnosticsEventName.HTTP_REQUEST_PERFORMED,
            properties = expectedPropertiesMap,
            dateProvider = testDateProvider,
            dateTime = testDate
        )
        every {
            anonymizer.anonymizedMap(originalPropertiesMap)
        } returns expectedPropertiesMap
        val anonymizedEvent = diagnosticsAnonymizer.anonymizeEntryIfNeeded(eventToAnonymize)
        assertThat(anonymizedEvent).isEqualTo(expectedEvent)
    }

    @Test
    fun `anonymizeEntryIfNeeded anonymizes counter tags`() {
        val originalPropertiesMap = mapOf("key-1" to "value-1")
        val expectedPropertiesMap = mapOf("key-1" to "anonymized-value-1")
        val counterToAnonymize = DiagnosticsEntry.Counter(
            name = DiagnosticsCounterName.HTTP_REQUEST_PERFORMED,
            tags = originalPropertiesMap,
            value = 123
        )
        val expectedCounter = DiagnosticsEntry.Counter(
            name = DiagnosticsCounterName.HTTP_REQUEST_PERFORMED,
            tags = expectedPropertiesMap,
            value = 123
        )
        every {
            anonymizer.anonymizedStringMap(originalPropertiesMap)
        } returns expectedPropertiesMap
        val anonymizedCounter = diagnosticsAnonymizer.anonymizeEntryIfNeeded(counterToAnonymize)
        assertThat(anonymizedCounter).isEqualTo(expectedCounter)
    }

    @Test
    fun `anonymizeEntryIfNeeded does not anonymize histogram`() {
        val histogramToAnonymize = DiagnosticsEntry.Histogram(
            name = "metric-name",
            tags = emptyMap(),
            values = listOf(1.1)
        )
        val anonymizedHistogram = diagnosticsAnonymizer.anonymizeEntryIfNeeded(histogramToAnonymize)
        assertThat(anonymizedHistogram).isEqualTo(histogramToAnonymize)
    }
}
