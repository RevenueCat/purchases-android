package com.revenuecat.purchases.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.models.GooglePurchasingData
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.StoreProduct
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.DefaultAsserter.fail

@RunWith(AndroidJUnit4::class)
class PurchaseParamsValidatorTest {
    // region Add-On Validation

    private val validator = PurchaseParamsValidator()

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `purchaseParams with base item that isn't a google subscription fails validation`() {
        val purchaseParams = PurchaseParams.Builder(
            mockk(),
            stubStoreProduct(productId = "abc")
        )
            .addOnStoreProducts(addOnStoreProducts = emptyList())
            .build()

        val validationResult = validator.validate(purchaseParams)
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
                    "Multiple purchases for the product monthly_freetrial were provided."
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
                    "Multiple purchases for the product product123 were provided."
                )
        } else {
            fail("Validation result should be error")
        }
    }
    // endregion Add-Ons
}
