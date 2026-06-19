package com.revenuecat.purchases.galaxy.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
class GalaxyDateParserTest {

    private var previousTimeZone: TimeZone? = null

    @Before
    fun setUp() {
        previousTimeZone = TimeZone.getDefault()
    }

    @After
    fun tearDown() {
        previousTimeZone?.let { TimeZone.setDefault(it) }
    }

    @Test
    fun `parseGalaxyDate parses local timezone timestamp`() {
        val timeZone = TimeZone.getTimeZone("America/Los_Angeles")
        TimeZone.setDefault(timeZone)

        val parsed = "2024-01-15 13:45:20".parseDateFromGalaxyDateString()

        val expectedCalendar = Calendar.getInstance(timeZone, Locale.US).apply {
            set(2024, Calendar.JANUARY, 15, 13, 45, 20)
            set(Calendar.MILLISECOND, 0)
        }

        assertThat(parsed).isEqualTo(expectedCalendar.time)
    }

    @Test
    fun `parseGalaxyDate throws for invalid format`() {
        assertThatThrownBy { "15-01-2024 abc13:45:20".parseDateFromGalaxyDateString() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Could not parse purchase date for Galaxy Store purchase. " +
                "Purchase date string: 15-01-2024 abc13:45:20"
            )
    }
}
