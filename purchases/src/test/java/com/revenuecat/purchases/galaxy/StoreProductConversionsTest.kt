package com.revenuecat.purchases.galaxy

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ProductType
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoreProductConversionsTest {

    @Test
    fun `convertSamsungIAPTypeStringToRevenueCatProductType maps item to INAPP`() {
        val productType = "item".convertSamsungIAPTypeStringToRevenueCatProductType()

        assertThat(productType).isEqualTo(ProductType.INAPP)
    }

    @Test
    fun `convertSamsungIAPTypeStringToRevenueCatProductType maps subscription to SUBS`() {
        val productType = "subscription".convertSamsungIAPTypeStringToRevenueCatProductType()

        assertThat(productType).isEqualTo(ProductType.SUBS)
    }

    @Test
    fun `convertSamsungIAPTypeStringToRevenueCatProductType handles uppercase values`() {
        val upperCaseItem = "ITEM".convertSamsungIAPTypeStringToRevenueCatProductType()
        val upperCaseSubscription = "SUBSCRIPTION".convertSamsungIAPTypeStringToRevenueCatProductType()

        assertThat(upperCaseItem).isEqualTo(ProductType.INAPP)
        assertThat(upperCaseSubscription).isEqualTo(ProductType.SUBS)
    }

    @Test
    fun `convertSamsungIAPTypeStringToRevenueCatProductType handles mixed case values`() {
        val mixedCaseItem = "ItEm".convertSamsungIAPTypeStringToRevenueCatProductType()
        val mixedCaseSubscription = "SubScription".convertSamsungIAPTypeStringToRevenueCatProductType()

        assertThat(mixedCaseItem).isEqualTo(ProductType.INAPP)
        assertThat(mixedCaseSubscription).isEqualTo(ProductType.SUBS)
    }

    @Test
    fun `convertSamsungIAPTypeStringToRevenueCatProductType returns UNKNOWN for unexpected value`() {
        val productType = "unknown-type".convertSamsungIAPTypeStringToRevenueCatProductType()

        assertThat(productType).isEqualTo(ProductType.UNKNOWN)
    }

    @Test
    fun `convertSamsungIAPTypeStringToRevenueCatProductType returns UNKNOWN for empty string`() {
        val productType = "".convertSamsungIAPTypeStringToRevenueCatProductType()

        assertThat(productType).isEqualTo(ProductType.UNKNOWN)
    }
}
