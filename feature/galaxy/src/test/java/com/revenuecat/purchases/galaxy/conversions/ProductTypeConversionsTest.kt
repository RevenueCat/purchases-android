package com.revenuecat.purchases.galaxy.conversions

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ProductType
import com.samsung.android.sdk.iap.lib.constants.HelperDefine
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductTypeConversionsTest {

    @Test
    fun `Samsung item type maps to INAPP`() {
        val productType = HelperDefine.PRODUCT_TYPE_ITEM.createRevenueCatProductTypeFromSamsungIAPTypeString()
        assertThat(productType).isEqualTo(ProductType.INAPP)
    }

    @Test
    fun `Samsung subscription type maps to SUBS`() {
        val productType = HelperDefine.PRODUCT_TYPE_SUBSCRIPTION.createRevenueCatProductTypeFromSamsungIAPTypeString()
        assertThat(productType).isEqualTo(ProductType.SUBS)
    }

    @Test
    fun `Samsung product types are matched case-insensitively`() {
        assertThat("ITEM".createRevenueCatProductTypeFromSamsungIAPTypeString()).isEqualTo(ProductType.INAPP)
        assertThat("ItEm".createRevenueCatProductTypeFromSamsungIAPTypeString()).isEqualTo(ProductType.INAPP)
        assertThat("SUBSCRIPTION".createRevenueCatProductTypeFromSamsungIAPTypeString()).isEqualTo(ProductType.SUBS)
        assertThat("SubScription".createRevenueCatProductTypeFromSamsungIAPTypeString()).isEqualTo(ProductType.SUBS)
    }

    @Test
    fun `unexpected Samsung product types map to UNKNOWN`() {
        assertThat("".createRevenueCatProductTypeFromSamsungIAPTypeString()).isEqualTo(ProductType.UNKNOWN)
    }
}
