package com.revenuecat.purchases.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.models.GooglePurchasingData
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.StoreProduct
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.DefaultAsserter.fail

@RunWith(AndroidJUnit4::class)
class PurchaseParamsValidatorTest {

    @Test
    fun `purchaseParams with storeProduct passes validation`() {
        val purchaseParams = PurchaseParams.Builder(
            mockk(),
            stubStoreProduct(productId = "abc")
        )
            .build()

        val validationResult = validator.validate(purchaseParams)
        assertThat(validationResult).isInstanceOf(Result.Success::class.java)
    }

    @Test
    fun `purchaseParams with package passes validation`() {
        val mockProduct = stubStoreProduct(productId = "abc")
        val mockPackage = mockk<Package>()
        every { mockPackage.product } returns mockProduct
        every { mockPackage.presentedOfferingContext } returns mockk()
        val purchaseParams = PurchaseParams.Builder(
            mockk(),
            packageToPurchase = mockPackage
        )
            .build()

        val validationResult = validator.validate(purchaseParams)
        assertThat(validationResult).isInstanceOf(Result.Success::class.java)
    }

    @Test
    fun `purchaseParams with subscriptionOption passes validation`() {
        val purchaseParams = PurchaseParams.Builder(
            mockk(),
            subscriptionOption = stubSubscriptionOption(id = "123", productId = "abc")
        )
            .build()

        val validationResult = validator.validate(purchaseParams)
        assertThat(validationResult).isInstanceOf(Result.Success::class.java)
    }

    @Test
    fun `purchaseParams with oldProductId passes validation`() {
        val purchaseParams = PurchaseParams.Builder(
            mockk(),
            subscriptionOption = stubSubscriptionOption(id = "123", productId = "abc")
        )
            .oldProductId("123")
            .build()

        val validationResult = validator.validate(purchaseParams)
        assertThat(validationResult).isInstanceOf(Result.Success::class.java)
    }

    @Test
    fun `purchaseParams with GoogleReplacementMode passes validation`() {
        val purchaseParams = PurchaseParams.Builder(
            mockk(),
            subscriptionOption = stubSubscriptionOption(id = "123", productId = "abc")
        )
            .googleReplacementMode(GoogleReplacementMode.WITHOUT_PRORATION)
            .build()

        val validationResult = validator.validate(purchaseParams)
        assertThat(validationResult).isInstanceOf(Result.Success::class.java)
    }

    @Test
    fun `purchaseParams with oldProductId and GoogleReplacementMode passes validation`() {
        val purchaseParams = PurchaseParams.Builder(
            mockk(),
            subscriptionOption = stubSubscriptionOption(id = "123", productId = "abc")
        )
            .oldProductId("123")
            .googleReplacementMode(GoogleReplacementMode.WITHOUT_PRORATION)
            .build()

        val validationResult = validator.validate(purchaseParams)
        assertThat(validationResult).isInstanceOf(Result.Success::class.java)
    }

    @Test
    fun `purchaseParams with personalized price passes validation`() {
        for (personalizedPrice in listOf(true, false)) {
            val purchaseParams = PurchaseParams.Builder(
                mockk(),
                subscriptionOption = stubSubscriptionOption(id = "123", productId = "abc")
            )
                .isPersonalizedPrice(personalizedPrice)
                .build()

            val validationResult = validator.validate(purchaseParams)
            assertThat(validationResult).isInstanceOf(Result.Success::class.java)
        }
    }

    @Test
    fun `purchaseParams with presentedOfferingContext passes validation`() {
        val purchaseParams = PurchaseParams.Builder(
            mockk(),
            subscriptionOption = stubSubscriptionOption(id = "123", productId = "abc")
        )
            .presentedOfferingContext(mockk())
            .build()

        val validationResult = validator.validate(purchaseParams)
        assertThat(validationResult).isInstanceOf(Result.Success::class.java)
    }

    // region Add-On Validation

    private val validator = PurchaseParamsValidator()

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `purchaseParams with add-ons list provided and base item that isn't a google subscription fails validation`() {
        // We need to mock the purchase params since the addOnProducts() function ignores products
        // that aren't GooglePurchasingData.Subscription
        val mockPurchasesParams = mockk<PurchaseParams>()
        val mockPurchasingData = mockk<GooglePurchasingData.InAppProduct>()
        every { mockPurchasingData.productId } returns "123"
        every { mockPurchasesParams.purchasingData } returns mockPurchasingData
        every { mockPurchasesParams.containsAddOnItems } returns true

        val validationResult = validator.validate(mockPurchasesParams)
        if (validationResult is Result.Error) {
            val purchasesError = validationResult.value
            assertThat(purchasesError.code).isEqualTo(PurchasesErrorCode.PurchaseInvalidError)
            assertThat(purchasesError.underlyingErrorMessage)
                .isEqualTo("Add-ons are currently only supported for Google subscriptions.")
        } else {
            fail("Validation result should be error")
        }
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `purchaseParams with add-on item that isn't a google subscription fails validation`() {
        // We need to mock the purchase params since the addOnProducts() function ignores products
        // that aren't GooglePurchasingData.Subscription
        val mockPurchasesParams = mockk<PurchaseParams>()
        val mockPurchasingData = mockk<GooglePurchasingData.Subscription>()
        every { mockPurchasingData.addOnProducts } returns listOf(mockk<GooglePurchasingData.InAppProduct>())
        every { mockPurchasingData.productId } returns "123"


        every { mockPurchasesParams.purchasingData } returns mockPurchasingData
        every { mockPurchasesParams.containsAddOnItems } returns true

        val validationResult = validator.validate(mockPurchasesParams)
        if (validationResult is Result.Error) {
            val purchasesError = validationResult.value
            assertThat(purchasesError.code).isEqualTo(PurchasesErrorCode.PurchaseInvalidError)
            assertThat(purchasesError.underlyingErrorMessage)
                .isEqualTo("Add-ons are currently only supported for Google subscriptions.")
        } else {
            fail("Validation result should be error")
        }
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `purchaseParams flagged with add-ons but null add-on list passes validation`() {
        // Shouldn't happen, but good to ensure that we don't crash
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        val baseSubscriptionPurchasingData = baseProduct.purchasingData as GooglePurchasingData.Subscription
        val subscriptionPurchasingData = GooglePurchasingData.Subscription(
            productId = baseSubscriptionPurchasingData.productId,
            optionId = baseSubscriptionPurchasingData.optionId,
            productDetails = baseSubscriptionPurchasingData.productDetails,
            token = baseSubscriptionPurchasingData.token,
            billingPeriod = baseSubscriptionPurchasingData.billingPeriod,
            addOnProducts = null
        )

        val purchaseParams = mockk<PurchaseParams>()
        every { purchaseParams.purchasingData } returns subscriptionPurchasingData
        every { purchaseParams.containsAddOnItems } returns true

        val validationResult = validator.validate(purchaseParams)
        assertThat(validationResult).isInstanceOf(Result.Success::class.java)
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `purchaseParams flagged with add-ons but empty add-on list passes validation`() {
        // Shouldn't happen, but good to ensure that we don't crash
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        val baseSubscriptionPurchasingData = baseProduct.purchasingData as GooglePurchasingData.Subscription
        val subscriptionPurchasingData = GooglePurchasingData.Subscription(
            productId = baseSubscriptionPurchasingData.productId,
            optionId = baseSubscriptionPurchasingData.optionId,
            productDetails = baseSubscriptionPurchasingData.productDetails,
            token = baseSubscriptionPurchasingData.token,
            billingPeriod = baseSubscriptionPurchasingData.billingPeriod,
            addOnProducts = emptyList()
        )

        val purchaseParams = mockk<PurchaseParams>()
        every { purchaseParams.purchasingData } returns subscriptionPurchasingData
        every { purchaseParams.containsAddOnItems } returns true

        val validationResult = validator.validate(purchaseParams)
        assertThat(validationResult).isInstanceOf(Result.Success::class.java)
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `purchaseParams with Google Subscription base item and empty add-ons list passes validation`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        val purchaseParams = PurchaseParams.Builder(mockk(), baseProduct)
            .addOnStoreProducts(addOnStoreProducts = emptyList())
            .build()

        val validationResult = validator.validate(purchaseParams)
        assertThat(validationResult).isInstanceOf(Result.Success::class.java)
    }

    @Test
    fun `purchaseParams with Google Subscription base item and null add-ons list passes validation`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        val purchaseParams = PurchaseParams.Builder(mockk(), baseProduct)
            .build()

        val validationResult = validator.validate(purchaseParams)
        assertThat(validationResult).isInstanceOf(Result.Success::class.java)
    }


    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `purchaseParams with Google Subscription base item and valid add-ons passes validation`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        val addOn = stubStoreProductWithGoogleSubscriptionPurchaseData(productId = "xyz")
        val purchaseParams = PurchaseParams.Builder(mockk(), baseProduct)
            .addOnStoreProducts(addOnStoreProducts = listOf(addOn))
            .build()

        val validationResult = validator.validate(purchaseParams)
        assertThat(validationResult).isInstanceOf(Result.Success::class.java)
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `purchaseParams with Google Subscription base item and 49 add-ons passes validation`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        val addOns = (1..PurchaseParamsValidator.MAX_NUMBER_OF_ADD_ON_PRODUCTS).map {
            stubStoreProductWithGoogleSubscriptionPurchaseData(productId = "addon_$it")
        }

        val purchaseParams = PurchaseParams.Builder(mockk(), baseProduct)
            .addOnStoreProducts(addOnStoreProducts = addOns)
            .build()

        val validationResult = validator.validate(purchaseParams)
        assertThat(validationResult).isInstanceOf(Result.Success::class.java)
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `purchaseParams with Google Subscription base item and more than 49 add-ons fails validation`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        val addOns = ArrayList<StoreProduct>()
        for (i in 1..50) {
            addOns.add(stubStoreProductWithGoogleSubscriptionPurchaseData(productId = "xyz_$i"))
        }


        val purchaseParams = PurchaseParams.Builder(mockk(), baseProduct)
            .addOnStoreProducts(addOnStoreProducts = addOns)
            .build()

        val validationResult = validator.validate(purchaseParams)
        if (validationResult is Result.Error) {
            val purchasesError = validationResult.value
            assertThat(purchasesError.code).isEqualTo(PurchasesErrorCode.PurchaseInvalidError)
            assertThat(purchasesError.underlyingErrorMessage)
                .isEqualTo("Multi-line purchases cannot contain more than 50 products (1 base + 49 add-ons).")
        } else {
            fail("Validation result should be error")
        }
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `purchaseParams with null base billing period and add-ons with billing period passes validation`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        val baseSubscriptionPurchasingData = baseProduct.purchasingData as GooglePurchasingData.Subscription
        val addOnPurchasingData = (stubStoreProductWithGoogleSubscriptionPurchaseData(productId = "addon_product")
            .purchasingData as GooglePurchasingData.Subscription)

        val subscriptionPurchasingData = GooglePurchasingData.Subscription(
            productId = baseSubscriptionPurchasingData.productId,
            optionId = baseSubscriptionPurchasingData.optionId,
            productDetails = baseSubscriptionPurchasingData.productDetails,
            token = baseSubscriptionPurchasingData.token,
            billingPeriod = null,
            addOnProducts = listOf(
                GooglePurchasingData.Subscription(
                    productId = addOnPurchasingData.productId,
                    optionId = addOnPurchasingData.optionId,
                    productDetails = addOnPurchasingData.productDetails,
                    token = addOnPurchasingData.token,
                    billingPeriod = addOnPurchasingData.billingPeriod,
                    addOnProducts = emptyList()
                )
            )
        )

        val purchaseParams = mockk<PurchaseParams>()
        every { purchaseParams.purchasingData } returns subscriptionPurchasingData
        every { purchaseParams.containsAddOnItems } returns true

        val validationResult = validator.validate(purchaseParams)
        assertThat(validationResult).isInstanceOf(Result.Success::class.java)
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `purchaseParams with add-ons missing billing periods passes validation`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        val baseSubscriptionPurchasingData = baseProduct.purchasingData as GooglePurchasingData.Subscription
        val addOnPurchasingData = (stubStoreProductWithGoogleSubscriptionPurchaseData(productId = "addon_product")
            .purchasingData as GooglePurchasingData.Subscription)

        val subscriptionPurchasingData = GooglePurchasingData.Subscription(
            productId = baseSubscriptionPurchasingData.productId,
            optionId = baseSubscriptionPurchasingData.optionId,
            productDetails = baseSubscriptionPurchasingData.productDetails,
            token = baseSubscriptionPurchasingData.token,
            billingPeriod = baseSubscriptionPurchasingData.billingPeriod,
            addOnProducts = listOf(
                GooglePurchasingData.Subscription(
                    productId = addOnPurchasingData.productId,
                    optionId = addOnPurchasingData.optionId,
                    productDetails = addOnPurchasingData.productDetails,
                    token = addOnPurchasingData.token,
                    billingPeriod = null,
                    addOnProducts = emptyList()
                )
            )
        )

        val purchaseParams = mockk<PurchaseParams>()
        every { purchaseParams.purchasingData } returns subscriptionPurchasingData
        every { purchaseParams.containsAddOnItems } returns true

        val validationResult = validator.validate(purchaseParams)
        assertThat(validationResult).isInstanceOf(Result.Success::class.java)
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `purchaseParams with different base item and add-on billing periods fails validation`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData(period = Period(1, Period.Unit.MONTH, "P1M"))
        val yearlyAddOn = stubStoreProductWithGoogleSubscriptionPurchaseData(productId = "yearly_addon",
            period = Period(1, Period.Unit.YEAR, "P1Y")
        )

        val purchaseParams = PurchaseParams.Builder(mockk(), baseProduct)
            .addOnStoreProducts(addOnStoreProducts = listOf(yearlyAddOn))
            .build()

        val validationResult = validator.validate(purchaseParams)
        if (validationResult is Result.Error) {
            val purchasesError = validationResult.value
            assertThat(purchasesError.code).isEqualTo(PurchasesErrorCode.PurchaseInvalidError)
            assertThat(purchasesError.underlyingErrorMessage)
                .isEqualTo("All items in a multi-line purchase must have the same billing period.")
        } else {
            fail("Validation result should be error")
        }
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `purchaseParams with differing and add-on product billing periods fails validation`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData(period = Period(1, Period.Unit.MONTH, "P1M"))
        val monthlyAddOn = stubStoreProductWithGoogleSubscriptionPurchaseData(productId = "monthly_addon",
            period = Period(1, Period.Unit.MONTH, "P1M")
        )
        val yearlyAddOn = stubStoreProductWithGoogleSubscriptionPurchaseData(productId = "yearly_addon",
            period = Period(1, Period.Unit.YEAR, "P1Y")
        )

        val purchaseParams = PurchaseParams.Builder(mockk(), baseProduct)
                .addOnStoreProducts(addOnStoreProducts = listOf(monthlyAddOn, yearlyAddOn))
                .build()

        val validationResult = validator.validate(purchaseParams)
        if (validationResult is Result.Error) {
            val purchasesError = validationResult.value
            assertThat(purchasesError.code).isEqualTo(PurchasesErrorCode.PurchaseInvalidError)
            assertThat(purchasesError.underlyingErrorMessage)
                .isEqualTo("All items in a multi-line purchase must have the same billing period.")
        } else {
            fail("Validation result should be error")
        }
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `purchaseParams with same product in base item and add-ons fails validation`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData(period = Period(1, Period.Unit.MONTH, "P1M"))
        val yearlyAddOn = stubStoreProductWithGoogleSubscriptionPurchaseData(period = Period(1, Period.Unit.YEAR, "P1Y"))

        val purchaseParams = PurchaseParams.Builder(mockk(), baseProduct)
                .addOnStoreProducts(addOnStoreProducts = listOf(yearlyAddOn))
                .build()

        val validationResult = validator.validate(purchaseParams)
        if (validationResult is Result.Error) {
            val purchasesError = validationResult.value
            assertThat(purchasesError.code).isEqualTo(PurchasesErrorCode.PurchaseInvalidError)
            assertThat(purchasesError.underlyingErrorMessage)
                .isEqualTo("Multi-line purchases cannot contain multiple purchases for the same product. " +
                    "Multiple instances for the product monthly_freetrial were provided."
                )
        } else {
            fail("Validation result should be error")
        }
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `purchaseParams with same product in multiple add-ons fails validation`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData(period = Period(1, Period.Unit.MONTH, "P1M"))
        val monthlyAddOn = stubStoreProductWithGoogleSubscriptionPurchaseData(
            productId = "product123",
            period = Period(1, Period.Unit.MONTH, "P1M")
        )
        val yearlyAddOn = stubStoreProductWithGoogleSubscriptionPurchaseData(
            productId = "product123",
            period = Period(1, Period.Unit.YEAR, "P1Y")
        )

        val purchaseParams = PurchaseParams.Builder(mockk(), baseProduct)
            .addOnStoreProducts(addOnStoreProducts = listOf(monthlyAddOn, yearlyAddOn))
            .build()

        val validationResult = validator.validate(purchaseParams)
        if (validationResult is Result.Error) {
            val purchasesError = validationResult.value
            assertThat(purchasesError.code).isEqualTo(PurchasesErrorCode.PurchaseInvalidError)
            assertThat(purchasesError.underlyingErrorMessage)
                .isEqualTo("Multi-line purchases cannot contain multiple purchases for the same product. " +
                    "Multiple instances for the product product123 were provided."
                )
        } else {
            fail("Validation result should be error")
        }
    }
    // endregion Add-Ons
}
