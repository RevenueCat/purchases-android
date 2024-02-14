//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.utils.STUB_OFFERING_IDENTIFIER
import com.revenuecat.purchases.utils.stubOfferings
import com.revenuecat.purchases.utils.stubStoreProduct
import com.revenuecat.purchases.utils.stubSubscriptionOption
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PurchaseParamsTest {

    @Test
    fun `Initializing with Package sets proper presentedOfferingIdentifier`() {
        val storeProduct = stubStoreProduct("abc")
        val (_, offerings) = stubOfferings(storeProduct)
        val purchasePackageParams = PurchaseParams.Builder(
            mockk(),
            offerings[STUB_OFFERING_IDENTIFIER]!!.monthly!!
        ).build()

        assertThat(purchasePackageParams.presentedOfferingContext.offeringIdentifier).isEqualTo(STUB_OFFERING_IDENTIFIER)
    }

    @Test
    fun `Initializing with StoreProduct sets proper presentedOfferingIdentifier`() {
        val storeProduct = stubStoreProduct(
            productId = "abc",
            presentedOfferingContext = PresentedOfferingContext(STUB_OFFERING_IDENTIFIER),
        )
        val (_, offerings) = stubOfferings(storeProduct)
        val purchaseProductParams = PurchaseParams.Builder(
            mockk(),
            offerings[STUB_OFFERING_IDENTIFIER]!!.monthly!!.product
        ).build()

        assertThat(purchaseProductParams.presentedOfferingContext.offeringIdentifier).isEqualTo(STUB_OFFERING_IDENTIFIER)
    }

    @Test
    fun `Initializing with TestStoreProduct throws error`() {
        val storeProduct = TestStoreProduct(
            "id",
            "name",
            "title",
            "description",
            Price("$1.99", 1_990_000, "US"),
            Period(1, Period.Unit.MONTH, "P1M")
        )
        try {
            PurchaseParams.Builder(mockk(), storeProduct).build()
            fail("Expected error")
        } catch (e: PurchasesException) {
            assertThat(e.code).isEqualTo(PurchasesErrorCode.ProductNotAvailableForPurchaseError)
        }
    }

    @Test
    fun `Initializing with SubscriptionOption sets proper presentedOfferingIdentifier`() {
        val storeProduct = stubStoreProduct(
            productId = "abc",
            presentedOfferingContext = PresentedOfferingContext(STUB_OFFERING_IDENTIFIER),
        )
        val (_, offerings) = stubOfferings(storeProduct)
        val defaultOption = offerings[STUB_OFFERING_IDENTIFIER]!!.monthly!!.product.defaultOption!!
        val purchaseProductParams = PurchaseParams.Builder(
            mockk(),
            defaultOption
        ).build()

        assertThat(purchaseProductParams.presentedOfferingContext.offeringIdentifier).isEqualTo(STUB_OFFERING_IDENTIFIER)
    }

    @Test
    fun `Initializing with Package sets proper purchasingData`() {
        val storeProduct = stubStoreProduct("abc")
        val (_, offerings) = stubOfferings(storeProduct)
        val packageToPurchase = offerings[STUB_OFFERING_IDENTIFIER]!!.monthly!!
        val purchasePackageParams = PurchaseParams.Builder(
            mockk(),
            packageToPurchase
        ).build()

        val expectedPurchasingData = packageToPurchase.product.purchasingData
        assertThat(purchasePackageParams.purchasingData).isEqualTo(expectedPurchasingData)
    }

    @Test
    fun `Initializing with Package containing TestStoreProduct throws error`() {
        val storeProduct = TestStoreProduct(
            "id",
            "name",
            "title",
            "description",
            Price("$1.99", 1_990_000, "US"),
            Period(1, Period.Unit.MONTH, "P1M")
        )
        val (_, offerings) = stubOfferings(storeProduct)
        try {
            PurchaseParams.Builder(mockk(), offerings[STUB_OFFERING_IDENTIFIER]!!.monthly!!).build()
            fail("Expected error")
        } catch (e: PurchasesException) {
            assertThat(e.code).isEqualTo(PurchasesErrorCode.ProductNotAvailableForPurchaseError)
        }
    }

    @Test
    fun `Initializing with product sets proper purchasingData`() {
        val storeProduct = stubStoreProduct("abc")
        val purchasePackageParams = PurchaseParams.Builder(
            mockk(),
            storeProduct
        ).build()

        val expectedPurchasingData = storeProduct.purchasingData
        assertThat(purchasePackageParams.purchasingData).isEqualTo(expectedPurchasingData)
    }

    @Test
    fun `Initializing with option sets proper purchasingData`() {
        val basePlanSubscriptionOption = stubSubscriptionOption("base-plan-purchase-option", "abc")
        val purchasePackageParams = PurchaseParams.Builder(
            mockk(),
            basePlanSubscriptionOption
        ).build()

        val expectedPurchasingData = basePlanSubscriptionOption.purchasingData
        assertThat(purchasePackageParams.purchasingData).isEqualTo(expectedPurchasingData)
    }
}
