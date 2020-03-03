package com.revenuecat.purchases

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.Date

class UtilsTest {

    @Test
    fun `Date time in seconds`() {
        val date = Date()
        val timeInMilliseconds = date.time
        val timeInSeconds = date.timeInSeconds
        assertThat(timeInMilliseconds / 1000).isEqualTo(timeInSeconds)
    }
}