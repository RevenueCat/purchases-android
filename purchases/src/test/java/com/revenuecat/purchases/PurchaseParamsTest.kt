//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.models.GooglePurchasingData
import com.revenuecat.purchases.models.GoogleSubscriptionOption
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.utils.STUB_OFFERING_IDENTIFIER
import com.revenuecat.purchases.utils.stubINAPPStoreProduct
import com.revenuecat.purchases.utils.stubOfferings
import com.revenuecat.purchases.utils.stubPricingPhase
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
    @Test
    fun `addOnSubscriptionOptions with empty list correctly sets purchasingData to Subscription`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        val purchaseParams = PurchaseParams.Builder(mockk(), baseProduct)
            .addOnSubscriptionOptions(addOnSubscriptionOptions = emptyList())
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
            addOnPurchasingData = addOn.purchasingData
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
            addOnPurchasingData = addOnProduct.purchasingData
        )
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `addOnSubscriptionOptions with add-ons provided correctly sets purchasingData to Subscription`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        val addOnSubscriptionOption: SubscriptionOption = GoogleSubscriptionOption(
            productId = "abc",
            basePlanId = "123",
            offerId = null,
            pricingPhases = listOf(stubPricingPhase()),
            tags = emptyList(),
            productDetails = mockk(),
            offerToken = "xyz",
        )
        val purchaseParams = PurchaseParams.Builder(mockk(), baseProduct)
            .addOnSubscriptionOptions(addOnSubscriptionOptions = listOf(addOnSubscriptionOption))
            .build()

        validatePurchasingDataForAddOnsWhenProvidedCorrectlySetsPurchaseParams(
            purchaseParams = purchaseParams,
            baseProduct = baseProduct,
            addOnPurchasingData = addOnSubscriptionOption.purchasingData
        )
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    private fun validatePurchasingDataForAddOnsWhenProvidedCorrectlySetsPurchaseParams(
        purchaseParams: PurchaseParams,
        baseProduct: StoreProduct,
        addOnPurchasingData: PurchasingData
    ) {
        assertThat(purchaseParams.purchasingData::class).isEqualTo(GooglePurchasingData.Subscription::class)
        assertThat(purchaseParams.containsAddOnItems).isTrue()
        val subscription = purchaseParams.purchasingData as GooglePurchasingData.Subscription
        assertThat(subscription.productId).isEqualTo(baseProduct.purchasingData.productId)
        assertThat(subscription.productType).isEqualTo(ProductType.SUBS)
        assertThat(subscription.addOnProducts?.size).isEqualTo(1)
        val addOnProduct = subscription.addOnProducts!!.first()
        assertThat(addOnProduct).isEqualTo(addOnPurchasingData)
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

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `addOnStoreProducts appends new add-ons to existing ones`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        val firstAddOn = stubStoreProductWithGoogleSubscriptionPurchaseData(productId = "addon_1")
        val secondAddOn = stubStoreProductWithGoogleSubscriptionPurchaseData(productId = "addon_2")
        val firstAddOnPurchasingData = firstAddOn.purchasingData as GooglePurchasingData
        val secondAddOnPurchasingData = secondAddOn.purchasingData as GooglePurchasingData

        val purchaseParams = PurchaseParams.Builder(mockk(), baseProduct)
            .addOnStoreProducts(addOnStoreProducts = listOf(firstAddOn))
            .addOnStoreProducts(addOnStoreProducts = listOf(secondAddOn))
            .build()

        val subscription = purchaseParams.purchasingData as GooglePurchasingData.Subscription
        assertThat(subscription.productId).isEqualTo(baseProduct.purchasingData.productId)
        val addOnProducts = subscription.addOnProducts
        assertThat(addOnProducts).isNotNull
        assertThat(addOnProducts!!)
            .containsExactly(firstAddOnPurchasingData, secondAddOnPurchasingData)
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `addOnStoreProducts appends new add-ons to existing ones after an empty call`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        val firstAddOn = stubStoreProductWithGoogleSubscriptionPurchaseData(productId = "addon_1")
        val secondAddOn = stubStoreProductWithGoogleSubscriptionPurchaseData(productId = "addon_2")
        val firstAddOnPurchasingData = firstAddOn.purchasingData as GooglePurchasingData
        val secondAddOnPurchasingData = secondAddOn.purchasingData as GooglePurchasingData

        val purchaseParams = PurchaseParams.Builder(mockk(), baseProduct)
            .addOnStoreProducts(addOnStoreProducts = emptyList())
            .addOnStoreProducts(addOnStoreProducts = listOf(firstAddOn))
            .addOnStoreProducts(addOnStoreProducts = listOf(secondAddOn))
            .build()

        val subscription = purchaseParams.purchasingData as GooglePurchasingData.Subscription
        assertThat(subscription.productId).isEqualTo(baseProduct.purchasingData.productId)
        val addOnProducts = subscription.addOnProducts
        assertThat(addOnProducts).isNotNull
        assertThat(addOnProducts!!)
            .containsExactly(firstAddOnPurchasingData, secondAddOnPurchasingData)
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `addOnPackages appends new add-ons to existing ones`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        val firstAddOnProduct = stubStoreProductWithGoogleSubscriptionPurchaseData(productId = "addon_package_1")
        val secondAddOnProduct = stubStoreProductWithGoogleSubscriptionPurchaseData(productId = "addon_package_2")
        val firstPackage = Package(
            identifier = "first_package",
            packageType = PackageType.UNKNOWN,
            product = firstAddOnProduct,
            presentedOfferingContext = mockk(),
        )
        val secondPackage = Package(
            identifier = "second_package",
            packageType = PackageType.UNKNOWN,
            product = secondAddOnProduct,
            presentedOfferingContext = mockk(),
        )

        val purchaseParams = PurchaseParams.Builder(mockk(), baseProduct)
            .addOnPackages(addOnPackages = listOf(firstPackage))
            .addOnPackages(addOnPackages = listOf(secondPackage))
            .build()

        val subscription = purchaseParams.purchasingData as GooglePurchasingData.Subscription
        assertThat(subscription.productId).isEqualTo(baseProduct.purchasingData.productId)
        val addOnProducts = subscription.addOnProducts
        assertThat(addOnProducts).isNotNull
        assertThat(addOnProducts!!)
            .containsExactly(
                firstAddOnProduct.purchasingData as GooglePurchasingData,
                secondAddOnProduct.purchasingData as GooglePurchasingData,
            )
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `addOnPackages appends new add-ons to existing ones after an empty call`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        val firstAddOnProduct = stubStoreProductWithGoogleSubscriptionPurchaseData(productId = "addon_package_1")
        val secondAddOnProduct = stubStoreProductWithGoogleSubscriptionPurchaseData(productId = "addon_package_2")
        val firstPackage = Package(
            identifier = "first_package",
            packageType = PackageType.UNKNOWN,
            product = firstAddOnProduct,
            presentedOfferingContext = mockk(),
        )
        val secondPackage = Package(
            identifier = "second_package",
            packageType = PackageType.UNKNOWN,
            product = secondAddOnProduct,
            presentedOfferingContext = mockk(),
        )

        val purchaseParams = PurchaseParams.Builder(mockk(), baseProduct)
            .addOnPackages(addOnPackages = emptyList())
            .addOnPackages(addOnPackages = listOf(firstPackage))
            .addOnPackages(addOnPackages = listOf(secondPackage))
            .build()

        val subscription = purchaseParams.purchasingData as GooglePurchasingData.Subscription
        assertThat(subscription.productId).isEqualTo(baseProduct.purchasingData.productId)
        val addOnProducts = subscription.addOnProducts
        assertThat(addOnProducts).isNotNull
        assertThat(addOnProducts!!)
            .containsExactly(
                firstAddOnProduct.purchasingData as GooglePurchasingData,
                secondAddOnProduct.purchasingData as GooglePurchasingData,
            )
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `addOnSubscriptionOptions appends new add-ons to existing ones`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        val firstOption = GoogleSubscriptionOption(
            productId = "productID1",
            basePlanId = "basePlan1",
            offerId = null,
            pricingPhases = listOf(stubPricingPhase()),
            tags = emptyList(),
            productDetails = mockk(),
            offerToken = "token1"
        )
        val secondOption = GoogleSubscriptionOption(
            productId = "productID2",
            basePlanId = "basePlan2",
            offerId = null,
            pricingPhases = listOf(stubPricingPhase()),
            tags = emptyList(),
            productDetails = mockk(),
            offerToken = "token2"
        )

        val purchaseParams = PurchaseParams.Builder(mockk(), baseProduct)
            .addOnSubscriptionOptions(addOnSubscriptionOptions = listOf(firstOption))
            .addOnSubscriptionOptions(addOnSubscriptionOptions = listOf(secondOption))
            .build()

        val subscription = purchaseParams.purchasingData as GooglePurchasingData.Subscription
        assertThat(subscription.productId).isEqualTo(baseProduct.purchasingData.productId)
        val addOnProducts = subscription.addOnProducts
        assertThat(addOnProducts).isNotNull
        assertThat(addOnProducts!!)
            .containsExactly(
                firstOption.purchasingData as GooglePurchasingData,
                secondOption.purchasingData as GooglePurchasingData,
            )
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `addOnSubscriptionOptions appends new add-ons to existing ones after an empty call`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        val firstOption = GoogleSubscriptionOption(
            productId = "productID1",
            basePlanId = "basePlan1",
            offerId = null,
            pricingPhases = listOf(stubPricingPhase()),
            tags = emptyList(),
            productDetails = mockk(),
            offerToken = "token1"
        )
        val secondOption = GoogleSubscriptionOption(
            productId = "productID2",
            basePlanId = "basePlan2",
            offerId = null,
            pricingPhases = listOf(stubPricingPhase()),
            tags = emptyList(),
            productDetails = mockk(),
            offerToken = "token2"
        )

        val purchaseParams = PurchaseParams.Builder(mockk(), baseProduct)
            .addOnSubscriptionOptions(addOnSubscriptionOptions = emptyList())
            .addOnSubscriptionOptions(addOnSubscriptionOptions = listOf(firstOption))
            .addOnSubscriptionOptions(addOnSubscriptionOptions = listOf(secondOption))
            .build()

        val subscription = purchaseParams.purchasingData as GooglePurchasingData.Subscription
        assertThat(subscription.productId).isEqualTo(baseProduct.purchasingData.productId)
        val addOnProducts = subscription.addOnProducts
        assertThat(addOnProducts).isNotNull
        assertThat(addOnProducts!!)
            .containsExactly(
                firstOption.purchasingData as GooglePurchasingData,
                secondOption.purchasingData as GooglePurchasingData,
            )
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `mixing addOnSubscriptionOptions, addOnPackages, and addOnStoreProducts appends add-ons in order`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        val storeAddOn = stubStoreProductWithGoogleSubscriptionPurchaseData(productId = "addon_store")
        val packageAddOnProduct = stubStoreProductWithGoogleSubscriptionPurchaseData(productId = "addon_package")
        val packageAddOn = Package(
            identifier = "addon_package_identifier",
            packageType = PackageType.UNKNOWN,
            product = packageAddOnProduct,
            presentedOfferingContext = mockk(),
        )
        val subscriptionOption = GoogleSubscriptionOption(
            productId = "productID1",
            basePlanId = "basePlan1",
            offerId = null,
            pricingPhases = listOf(stubPricingPhase()),
            tags = emptyList(),
            productDetails = mockk(),
            offerToken = "token1"
        )

        val purchaseParams = PurchaseParams.Builder(mockk(), baseProduct)
            .addOnSubscriptionOptions(addOnSubscriptionOptions = listOf(subscriptionOption))
            .addOnStoreProducts(addOnStoreProducts = listOf(storeAddOn))
            .addOnPackages(addOnPackages = listOf(packageAddOn))
            .build()

        val subscription = purchaseParams.purchasingData as GooglePurchasingData.Subscription
        assertThat(subscription.productId).isEqualTo(baseProduct.purchasingData.productId)
        val addOnProducts = subscription.addOnProducts
        assertThat(addOnProducts).isNotNull
        val storePurchasingData = storeAddOn.purchasingData as GooglePurchasingData
        val packagePurchasingData = packageAddOnProduct.purchasingData as GooglePurchasingData
        val optionPurchasingData = subscriptionOption.purchasingData as GooglePurchasingData
        assertThat(addOnProducts!!)
            .containsExactly(optionPurchasingData, storePurchasingData, packagePurchasingData)
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
