//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.utils.STUB_OFFERING_IDENTIFIER
import com.revenuecat.purchases.utils.stubOfferings
import com.revenuecat.purchases.utils.stubStoreProduct
import com.revenuecat.purchases.utils.stubSubscriptionOption
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.exp

// endregion
@RunWith(AndroidJUnit4::class)
class PurchaseParamsTest {

    @Test
    fun `Initializing with Package sets proper presentedOfferingIdentifier`() {
        val storeProduct = stubStoreProduct("abc")
        val (_, offerings) = stubOfferings(storeProduct)
        val purchasePackageParams = PurchaseParams.Builder(
            offerings[STUB_OFFERING_IDENTIFIER]!!.monthly!!,
            mockk()
        ).build()

        assertThat(purchasePackageParams.presentedOfferingIdentifier).isEqualTo(STUB_OFFERING_IDENTIFIER)
    }

    @Test
    fun `Initializing with Package sets proper purchasingData`() {
        val storeProduct = stubStoreProduct("abc")
        val (_, offerings) = stubOfferings(storeProduct)
        val packageToPurchase = offerings[STUB_OFFERING_IDENTIFIER]!!.monthly!!
        val purchasePackageParams = PurchaseParams.Builder(
            packageToPurchase,
            mockk<Activity>()
        ).build()

        val expectedPurchasingData = packageToPurchase.product.purchasingData
        assertThat(purchasePackageParams.purchasingData).isEqualTo(expectedPurchasingData)
    }

    @Test
    fun `Initializing with product sets proper purchasingData`() {
        val storeProduct = stubStoreProduct("abc")
        val purchasePackageParams = PurchaseParams.Builder(
            storeProduct,
            mockk()
        ).build()

        val expectedPurchasingData = storeProduct.purchasingData
        assertThat(purchasePackageParams.purchasingData).isEqualTo(expectedPurchasingData)
    }

    @Test
    fun `Initializing with option sets proper purchasingData`() {
        val basePlanSubscriptionOption = stubSubscriptionOption("base-plan-purchase-option", "abc")
        val purchasePackageParams = PurchaseParams.Builder(
            basePlanSubscriptionOption,
            mockk()
        ).build()

        val expectedPurchasingData = basePlanSubscriptionOption.purchasingData
        assertThat(purchasePackageParams.purchasingData).isEqualTo(expectedPurchasingData)
    }
}
