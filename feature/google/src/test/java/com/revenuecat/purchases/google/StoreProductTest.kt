package com.revenuecat.purchases.google

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.GoogleSubscriptionOption
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
            formatted = "$1.00",
            amountMicros = 100,
            currencyCode = "USD"
        )
        val price2 = Price(
            formatted = "$1.00",
            amountMicros = 100,
            currencyCode = "USD"
        )

        val subscriptionOption1 = GoogleSubscriptionOption(
            id = "subscriptionOptionId",
            pricingPhases = listOf(PricingPhase(
                billingPeriod = "",
                recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                billingCycleCount = 0,
                price = Price(
                    formatted = "",
                    amountMicros = 0L,
                    currencyCode = "",
                )
            )),
            tags = emptyList(),
            purchasingData = GooglePurchasingData.Subscription(
                productId = "product_id",
                productDetails = productDetails,
                optionId = "subscriptionOptionId",
                token = "mock-token"
            )
        )
        val subscriptionOption2 = GoogleSubscriptionOption(
            id = "subscriptionOptionId",
            pricingPhases = listOf(PricingPhase(
                billingPeriod = "",
                recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                billingCycleCount = 0,
                price = Price(
                    formatted = "",
                    amountMicros = 0L,
                    currencyCode = "",
                )
            )),
            tags = emptyList(),
            purchasingData = GooglePurchasingData.Subscription(
                productId = "product_id",
                productDetails = productDetails,
                optionId = "subscriptionOptionId",
                token = "mock-token"
            )
        )

        val storeProduct1 = GoogleStoreProduct(
            productId = "product_id",
            basePlanId = "does_not_matter_for_this_test",
            type = ProductType.SUBS,
            price = price1,
            title = "TITLE",
            description = "DESCRIPTION",
            subscriptionPeriod = "P1M",
            subscriptionOptions = listOf(subscriptionOption1),
            defaultOption = null,
            productDetails = productDetails
        )

        val storeProduct2 = GoogleStoreProduct(
            productId = "product_id",
            basePlanId = "does_not_matter_for_this_test",
            type = ProductType.SUBS,
            price = price2,
            title = "TITLE",
            description = "DESCRIPTION",
            subscriptionPeriod = "P1M",
            subscriptionOptions = listOf(subscriptionOption2),
            defaultOption = null,
            productDetails = productDetails
        )

        assertThat(storeProduct1).isEqualTo(storeProduct2)
    }

    @Test
    fun `Two StoreProducts with the same properties have the same hashcode`() {
        val productDetails = mockProductDetails()
        val price1 = Price(
            formatted = "$1.00",
            amountMicros = 100,
            currencyCode = "USD"
        )
        val price2 = Price(
            formatted = "$1.00",
            amountMicros = 100,
            currencyCode = "USD"
        )

        val subscriptionOption1 = GoogleSubscriptionOption(
            id = "subscriptionOptionId",
            pricingPhases = listOf(PricingPhase(
                billingPeriod = "",
                recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                billingCycleCount = 0,
                price = Price(
                    formatted = "",
                    amountMicros = 0L,
                    currencyCode = "",
                )
            )),
            tags = emptyList(),
            purchasingData = GooglePurchasingData.Subscription(
                productId = "product_id",
                productDetails = productDetails,
                optionId = "subscriptionOptionId",
                token = "mock-token"
            )
        )
        val subscriptionOption2 = GoogleSubscriptionOption(
            id = "subscriptionOptionId",
            pricingPhases = listOf(PricingPhase(
                billingPeriod = "",
                recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                billingCycleCount = 0,
                price = Price(
                    formatted = "",
                    amountMicros = 0L,
                    currencyCode = "",
                )
            )),
            tags = emptyList(),
            purchasingData = GooglePurchasingData.Subscription(
                productId = "product_id",
                productDetails = productDetails,
                optionId = "subscriptionOptionId",
                token = "mock-token"
            )
        )

        val storeProduct1 = GoogleStoreProduct(
            productId = "product_id",
            basePlanId = "does_not_matter_for_this_test",
            type = ProductType.SUBS,
            price = price1,
            title = "TITLE",
            description = "DESCRIPTION",
            subscriptionPeriod = "P1M",
            subscriptionOptions = listOf(subscriptionOption1),
            defaultOption = null,
            productDetails = productDetails
        )

        val storeProduct2 = GoogleStoreProduct(
            productId = "product_id",
            basePlanId = "does_not_matter_for_this_test",
            type = ProductType.SUBS,
            price = price2,
            title = "TITLE",
            description = "DESCRIPTION",
            subscriptionPeriod = "P1M",
            subscriptionOptions = listOf(subscriptionOption2),
            defaultOption = null,
            productDetails = productDetails
        )

        assertThat(storeProduct1.hashCode()).isEqualTo(storeProduct2.hashCode())
    }
}