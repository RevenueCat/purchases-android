package com.revenuecat.purchases.google

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.GooglePurchaseOption
import com.revenuecat.purchases.models.GooglePurchasingData
import com.revenuecat.purchases.models.GoogleStoreProduct
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.RecurrenceMode
import com.revenuecat.purchases.utils.mockProductDetails
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoreProductTest {
    @Test
    fun `Two StoreProducts with the same properties are equal`() {
        val productDetails = mockProductDetails()
        val price1 = Price(
            formattedPrice = "$1.00",
            priceAmountMicros = 100,
            currencyCode = "USD"
        )
        val price2 = Price(
            formattedPrice = "$1.00",
            priceAmountMicros = 100,
            currencyCode = "USD"
        )

        val purchaseOption1 = GooglePurchaseOption(
            id = "purchaseOptionId",
            pricingPhases = listOf(PricingPhase(
                billingPeriod = "",
                priceCurrencyCode = "",
                formattedPrice = "",
                priceAmountMicros = 0L,
                recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                billingCycleCount = 0
            )),
            tags = emptyList(),
            purchasingData = GooglePurchasingData.Subscription(
                productId = "product_id",
                productDetails = productDetails,
                optionId = "purchaseOptionId",
                token = "mock-token"
            )
        )
        val purchaseOption2 = GooglePurchaseOption(
            id = "purchaseOptionId",
            pricingPhases = listOf(PricingPhase(
                billingPeriod = "",
                priceCurrencyCode = "",
                formattedPrice = "",
                priceAmountMicros = 0L,
                recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                billingCycleCount = 0
            )),
            tags = emptyList(),
            purchasingData = GooglePurchasingData.Subscription(
                productId = "product_id",
                productDetails = productDetails,
                optionId = "purchaseOptionId",
                token = "mock-token"
            )
        )

        val storeProduct1 = GoogleStoreProduct(
            productId = "product_id",
            type = ProductType.SUBS,
            oneTimeProductPrice = price1,
            title = "TITLE",
            description = "DESCRIPTION",
            subscriptionPeriod = "P1M",
            purchaseOptions = listOf(purchaseOption1),
            defaultOption = null,
            productDetails = productDetails
        )

        val storeProduct2 = GoogleStoreProduct(
            productId = "product_id",
            type = ProductType.SUBS,
            oneTimeProductPrice = price2,
            title = "TITLE",
            description = "DESCRIPTION",
            subscriptionPeriod = "P1M",
            purchaseOptions = listOf(purchaseOption2),
            defaultOption = null,
            productDetails = productDetails
        )

        assertThat(storeProduct1).isEqualTo(storeProduct2)
    }

    @Test
    fun `Two StoreProducts with the same properties have the same hashcode`() {
        val productDetails = mockProductDetails()
        val price1 = Price(
            formattedPrice = "$1.00",
            priceAmountMicros = 100,
            currencyCode = "USD"
        )
        val price2 = Price(
            formattedPrice = "$1.00",
            priceAmountMicros = 100,
            currencyCode = "USD"
        )

        val purchaseOption1 = GooglePurchaseOption(
            id = "purchaseOptionId",
            pricingPhases = listOf(PricingPhase(
                billingPeriod = "",
                priceCurrencyCode = "",
                formattedPrice = "",
                priceAmountMicros = 0L,
                recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                billingCycleCount = 0
            )),
            tags = emptyList(),
            purchasingData = GooglePurchasingData.Subscription(
                productId = "product_id",
                productDetails = productDetails,
                optionId = "purchaseOptionId",
                token = "mock-token"
            )
        )
        val purchaseOption2 = GooglePurchaseOption(
            id = "purchaseOptionId",
            pricingPhases = listOf(PricingPhase(
                billingPeriod = "",
                priceCurrencyCode = "",
                formattedPrice = "",
                priceAmountMicros = 0L,
                recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                billingCycleCount = 0
            )),
            tags = emptyList(),
            purchasingData = GooglePurchasingData.Subscription(
                productId = "product_id",
                productDetails = productDetails,
                optionId = "purchaseOptionId",
                token = "mock-token"
            )
        )

        val storeProduct1 = GoogleStoreProduct(
            productId = "product_id",
            type = ProductType.SUBS,
            oneTimeProductPrice = price1,
            title = "TITLE",
            description = "DESCRIPTION",
            subscriptionPeriod = "P1M",
            purchaseOptions = listOf(purchaseOption1),
            defaultOption = null,
            productDetails = productDetails
        )

        val storeProduct2 = GoogleStoreProduct(
            productId = "product_id",
            type = ProductType.SUBS,
            oneTimeProductPrice = price2,
            title = "TITLE",
            description = "DESCRIPTION",
            subscriptionPeriod = "P1M",
            purchaseOptions = listOf(purchaseOption2),
            defaultOption = null,
            productDetails = productDetails
        )

        assertThat(storeProduct1.hashCode()).isEqualTo(storeProduct2.hashCode())
    }
}