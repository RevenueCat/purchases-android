package com.revenuecat.purchases.galaxy.conversions

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.galaxy.GalaxyStrings
import com.revenuecat.purchases.models.StoreReplacementMode
import com.samsung.android.sdk.iap.lib.constants.HelperDefine
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoreReplacementModeConversionsTest {

    private val storeReplacementModes = listOf(
        StoreReplacementMode.WITHOUT_PRORATION,
        StoreReplacementMode.WITH_TIME_PRORATION,
        StoreReplacementMode.CHARGE_FULL_PRICE,
        StoreReplacementMode.CHARGE_PRORATED_PRICE,
        StoreReplacementMode.DEFERRED,
    )

    @Test
    fun `supported store replacement modes map to Galaxy proration modes`() {
        val expectations = mapOf(
            StoreReplacementMode.WITHOUT_PRORATION to HelperDefine.ProrationMode.INSTANT_NO_PRORATION,
            StoreReplacementMode.WITH_TIME_PRORATION to HelperDefine.ProrationMode.INSTANT_PRORATED_DATE,
            StoreReplacementMode.CHARGE_PRORATED_PRICE to HelperDefine.ProrationMode.INSTANT_PRORATED_CHARGE,
            StoreReplacementMode.DEFERRED to HelperDefine.ProrationMode.DEFERRED,
        )

        storeReplacementModes
            .filter { it != StoreReplacementMode.CHARGE_FULL_PRICE }
            .forEach { mode ->
                val expected = expectations[mode] ?: error("Missing expected mapping for $mode")
                assertThat(mode.toGalaxyReplacementMode()).isEqualTo(expected)
            }

        assertThat(expectations.size).isEqualTo(storeReplacementModes.size - 1)
    }

    @Test
    fun `only charge full price is excluded from supported Galaxy mappings`() {
        val unsupportedModes = storeReplacementModes
            .filter { mode ->
                runCatching { mode.toGalaxyReplacementMode() }.isFailure
            }

        assertThat(unsupportedModes).containsExactly(StoreReplacementMode.CHARGE_FULL_PRICE)
    }

    @Test
    fun `charge full price throws unsupported error for Galaxy`() {
        val exception = catchThrowableOfType(
            { StoreReplacementMode.CHARGE_FULL_PRICE.toGalaxyReplacementMode() },
            PurchasesException::class.java,
        )

        assertThat(exception).isNotNull
        assertThat(exception.error.code).isEqualTo(PurchasesErrorCode.UnsupportedError)
        assertThat(exception.error.underlyingErrorMessage).isEqualTo(GalaxyStrings.CHARGE_FULL_PRICE_NOT_SUPPORTED)
    }
}
