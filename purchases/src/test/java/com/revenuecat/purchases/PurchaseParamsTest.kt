//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.models.GooglePurchasingData
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.utils.STUB_OFFERING_IDENTIFIER
import com.revenuecat.purchases.utils.stubINAPPStoreProduct
import com.revenuecat.purchases.utils.stubOfferings
import com.revenuecat.purchases.utils.stubStoreProduct
import com.revenuecat.purchases.utils.stubStoreProductWithGoogleSubscriptionPurchaseData
import com.revenuecat.purchases.utils.stubSubscriptionOption
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
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

        assertThat(purchasePackageParams.presentedOfferingContext?.offeringIdentifier).isEqualTo(STUB_OFFERING_IDENTIFIER)
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

        assertThat(purchaseProductParams.presentedOfferingContext?.offeringIdentifier).isEqualTo(STUB_OFFERING_IDENTIFIER)
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

        assertThat(purchaseProductParams.presentedOfferingContext?.offeringIdentifier).isEqualTo(STUB_OFFERING_IDENTIFIER)
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

    // region Add-Ons
    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `addOnStoreProducts with empty list correctly sets purchasingData to Subscription`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        val purchaseParams = PurchaseParams.Builder(mockk(), baseProduct)
            .addOnStoreProducts(addOnStoreProducts = emptyList())
            .build()

        validatePurchasingDataForAddOnsWithEmptyListCorrectlySetsPurchaseParams(
            purchaseParams = purchaseParams,
            baseProduct = baseProduct
        )
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `addOnPackages with empty list correctly sets purchasingData to Subscription`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        val purchaseParams = PurchaseParams.Builder(mockk(), baseProduct)
            .addOnPackages(addOnPackages = emptyList())
            .build()

        validatePurchasingDataForAddOnsWithEmptyListCorrectlySetsPurchaseParams(
            purchaseParams = purchaseParams,
            baseProduct = baseProduct
        )
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    private fun validatePurchasingDataForAddOnsWithEmptyListCorrectlySetsPurchaseParams(
        purchaseParams: PurchaseParams,
        baseProduct: StoreProduct
    ) {
        assertThat(purchaseParams.purchasingData::class).isEqualTo(GooglePurchasingData.Subscription::class)
        assertThat(purchaseParams.containsAddOnItems).isFalse()
        val subscription = purchaseParams.purchasingData as GooglePurchasingData.Subscription
        assertThat(subscription.addOnProducts).isEmpty()
        assertThat(subscription.productId).isEqualTo(baseProduct.purchasingData.productId)
        assertThat(subscription.productType).isEqualTo(ProductType.SUBS)
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `addOnStoreProducts with add-ons provided correctly sets purchasingData to Subscription`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        val addOn = stubStoreProductWithGoogleSubscriptionPurchaseData(productId = "xyz")

        val purchaseParams = PurchaseParams.Builder(mockk(), baseProduct)
            .addOnStoreProducts(addOnStoreProducts = listOf(addOn))
            .build()

        validatePurchasingDataForAddOnsWhenProvidedCorrectlySetsPurchaseParams(
            purchaseParams = purchaseParams,
            baseProduct = baseProduct,
            addOn = addOn
        )
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `addOnPackages with add-ons provided correctly sets purchasingData to Subscription`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        val addOnProduct = stubStoreProductWithGoogleSubscriptionPurchaseData(productId = "xyz")
        val aPackage = Package(
            identifier = "abc",
            packageType = PackageType.UNKNOWN,
            product = addOnProduct,
            presentedOfferingContext = mockk()
        )
        val purchaseParams = PurchaseParams.Builder(mockk(), baseProduct)
            .addOnPackages(addOnPackages = listOf(aPackage))
            .build()

        validatePurchasingDataForAddOnsWhenProvidedCorrectlySetsPurchaseParams(
            purchaseParams = purchaseParams,
            baseProduct = baseProduct,
            addOn = addOnProduct
        )
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    private fun validatePurchasingDataForAddOnsWhenProvidedCorrectlySetsPurchaseParams(
        purchaseParams: PurchaseParams,
        baseProduct: StoreProduct,
        addOn: StoreProduct
    ) {
        assertThat(purchaseParams.purchasingData::class).isEqualTo(GooglePurchasingData.Subscription::class)
        assertThat(purchaseParams.containsAddOnItems).isTrue()
        val subscription = purchaseParams.purchasingData as GooglePurchasingData.Subscription
        assertThat(subscription.productId).isEqualTo(baseProduct.purchasingData.productId)
        assertThat(subscription.productType).isEqualTo(ProductType.SUBS)
        assertThat(subscription.addOnProducts?.size).isEqualTo(1)
        val addOnProduct = subscription.addOnProducts!!.first()
        assertThat(addOnProduct).isEqualTo(addOn.purchasingData)
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `purchaseParams with add-ons sets containsAddOns to true`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        val addOn = stubStoreProductWithGoogleSubscriptionPurchaseData(productId = "xyz")

        val purchaseParamsWithStoreProducts = PurchaseParams.Builder(mockk(), baseProduct)
            .addOnStoreProducts(addOnStoreProducts = listOf(addOn))
            .build()

        assertThat(purchaseParamsWithStoreProducts.containsAddOnItems).isTrue()

        val purchaseParamsWithStorePackages = PurchaseParams.Builder(mockk(), baseProduct)
            .addOnPackages(listOf(
                Package(
                    identifier = "abc",
                    packageType = PackageType.UNKNOWN,
                    product = addOn,
                    presentedOfferingContext = mockk()
                )
            ))
            .build()

        assertThat(purchaseParamsWithStorePackages.containsAddOnItems).isTrue()
    }

    @Test
    fun `purchaseParams with no add-ons sets containsAddOns to false`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()

        val purchaseParams = PurchaseParams.Builder(mockk(), baseProduct)
            .build()

        assertThat(purchaseParams.containsAddOnItems).isFalse()
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `purchaseParams with in app product sets containsAddOns to false`() {
        val inAppProduct = stubINAPPStoreProduct(productId = "abc")

        val purchaseParams = PurchaseParams.Builder(mockk(), inAppProduct)
            .build()

        assertThat(purchaseParams.containsAddOnItems).isFalse()

        val purchaseParams2 = PurchaseParams.Builder(mockk(), inAppProduct)
            .addOnStoreProducts(listOf(inAppProduct))
            .build()

        assertThat(purchaseParams2.containsAddOnItems).isFalse()
    }
    // endregion Add-Ons
}
