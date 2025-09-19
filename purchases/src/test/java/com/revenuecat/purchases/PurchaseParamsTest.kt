//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.models.GooglePurchasingData
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.utils.STUB_OFFERING_IDENTIFIER
import com.revenuecat.purchases.utils.stubOfferings
import com.revenuecat.purchases.utils.stubStoreProduct
import com.revenuecat.purchases.utils.stubStoreProductWithGoogleSubscriptionPurchaseData
import com.revenuecat.purchases.utils.stubSubscriptionOption
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
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
    fun `addOnStoreProducts with empty list doesn't throw`() {
        PurchaseParams.Builder(mockk(), stubStoreProductWithGoogleSubscriptionPurchaseData())
            .addOnStoreProducts(addOnStoreProducts = emptyList())
            .build()
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `addOnPackages with empty list doesn't throw`() {
        PurchaseParams.Builder(mockk(), stubStoreProductWithGoogleSubscriptionPurchaseData())
            .addOnPackages(addOnPackages = emptyList())
            .build()
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `calling addOnStoreProducts when base product isn't a google subscription throws`() {
        val exception = catchThrowable {
            PurchaseParams.Builder(
                mockk(),
                stubStoreProduct(productId = "abc")
            )
                .addOnStoreProducts(addOnStoreProducts = emptyList())
                .build()
        }

        validateExceptionForAddOnsWhenBaseProductIsntGoogleSubThrows(exception)
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `calling addOnPackages when base product isn't a google subscription throws`() {
        val exception = catchThrowable {
            PurchaseParams.Builder(mockk(), stubStoreProduct(productId = "abc"))
                .addOnPackages(addOnPackages = emptyList())
                .build()
        }

        validateExceptionForAddOnsWhenBaseProductIsntGoogleSubThrows(exception)
    }

    private fun validateExceptionForAddOnsWhenBaseProductIsntGoogleSubThrows(exception: Throwable) {
        assertThat(exception::class).isEqualTo(PurchasesException::class)
        val purchasesException = exception as PurchasesException
        assertThat(purchasesException.code).isEqualTo(PurchasesErrorCode.PurchaseInvalidError)
        assertThat(purchasesException.underlyingErrorMessage)
            .isEqualTo("Add-ons are currently only supported for Google subscriptions.")
        assertThat(purchasesException.error.code).isEqualTo(PurchasesErrorCode.PurchaseInvalidError)
        assertThat(purchasesException.error.message).isEqualTo(PurchasesErrorCode.PurchaseInvalidError.description)
        assertThat(purchasesException.error.underlyingErrorMessage)
            .isEqualTo("Add-ons are currently only supported for Google subscriptions.")
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `addOnStoreProducts with empty list correctly sets purchasingData to ProductWithAddOns`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        val purchaseParams = PurchaseParams.Builder(mockk(), baseProduct)
            .addOnStoreProducts(addOnStoreProducts = emptyList())
            .build()

        validatePurchasingDataForAddOnsWithEmptyListCorrectlySetsPurchasingData(
            purchasingData = purchaseParams.purchasingData,
            baseProduct = baseProduct
        )
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `addOnPackages with empty list correctly sets purchasingData to ProductWithAddOns`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        val purchaseParams = PurchaseParams.Builder(mockk(), baseProduct)
            .addOnPackages(addOnPackages = emptyList())
            .build()

        validatePurchasingDataForAddOnsWithEmptyListCorrectlySetsPurchasingData(
            purchasingData = purchaseParams.purchasingData,
            baseProduct = baseProduct
        )
    }

    private fun validatePurchasingDataForAddOnsWithEmptyListCorrectlySetsPurchasingData(
        purchasingData: PurchasingData,
        baseProduct: StoreProduct
    ) {
        assertThat(purchasingData::class).isEqualTo(GooglePurchasingData.ProductWithAddOns::class)
        val productWithAddOns = purchasingData as GooglePurchasingData.ProductWithAddOns
        assertThat(productWithAddOns.addOnProducts).isEmpty()
        assertThat(productWithAddOns.productId).isEqualTo(baseProduct.purchasingData.productId)
        assertThat(productWithAddOns.productType).isEqualTo(ProductType.SUBS)
        assertThat(productWithAddOns.baseProduct.productId).isEqualTo(baseProduct.purchasingData.productId)
        assertThat(productWithAddOns.baseProduct.productType).isEqualTo(ProductType.SUBS)
        assertThat(productWithAddOns.replacementMode).isEqualTo(GoogleReplacementMode.WITHOUT_PRORATION)
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `addOnStoreProducts with add-ons provided correctly sets purchasingData to ProductWithAddOns`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        val addOn = stubStoreProductWithGoogleSubscriptionPurchaseData(productId = "xyz")

        val purchaseParams = PurchaseParams.Builder(mockk(), baseProduct)
            .addOnStoreProducts(addOnStoreProducts = listOf(addOn))
            .build()

        validatePurchasingDataForAddOnsWhenProvidedCorrectlySetsPurchasingData(
            purchasingData = purchaseParams.purchasingData,
            baseProduct = baseProduct,
            addOn = addOn
        )
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `addOnPackages with add-ons provided correctly sets purchasingData to ProductWithAddOns`() {
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

        validatePurchasingDataForAddOnsWhenProvidedCorrectlySetsPurchasingData(
            purchasingData = purchaseParams.purchasingData,
            baseProduct = baseProduct,
            addOn = addOnProduct
        )
    }

    private fun validatePurchasingDataForAddOnsWhenProvidedCorrectlySetsPurchasingData(
        purchasingData: PurchasingData,
        baseProduct: StoreProduct,
        addOn: StoreProduct
    ) {
        assertThat(purchasingData::class).isEqualTo(GooglePurchasingData.ProductWithAddOns::class)
        val productWithAddOns = purchasingData as GooglePurchasingData.ProductWithAddOns
        assertThat(productWithAddOns.productId).isEqualTo(baseProduct.purchasingData.productId)
        assertThat(productWithAddOns.productType).isEqualTo(ProductType.SUBS)
        assertThat(productWithAddOns.baseProduct.productId).isEqualTo(baseProduct.purchasingData.productId)
        assertThat(productWithAddOns.baseProduct.productType).isEqualTo(ProductType.SUBS)
        assertThat(productWithAddOns.replacementMode).isEqualTo(GoogleReplacementMode.WITHOUT_PRORATION)
        assertThat(productWithAddOns.addOnProducts.size).isEqualTo(1)
        val addOnProduct = productWithAddOns.addOnProducts.first()
        assertThat(addOnProduct).isEqualTo(addOn.purchasingData)
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `addOnStoreProducts throws if more than 49 add-ons are provided`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        val addOn = stubStoreProductWithGoogleSubscriptionPurchaseData(productId = "xyz")

        val exception = catchThrowable {
            PurchaseParams.Builder(mockk(), baseProduct)
                .addOnStoreProducts(addOnStoreProducts = List(size = 50) { addOn })
                .build()
        }
        validateExceptionForAddOnsThrowIfMoreThan49AddOnsAreProvided(exception)
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `addOnPackages throws if more than 49 add-ons are provided`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        val addOnProduct = stubStoreProductWithGoogleSubscriptionPurchaseData(productId = "xyz")
        val aPackage = Package(
            identifier = "abc",
            packageType = PackageType.UNKNOWN,
            product = addOnProduct,
            presentedOfferingContext = mockk()
        )

        val exception = catchThrowable {
            PurchaseParams.Builder(mockk(), baseProduct)
                .addOnPackages(addOnPackages = List(size = 50) { aPackage })
                .build()
        }
        validateExceptionForAddOnsThrowIfMoreThan49AddOnsAreProvided(exception)
    }

    private fun validateExceptionForAddOnsThrowIfMoreThan49AddOnsAreProvided(exception: Throwable) {
        assertThat(exception::class).isEqualTo(PurchasesException::class)
        val purchasesException = exception as PurchasesException
        assertThat(purchasesException.code).isEqualTo(PurchasesErrorCode.PurchaseInvalidError)
        assertThat(purchasesException.underlyingErrorMessage)
            .isEqualTo("Multi-line purchases cannot contain more than 50 products (1 base + 49 add-ons).")
        assertThat(purchasesException.error.code).isEqualTo(PurchasesErrorCode.PurchaseInvalidError)
        assertThat(purchasesException.error.message).isEqualTo(PurchasesErrorCode.PurchaseInvalidError.description)
        assertThat(purchasesException.error.underlyingErrorMessage)
            .isEqualTo("Multi-line purchases cannot contain more than 50 products (1 base + 49 add-ons).")
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `addOnStoreProducts throws if add-ons with different periods are provided`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData(period = Period(1, Period.Unit.MONTH, "P1M"))
        val monthlyAddOn = stubStoreProductWithGoogleSubscriptionPurchaseData(productId = "monthly_addon",
            period = Period(1, Period.Unit.MONTH, "P1M"))
        val yearlyAddOn = stubStoreProductWithGoogleSubscriptionPurchaseData(productId = "yearly_addon",
            period = Period(1, Period.Unit.YEAR, "P1Y"))

        val exception = catchThrowable {
            PurchaseParams.Builder(mockk(), baseProduct)
                .addOnStoreProducts(addOnStoreProducts = listOf(monthlyAddOn, yearlyAddOn))
                .build()
        }

        validateExceptionForAddOnsThrowIfDifferentPeriodsAreProvided(exception)
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `addOnPackages throws if add-ons with different periods are provided`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData(period = Period(1, Period.Unit.MONTH, "P1M"))
        val monthlyAddOnProduct = stubStoreProductWithGoogleSubscriptionPurchaseData(productId = "monthly_addon",
            period = Period(1, Period.Unit.MONTH, "P1M"))
        val yearlyAddOnProduct = stubStoreProductWithGoogleSubscriptionPurchaseData(productId = "yearly_addon",
            period = Period(1, Period.Unit.YEAR, "P1Y"))
        val monthlyPackage = Package(
            identifier = "monthly_package",
            packageType = PackageType.UNKNOWN,
            product = monthlyAddOnProduct,
            presentedOfferingContext = mockk()
        )
        val yearlyPackage = Package(
            identifier = "yearly_package",
            packageType = PackageType.UNKNOWN,
            product = yearlyAddOnProduct,
            presentedOfferingContext = mockk()
        )

        val exception = catchThrowable {
            PurchaseParams.Builder(mockk(), baseProduct)
                .addOnPackages(addOnPackages = listOf(monthlyPackage, yearlyPackage))
                .build()
        }

        validateExceptionForAddOnsThrowIfDifferentPeriodsAreProvided(exception)
    }

    private fun validateExceptionForAddOnsThrowIfDifferentPeriodsAreProvided(exception: Throwable) {
        assertThat(exception::class).isEqualTo(PurchasesException::class)
        val purchasesException = exception as PurchasesException
        assertThat(purchasesException.code).isEqualTo(PurchasesErrorCode.PurchaseInvalidError)
        assertThat(purchasesException.underlyingErrorMessage)
            .isEqualTo("All items in a multi-line purchase must have the same billing period.")
        assertThat(purchasesException.error.code).isEqualTo(PurchasesErrorCode.PurchaseInvalidError)
        assertThat(purchasesException.error.message).isEqualTo(PurchasesErrorCode.PurchaseInvalidError.description)
        assertThat(purchasesException.error.underlyingErrorMessage)
            .isEqualTo("All items in a multi-line purchase must have the same billing period.")
    }
    // endregion Add-Ons
}
