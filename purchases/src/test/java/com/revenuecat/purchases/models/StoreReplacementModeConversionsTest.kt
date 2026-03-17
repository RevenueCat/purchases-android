package com.revenuecat.purchases.models

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingFlowParams
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoreReplacementModeConversionsTest {

    @Test
    fun `all store replacement modes map to Google BillingClient modes`() {
        val expectations = mapOf(
            StoreReplacementMode.WITHOUT_PRORATION to
                BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITHOUT_PRORATION,
            StoreReplacementMode.WITH_TIME_PRORATION to
                BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION,
            StoreReplacementMode.CHARGE_FULL_PRICE to
                BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_FULL_PRICE,
            StoreReplacementMode.CHARGE_PRORATED_PRICE to
                BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_PRORATED_PRICE,
            StoreReplacementMode.DEFERRED to
                BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.DEFERRED,
        )

        StoreReplacementMode.values().forEach { mode ->
            val expected = expectations[mode] ?: error("Missing expected mapping for $mode")
            assertThat(mode.toPlayBillingClientMode()).isEqualTo(expected)
        }

        assertThat(expectations.size).isEqualTo(StoreReplacementMode.values().size)
    }

    @Test
    fun `all store replacement modes map to deprecated Google replacement modes`() {
        val expectations = mapOf(
            StoreReplacementMode.WITHOUT_PRORATION to GoogleReplacementMode.WITHOUT_PRORATION,
            StoreReplacementMode.WITH_TIME_PRORATION to GoogleReplacementMode.WITH_TIME_PRORATION,
            StoreReplacementMode.CHARGE_FULL_PRICE to GoogleReplacementMode.CHARGE_FULL_PRICE,
            StoreReplacementMode.CHARGE_PRORATED_PRICE to GoogleReplacementMode.CHARGE_PRORATED_PRICE,
                StoreReplacementMode.DEFERRED to GoogleReplacementMode.DEFERRED,
        )

        StoreReplacementMode.values().forEach { mode ->
            val expected = expectations[mode] ?: error("Missing expected mapping for $mode")
            assertThat(mode.toGoogleReplacementMode()).isEqualTo(expected)
        }

        assertThat(expectations.size).isEqualTo(StoreReplacementMode.values().size)
    }
}
