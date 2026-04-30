package com.revenuecat.purchases.models

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoreReplacementModeTest {
    @Test
    fun `equality works`() {
        assertThat(StoreReplacementMode.CHARGE_FULL_PRICE).isEqualTo(StoreReplacementMode.CHARGE_FULL_PRICE)
        assertThat(StoreReplacementMode.CHARGE_PRORATED_PRICE).isEqualTo(StoreReplacementMode.CHARGE_PRORATED_PRICE)
        assertThat(StoreReplacementMode.WITH_TIME_PRORATION).isEqualTo(StoreReplacementMode.WITH_TIME_PRORATION)
        assertThat(StoreReplacementMode.WITHOUT_PRORATION).isEqualTo(StoreReplacementMode.WITHOUT_PRORATION)
        assertThat(StoreReplacementMode.DEFERRED).isEqualTo(StoreReplacementMode.DEFERRED)
    }
}
