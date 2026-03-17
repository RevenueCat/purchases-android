package com.revenuecat.purchases

import android.os.Parcel
import com.revenuecat.purchases.models.GalaxyReplacementMode
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.StoreReplacementMode
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ReplacementModeTest {

    @Test
    fun `google replacement modes map to legacy backend names`() {
        val expectations = mapOf(
            GoogleReplacementMode.WITHOUT_PRORATION to "IMMEDIATE_WITHOUT_PRORATION",
            GoogleReplacementMode.WITH_TIME_PRORATION to "IMMEDIATE_WITH_TIME_PRORATION",
            GoogleReplacementMode.CHARGE_FULL_PRICE to "IMMEDIATE_AND_CHARGE_FULL_PRICE",
            GoogleReplacementMode.CHARGE_PRORATED_PRICE to "IMMEDIATE_AND_CHARGE_PRORATED_PRICE",
            GoogleReplacementMode.DEFERRED to "DEFERRED",
        )

        GoogleReplacementMode.values().forEach { mode ->
            assertThat(expectations).containsKey(mode)
            assertThat(mode.backendName).isEqualTo(expectations.getValue(mode))
        }
        assertThat(expectations.size).isEqualTo(GoogleReplacementMode.values().size)
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `galaxy replacement modes use enum name for backend`() {
        val expectations = mapOf(
            GalaxyReplacementMode.INSTANT_PRORATED_DATE to "INSTANT_PRORATED_DATE",
            GalaxyReplacementMode.INSTANT_PRORATED_CHARGE to "INSTANT_PRORATED_CHARGE",
            GalaxyReplacementMode.INSTANT_NO_PRORATION to "INSTANT_NO_PRORATION",
            GalaxyReplacementMode.DEFERRED to "DEFERRED",
        )

        GalaxyReplacementMode.values().forEach { mode ->
            assertThat(expectations).containsKey(mode)
            assertThat(mode.backendName).isEqualTo(expectations.getValue(mode))
        }
        assertThat(expectations.size).isEqualTo(GalaxyReplacementMode.values().size)
    }

    @Test
    fun `backend name falls back to replacement mode name`() {
        val mode = TestReplacementMode("CUSTOM_MODE")

        assertThat(mode.backendName).isEqualTo("CUSTOM_MODE")
    }

    @Test
    fun `store replacement modes map to legacy Play Store replacement mode backend names`() {
        val expectations = mapOf(
            StoreReplacementMode.WITHOUT_PRORATION to "IMMEDIATE_WITHOUT_PRORATION",
            StoreReplacementMode.WITH_TIME_PRORATION to "IMMEDIATE_WITH_TIME_PRORATION",
            StoreReplacementMode.CHARGE_FULL_PRICE to "IMMEDIATE_AND_CHARGE_FULL_PRICE",
            StoreReplacementMode.CHARGE_PRORATED_PRICE to "IMMEDIATE_AND_CHARGE_PRORATED_PRICE",
            StoreReplacementMode.DEFERRED to "DEFERRED",
        )

        StoreReplacementMode.values().forEach { mode ->
            assertThat(expectations).containsKey(mode)
            assertThat(mode.backendName(Store.PLAY_STORE)).isEqualTo(expectations.getValue(mode))
        }
        assertThat(expectations.size).isEqualTo(StoreReplacementMode.values().size)
    }

    @Test
    fun `store replacement modes map to Galaxy backend names`() {
        val expectations = mapOf(
            StoreReplacementMode.WITHOUT_PRORATION to "INSTANT_NO_PRORATION",
            StoreReplacementMode.WITH_TIME_PRORATION to "INSTANT_PRORATED_DATE",
            StoreReplacementMode.CHARGE_PRORATED_PRICE to "INSTANT_PRORATED_CHARGE",
            StoreReplacementMode.DEFERRED to "DEFERRED",
        )

        StoreReplacementMode.values().forEach { mode ->
            if (mode == StoreReplacementMode.CHARGE_FULL_PRICE) {
                assertThat(mode.backendName(Store.GALAXY)).isNull()
            } else {
                assertThat(expectations).containsKey(mode)
                assertThat(mode.backendName(Store.GALAXY)).isEqualTo(expectations.getValue(mode))
            }
        }
        assertThat(expectations.size).isEqualTo(StoreReplacementMode.values().size - 1)
    }

    @Test
    fun `google replacement modes use store specific backend names`() {
        val playExpectations = mapOf(
            GoogleReplacementMode.WITHOUT_PRORATION to "IMMEDIATE_WITHOUT_PRORATION",
            GoogleReplacementMode.WITH_TIME_PRORATION to "IMMEDIATE_WITH_TIME_PRORATION",
            GoogleReplacementMode.CHARGE_FULL_PRICE to "IMMEDIATE_AND_CHARGE_FULL_PRICE",
            GoogleReplacementMode.CHARGE_PRORATED_PRICE to "IMMEDIATE_AND_CHARGE_PRORATED_PRICE",
            GoogleReplacementMode.DEFERRED to "DEFERRED",
        )
        val galaxyExpectations = mapOf(
            GoogleReplacementMode.WITHOUT_PRORATION to "INSTANT_NO_PRORATION",
            GoogleReplacementMode.WITH_TIME_PRORATION to "INSTANT_PRORATED_DATE",
            GoogleReplacementMode.CHARGE_PRORATED_PRICE to "INSTANT_PRORATED_CHARGE",
            GoogleReplacementMode.DEFERRED to "DEFERRED",
        )

        GoogleReplacementMode.values().forEach { mode ->
            assertThat(playExpectations).containsKey(mode)
            assertThat(mode.backendName(Store.PLAY_STORE)).isEqualTo(playExpectations.getValue(mode))

            if (mode == GoogleReplacementMode.CHARGE_FULL_PRICE) {
                assertThat(mode.backendName(Store.GALAXY)).isNull()
            } else {
                assertThat(galaxyExpectations).containsKey(mode)
                assertThat(mode.backendName(Store.GALAXY)).isEqualTo(galaxyExpectations.getValue(mode))
            }
        }
        assertThat(playExpectations.size).isEqualTo(GoogleReplacementMode.values().size)
        assertThat(galaxyExpectations.size).isEqualTo(GoogleReplacementMode.values().size - 1)
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `galaxy replacement modes use store specific backend names`() {
        val playExpectations = mapOf(
            GalaxyReplacementMode.INSTANT_NO_PRORATION to "IMMEDIATE_WITHOUT_PRORATION",
            GalaxyReplacementMode.INSTANT_PRORATED_DATE to "IMMEDIATE_WITH_TIME_PRORATION",
            GalaxyReplacementMode.INSTANT_PRORATED_CHARGE to "IMMEDIATE_AND_CHARGE_PRORATED_PRICE",
            GalaxyReplacementMode.DEFERRED to "DEFERRED",
        )
        val galaxyExpectations = mapOf(
            GalaxyReplacementMode.INSTANT_PRORATED_DATE to "INSTANT_PRORATED_DATE",
            GalaxyReplacementMode.INSTANT_PRORATED_CHARGE to "INSTANT_PRORATED_CHARGE",
            GalaxyReplacementMode.INSTANT_NO_PRORATION to "INSTANT_NO_PRORATION",
            GalaxyReplacementMode.DEFERRED to "DEFERRED",
        )

        GalaxyReplacementMode.values().forEach { mode ->
            assertThat(playExpectations).containsKey(mode)
            assertThat(mode.backendName(Store.PLAY_STORE)).isEqualTo(playExpectations.getValue(mode))
            assertThat(galaxyExpectations).containsKey(mode)
            assertThat(mode.backendName(Store.GALAXY)).isEqualTo(galaxyExpectations.getValue(mode))
        }
        assertThat(playExpectations.size).isEqualTo(GalaxyReplacementMode.values().size)
        assertThat(galaxyExpectations.size).isEqualTo(GalaxyReplacementMode.values().size)
    }

    private class TestReplacementMode(
        override val name: String,
    ) : ReplacementMode {
        override fun describeContents(): Int = 0

        override fun writeToParcel(dest: Parcel, flags: Int) {
            // No-op for test.
        }
    }
}
