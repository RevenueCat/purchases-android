package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.models.RecurrenceMode
import com.revenuecat.purchases.models.toRecurrenceMode
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
 class RecurrenceModeTest {
    @Test
    fun testToRecurrenceMode() {
        val unknown0 = 0.toRecurrenceMode()
        val infinite = 1.toRecurrenceMode()
        val finite = 2.toRecurrenceMode()
        val nonRecurring = 3.toRecurrenceMode()
        val unknown4 = 4.toRecurrenceMode()

        assertThat(unknown0).isEqualTo(RecurrenceMode.UNKNOWN)
        assertThat(infinite).isEqualTo(RecurrenceMode.INFINITE_RECURRING)
        assertThat(finite).isEqualTo(RecurrenceMode.FINITE_RECURRING)
        assertThat(nonRecurring).isEqualTo(RecurrenceMode.NON_RECURRING)
        assertThat(unknown4).isEqualTo(RecurrenceMode.UNKNOWN)
    }
}
