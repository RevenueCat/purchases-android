package com.revenuecat.purchases.google

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.GoogleStoreProduct
import com.revenuecat.purchases.models.GoogleSubscriptionOption
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.RecurrenceMode
import com.revenuecat.purchases.models.SubscriptionOptions
import com.revenuecat.purchases.utils.mockProductDetails
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoreProductTest {

    private val productId = "product-id"
    private val basePlanId = "base-plan-id"
    private val offerToken = "mock-token"

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
            productId = productId,
            basePlanId = basePlanId,
            offerId = null,
            pricingPhases = listOf(PricingPhase(
                billingPeriod = Period.create("P1M"),
                recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                billingCycleCount = 0,
                price = Price(
                    formatted = "",
                    amountMicros = 0L,
                    currencyCode = "",
                )
            )),
            tags = emptyList(),
            productDetails,
            offerToken
        )
        val subscriptionOption2 = GoogleSubscriptionOption(
            productId = productId,
            basePlanId = basePlanId,
            offerId = null,
            pricingPhases = listOf(PricingPhase(
                billingPeriod = Period.create("P1M"),
                recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                billingCycleCount = 0,
                price = Price(
                    formatted = "",
                    amountMicros = 0L,
                    currencyCode = "",
                )
            )),
            tags = emptyList(),
            productDetails,
            offerToken
        )

        val storeProduct1 = GoogleStoreProduct(
            productId = "product_id",
            basePlanId = "base_plan_id",
            type = ProductType.SUBS,
            price = price1,
            title = "TITLE",
            description = "DESCRIPTION",
            period = Period.create("P1M"),
            subscriptionOptions = SubscriptionOptions(listOf(subscriptionOption1)),
            defaultOption = null,
            productDetails = productDetails
        )

        val storeProduct2 = GoogleStoreProduct(
            productId = "product_id",
            basePlanId = "base_plan_id",
            type = ProductType.SUBS,
            price = price2,
            title = "TITLE",
            description = "DESCRIPTION",
            period = Period.create("P1M"),
            subscriptionOptions = SubscriptionOptions(listOf(subscriptionOption2)),
            defaultOption = null,
            productDetails = productDetails
        )

        assertThat(storeProduct1).isEqualTo(storeProduct2)
    }

    @Test
    fun `GoogleStoreProduct can access computed properties correctly`() {
        val productDetails = mockProductDetails()
        val price = Price(
            formatted = "$1.00",
            amountMicros = 100,
            currencyCode = "USD"
        )

        val subscriptionOption = GoogleSubscriptionOption(
            productId = productId,
            basePlanId = basePlanId,
            offerId = null,
            pricingPhases = listOf(PricingPhase(
                billingPeriod = Period.create("P1M"),
                recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                billingCycleCount = 0,
                price = price
            )),
            tags = emptyList(),
            productDetails,
            offerToken
        )

        val storeProduct = GoogleStoreProduct(
            productId = "product_id",
            basePlanId = "base_plan_id",
            type = ProductType.SUBS,
            price = price,
            title = "TITLE",
            description = "DESCRIPTION",
            period = Period.create("P1M"),
            subscriptionOptions = SubscriptionOptions(listOf(subscriptionOption)),
            defaultOption = subscriptionOption,
            productDetails = productDetails
        )

        assertThat(storeProduct.id).isEqualTo("product_id:base_plan_id")
        assertThat(storeProduct.sku).isEqualTo("product_id")
        assertThat(storeProduct.purchasingData).isEqualTo(subscriptionOption.purchasingData)
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
            productId = productId,
            basePlanId = basePlanId,
            offerId = null,
            pricingPhases = listOf(PricingPhase(
                billingPeriod = Period.create("P1M"),
                recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                billingCycleCount = 0,
                price = Price(
                    formatted = "",
                    amountMicros = 0L,
                    currencyCode = "",
                )
            )),
            tags = emptyList(),
            productDetails,
            offerToken
        )
        val subscriptionOption2 = GoogleSubscriptionOption(
            productId = productId,
            basePlanId = basePlanId,
            offerId = null,
            pricingPhases = listOf(PricingPhase(
                billingPeriod = Period.create("P1M"),
                recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                billingCycleCount = 0,
                price = Price(
                    formatted = "",
                    amountMicros = 0L,
                    currencyCode = "",
                )
            )),
            tags = emptyList(),
            productDetails,
            offerToken
        )

        val storeProduct1 = GoogleStoreProduct(
            productId = "product_id",
            basePlanId = "base_plan_id",
            type = ProductType.SUBS,
            price = price1,
            title = "TITLE",
            description = "DESCRIPTION",
            period = Period.create("P1M"),
            subscriptionOptions = SubscriptionOptions(listOf(subscriptionOption1)),
            defaultOption = null,
            productDetails = productDetails
        )

        val storeProduct2 = GoogleStoreProduct(
            productId = "product_id",
            basePlanId = "base_plan_id",
            type = ProductType.SUBS,
            price = price2,
            title = "TITLE",
            description = "DESCRIPTION",
            period = Period.create("P1M"),
            subscriptionOptions = SubscriptionOptions(listOf(subscriptionOption2)),
            defaultOption = null,
            productDetails = productDetails
        )

        assertThat(storeProduct1.hashCode()).isEqualTo(storeProduct2.hashCode())
    }
}
