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
import java.util.Locale

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
            name = "TITLE",
            title = "TITLE (App name)",
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
            name = "TITLE",
            title = "TITLE (App name)",
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
            name = "TITLE",
            title = "TITLE (App name)",
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
            name = "TITLE",
            title = "TITLE (App name)",
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
            name = "TITLE",
            title = "TITLE (App name)",
            description = "DESCRIPTION",
            period = Period.create("P1M"),
            subscriptionOptions = SubscriptionOptions(listOf(subscriptionOption2)),
            defaultOption = null,
            productDetails = productDetails
        )

        assertThat(storeProduct1.hashCode()).isEqualTo(storeProduct2.hashCode())
    }

    @Test
    fun `copyWithOfferingId copies product with offeringId applied to itself and all subscriptionOptions`() {
        val productDetails = mockProductDetails()
        val price = Price(
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

        val storeProduct = GoogleStoreProduct(
            productId = "product_id",
            basePlanId = "base_plan_id",
            type = ProductType.SUBS,
            price = price,
            name = "TITLE",
            title = "TITLE (App name)",
            description = "DESCRIPTION",
            period = Period.create("P1M"),
            subscriptionOptions = SubscriptionOptions(listOf(subscriptionOption1, subscriptionOption2)),
            defaultOption = subscriptionOption1,
            productDetails = productDetails,
            presentedOfferingIdentifier = "originalOfferingId"
        )

        val expectedOfferingId = "expectedOfferingId"
        val copiedStoreProduct = storeProduct.copyWithOfferingId(expectedOfferingId) as GoogleStoreProduct

        assertThat(copiedStoreProduct.id).isEqualTo(storeProduct.id)
        assertThat(copiedStoreProduct.productId).isEqualTo(storeProduct.productId)
        assertThat(copiedStoreProduct.basePlanId).isEqualTo(storeProduct.basePlanId)
        assertThat(copiedStoreProduct.type).isEqualTo(storeProduct.type)
        assertThat(copiedStoreProduct.price).isEqualTo(storeProduct.price)
        assertThat(copiedStoreProduct.title).isEqualTo(storeProduct.title)
        assertThat(copiedStoreProduct.description).isEqualTo(storeProduct.description)
        assertThat(copiedStoreProduct.period).isEqualTo(storeProduct.period)
        assertThat(copiedStoreProduct.productDetails).isEqualTo(storeProduct.productDetails)

        assertThat(copiedStoreProduct.presentedOfferingIdentifier).isEqualTo(expectedOfferingId)
        assertThat(copiedStoreProduct.defaultOption!!.presentedOfferingIdentifier).isEqualTo(expectedOfferingId)
        copiedStoreProduct.subscriptionOptions!!.forEach {
            assertThat(it.presentedOfferingIdentifier).isEqualTo(expectedOfferingId)
        }
    }

    @Test
    fun `formattedPricePerMonth is null for INAPP product`() {
        val storeProduct = GoogleStoreProduct(
            productId = "product_id",
            basePlanId = "base_plan_id",
            type = ProductType.INAPP,
            price = Price(formatted = "$1.00", amountMicros = 1_000_000, currencyCode = "USD"),
            name = "TITLE",
            title = "TITLE (App name)",
            description = "DESCRIPTION",
            period = null,
            subscriptionOptions = null,
            defaultOption = null,
            productDetails = mockProductDetails(),
            presentedOfferingIdentifier = "originalOfferingId"
        )
        assertThat(storeProduct.pricePerMonth(Locale.US)).isNull()
        assertThat(storeProduct.formattedPricePerMonth(Locale.US)).isNull()
    }

    @Test
    fun `formattedPricePerMonth is correct for SUBS monthly product with free trial`() {
        val product = createSubscriptionStoreProduct(Period.create("P1M"))
        assertThat(product.pricePerMonth(Locale.US)).isEqualTo(Price(
            "$1.00",
            1_000_000,
            product.price.currencyCode,
        ))
        assertThat(product.formattedPricePerMonth(Locale.US)).isEqualTo("$1.00")
    }

    @Test
    fun `formattedPricePerMonth is correct for SUBS annual product with free trial`() {
        val product = createSubscriptionStoreProduct(Period.create("P1Y"))
        assertThat(product.pricePerMonth(Locale.US)).isEqualTo(Price(
            "$0.08",
            83_333,
            product.price.currencyCode,
        ))
        assertThat(product.formattedPricePerMonth(Locale.US)).isEqualTo("$0.08")
    }

    private fun createSubscriptionStoreProduct(
        period: Period,
    ): GoogleStoreProduct {
        val productDetails = mockProductDetails()

        val priceAfterTrial = Price(
            formatted = "$1.00",
            amountMicros = 1_000_000,
            currencyCode = "USD"
        )

        val freeTrialPricingPhase = PricingPhase(
            billingPeriod = period,
            recurrenceMode = RecurrenceMode.FINITE_RECURRING,
            billingCycleCount = 1,
            price = Price("$0.00", 0L, "USD")
        )

        val regularPricingPhase = PricingPhase(
            billingPeriod = period,
            recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
            billingCycleCount = 0,
            price = priceAfterTrial
        )

        val subscriptionOption1 = GoogleSubscriptionOption(
            productId = productId,
            basePlanId = basePlanId,
            offerId = null,
            pricingPhases = listOf(freeTrialPricingPhase, regularPricingPhase),
            tags = emptyList(),
            productDetails,
            offerToken
        )

        val subscriptionOption2 = GoogleSubscriptionOption(
            productId = productId,
            basePlanId = basePlanId,
            offerId = null,
            pricingPhases = listOf(regularPricingPhase),
            tags = emptyList(),
            productDetails,
            offerToken
        )

        return GoogleStoreProduct(
            productId = "product_id",
            basePlanId = "base_plan_id",
            type = ProductType.SUBS,
            price = priceAfterTrial,
            name = "TITLE",
            title = "TITLE (App name)",
            description = "DESCRIPTION",
            period = period,
            subscriptionOptions = SubscriptionOptions(listOf(subscriptionOption1, subscriptionOption2)),
            defaultOption = subscriptionOption1,
            productDetails = productDetails,
            presentedOfferingIdentifier = "originalOfferingId"
        )
    }
}
