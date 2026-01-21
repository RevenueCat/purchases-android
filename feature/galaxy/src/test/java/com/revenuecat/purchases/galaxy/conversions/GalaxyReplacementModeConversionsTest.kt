package com.revenuecat.purchases.galaxy.conversions

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.models.GalaxyReplacementMode
import com.samsung.android.sdk.iap.lib.constants.HelperDefine
import kotlin.test.Test
import org.assertj.core.api.Assertions.assertThat
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GalaxyReplacementModeConversionsTest {

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `all replacement modes map to Samsung proration modes`() {
        val expectations = mapOf(
            GalaxyReplacementMode.INSTANT_PRORATED_DATE to HelperDefine.ProrationMode.INSTANT_PRORATED_DATE,
            GalaxyReplacementMode.INSTANT_PRORATED_CHARGE to HelperDefine.ProrationMode.INSTANT_PRORATED_CHARGE,
            GalaxyReplacementMode.INSTANT_NO_PRORATION to HelperDefine.ProrationMode.INSTANT_NO_PRORATION,
            GalaxyReplacementMode.DEFERRED to HelperDefine.ProrationMode.DEFERRED,
        )

        for (mode in GalaxyReplacementMode.values()) {
            val expected = expectations[mode] ?: error("Missing expected mapping for $mode")
            assertThat(mode.toSamsungProrationMode()).isEqualTo(expected)
        }

        assertThat(expectations.size).isEqualTo(GalaxyReplacementMode.values().size)
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
    @Test
    fun `default replacement mode is instant no proration`() {
        assertThat(GalaxyReplacementMode.default).isEqualTo(GalaxyReplacementMode.INSTANT_NO_PRORATION)
    }
}
