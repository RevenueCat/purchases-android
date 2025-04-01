package com.revenuecat.purchases.common.diagnostics

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.DateProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date
import java.util.UUID

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class DiagnosticsEntryTest {

    private val testDate = Date(1675954145000L) // Thursday, February 9, 2023 2:49:05 PM GMT

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
        val eventID = UUID.randomUUID()
        val appSessionID = UUID.randomUUID()
        val event = DiagnosticsEntry(
            id = eventID,
            name = DiagnosticsEntryName.HTTP_REQUEST_PERFORMED,
            properties = mapOf("test-key-1" to "test-value-1", "test-key-2" to 123, "test-key-3" to true),
            dateProvider = testDateProvider,
            appSessionID = appSessionID,
        )
        val eventAsString = event.toString()
        val expectedString = "{" +
            "\"id\":\"$eventID\"," +
            "\"version\":1," +
            "\"name\":\"http_request_performed\"," +
            "\"properties\":{\"test-key-1\":\"test-value-1\",\"test-key-2\":123,\"test-key-3\":true}," +
            "\"app_session_id\":\"$appSessionID\"," +
            "\"timestamp\":\"2023-02-09T14:49:05.000Z\"" +
            "}"
        assertThat(eventAsString).isEqualTo(expectedString)
    }
}
