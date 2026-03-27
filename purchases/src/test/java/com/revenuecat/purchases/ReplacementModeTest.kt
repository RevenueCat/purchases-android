package com.revenuecat.purchases

import android.os.Parcel
import com.revenuecat.purchases.models.GalaxyReplacementMode
import com.revenuecat.purchases.models.GoogleReplacementMode
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

    private class TestReplacementMode(
        override val name: String,
    ) : ReplacementMode {
        override fun describeContents(): Int = 0

        override fun writeToParcel(dest: Parcel, flags: Int) {
            // No-op for test.
        }
    }
}
