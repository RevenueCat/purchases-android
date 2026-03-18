package com.revenuecat.purchases.models

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingFlowParams
import com.revenuecat.purchases.Store
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
    fun `all store replacement modes map to store specific backend names`() {
        val playExpectations = mapOf(
            StoreReplacementMode.WITHOUT_PRORATION to "IMMEDIATE_WITHOUT_PRORATION",
            StoreReplacementMode.WITH_TIME_PRORATION to "IMMEDIATE_WITH_TIME_PRORATION",
            StoreReplacementMode.CHARGE_FULL_PRICE to "IMMEDIATE_AND_CHARGE_FULL_PRICE",
            StoreReplacementMode.CHARGE_PRORATED_PRICE to "IMMEDIATE_AND_CHARGE_PRORATED_PRICE",
            StoreReplacementMode.DEFERRED to "DEFERRED",
        )
        val galaxyExpectations = mapOf(
            StoreReplacementMode.WITHOUT_PRORATION to "INSTANT_NO_PRORATION",
            StoreReplacementMode.WITH_TIME_PRORATION to "INSTANT_PRORATED_DATE",
            StoreReplacementMode.CHARGE_PRORATED_PRICE to "INSTANT_PRORATED_CHARGE",
            StoreReplacementMode.DEFERRED to "DEFERRED",
        )

        StoreReplacementMode.values().forEach { mode ->
            assertThat(mode.storeBackendName(Store.PLAY_STORE)).isEqualTo(playExpectations.getValue(mode))

            if (mode == StoreReplacementMode.CHARGE_FULL_PRICE) {
                assertThat(mode.storeBackendName(Store.GALAXY)).isNull()
            } else {
                assertThat(mode.storeBackendName(Store.GALAXY)).isEqualTo(galaxyExpectations.getValue(mode))
            }
        }
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

    @Test
    fun `all deprecated Google replacement modes map to store replacement modes`() {
        val expectations = mapOf(
            GoogleReplacementMode.WITHOUT_PRORATION to StoreReplacementMode.WITHOUT_PRORATION,
            GoogleReplacementMode.WITH_TIME_PRORATION to StoreReplacementMode.WITH_TIME_PRORATION,
            GoogleReplacementMode.CHARGE_FULL_PRICE to StoreReplacementMode.CHARGE_FULL_PRICE,
            GoogleReplacementMode.CHARGE_PRORATED_PRICE to StoreReplacementMode.CHARGE_PRORATED_PRICE,
            GoogleReplacementMode.DEFERRED to StoreReplacementMode.DEFERRED,
        )

        GoogleReplacementMode.values().forEach { mode ->
            val expected = expectations[mode] ?: error("Missing expected mapping for $mode")
            assertThat(mode.toStoreReplacementMode()).isEqualTo(expected)
        }

        assertThat(expectations.size).isEqualTo(GoogleReplacementMode.values().size)
    }

    @Test
    fun `ReplacementMode normalization returns canonical store replacement modes`() {
        assertThat(StoreReplacementMode.DEFERRED.toStoreReplacementModeOrNull()).isEqualTo(StoreReplacementMode.DEFERRED)
        assertThat(GoogleReplacementMode.DEFERRED.toStoreReplacementModeOrNull()).isEqualTo(StoreReplacementMode.DEFERRED)
        assertThat((null as com.revenuecat.purchases.ReplacementMode?).toStoreReplacementModeOrNull()).isNull()
    }
}
