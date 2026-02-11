package com.revenuecat.purchases.simulatedstore

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.common.networking.WebBillingPhase
import com.revenuecat.purchases.common.networking.WebBillingPrice
import com.revenuecat.purchases.common.networking.WebBillingProductResponse
import com.revenuecat.purchases.common.networking.WebBillingPurchaseOption
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.RecurrenceMode
import com.revenuecat.purchases.models.TestStoreProduct
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
public class SimulatedStoreProductConverterTest {

    private val testLocale = Locale.US

    @Test
    fun `converts one time product correctly`() {
        val productResponse = WebBillingProductResponse(
            identifier = "test_product",
            productType = "subscription",
            title = "Test Product",
            description = "Test Description",
            defaultPurchaseOptionId = "option1",
            purchaseOptions = mapOf(
                "option1" to WebBillingPurchaseOption(
                    basePrice = WebBillingPrice(
                        amountMicros = 9990000L,
                        currency = "USD"
                    )
                )
            )
        )

        val result = convertToStoreProduct(productResponse)

        assertThat(result).isInstanceOf(TestStoreProduct::class.java)
        assertThat(result.id).isEqualTo("test_product")
        assertThat(result.title).isEqualTo("Test Product")
        assertThat(result.name).isEqualTo("Test Product")
        assertThat(result.description).isEqualTo("Test Description")
        assertThat(result.price.formatted).isEqualTo("$9.99")
        assertThat(result.price.amountMicros).isEqualTo(9990000L)
        assertThat(result.price.currencyCode).isEqualTo("USD")
    }

    @Test
    fun `converts subscription product correctly`() {
        val productResponse = WebBillingProductResponse(
            identifier = "sub_product",
            productType = "subscription",
            title = "Sub Product",
            description = null,
            defaultPurchaseOptionId = null,
            purchaseOptions = mapOf(
                "option1" to WebBillingPurchaseOption(
                    base = WebBillingPhase(
                        price = WebBillingPrice(
                            amountMicros = 4990000L,
                            currency = "EUR"
                        ),
                        periodDuration = "P1M",
                    )
                )
            )
        )

        val result = convertToStoreProduct(productResponse)

        val expectedPrice = Price(
            formatted = "€4.99",
            amountMicros = 4990000L,
            currencyCode = "EUR"
        )
        assertThat(result.id).isEqualTo("sub_product")
        assertThat(result.description).isEqualTo("")
        assertThat(result.price).isEqualTo(expectedPrice)
        assertThat(result.period).isEqualTo(Period.create("P1M"))
        assertThat(result.defaultOption?.pricingPhases?.size).isEqualTo(1)
        assertThat(result.defaultOption?.pricingPhases?.get(0)).isEqualTo(
            PricingPhase(
                billingPeriod = Period.create("P1M"),
                recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                billingCycleCount = null,
                price = expectedPrice,
            )
        )
    }

    @Test
    fun `converts product with free trial correctly`() {
        val productResponse = WebBillingProductResponse(
            identifier = "trial_product",
            productType = "subscription",
            title = "Trial Product",
            description = "With trial",
            defaultPurchaseOptionId = "option1",
            purchaseOptions = mapOf(
                "option1" to WebBillingPurchaseOption(
                    base = WebBillingPhase(
                        price = WebBillingPrice(
                            amountMicros = 9990000L,
                            currency = "USD"
                        ),
                        periodDuration = "P1M"
                    ),
                    trial = WebBillingPhase(
                        periodDuration = "P7D",
                        cycleCount = 2
                    )
                )
            )
        )

        val result = convertToStoreProduct(productResponse)

        assertThat(result.defaultOption?.freePhase).isEqualTo(
            PricingPhase(
                billingPeriod = Period.create("P7D"),
                recurrenceMode = RecurrenceMode.FINITE_RECURRING,
                billingCycleCount = 2,
                price = Price(
                    formatted = "$0.00",
                    amountMicros = 0L,
                    currencyCode = "USD"
                )
            )
        )
        assertThat(result.defaultOption?.introPhase).isNull()
        assertThat(result.defaultOption?.fullPricePhase).isEqualTo(
            PricingPhase(
                billingPeriod = Period.create("P1M"),
                recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                billingCycleCount = null,
                price = Price(
                    formatted = "$9.99",
                    amountMicros = 9990000L,
                    currencyCode = "USD"
                )
            )
        )
    }

    @Test
    fun `converts product with intro price correctly`() {
        val productResponse = WebBillingProductResponse(
            identifier = "intro_product",
            productType = "subscription",
            title = "Intro Product",
            description = "With intro",
            defaultPurchaseOptionId = "option1",
            purchaseOptions = mapOf(
                "option1" to WebBillingPurchaseOption(
                    base = WebBillingPhase(
                        price = WebBillingPrice(
                            amountMicros = 9990000L,
                            currency = "USD"
                        ),
                        periodDuration = "P1M"
                    ),
                    introPrice = WebBillingPhase(
                        price = WebBillingPrice(
                            amountMicros = 1990000L,
                            currency = "USD"
                        ),
                        periodDuration = "P1M",
                        cycleCount = 3
                    )
                )
            )
        )

        val result = convertToStoreProduct(productResponse)

        assertThat(result.defaultOption?.freePhase).isNull()
        assertThat(result.defaultOption?.introPhase).isEqualTo(
            PricingPhase(
                billingPeriod = Period.create("P1M"),
                recurrenceMode = RecurrenceMode.FINITE_RECURRING,
                billingCycleCount = 3,
                price = Price(
                    formatted = "$1.99",
                    amountMicros = 1990000L,
                    currencyCode = "USD"
                )
            )
        )
        assertThat(result.defaultOption?.fullPricePhase).isEqualTo(
            PricingPhase(
                billingPeriod = Period.create("P1M"),
                recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                billingCycleCount = null,
                price = Price(
                    formatted = "$9.99",
                    amountMicros = 9990000L,
                    currencyCode = "USD"
                )
            )
        )
    }

    @Test
    fun `throws exception when defaultPurchaseOptionId is invalid`() {
        val productResponse = WebBillingProductResponse(
            identifier = "test_product",
            productType = "subscription",
            title = "Test Product",
            description = "Test",
            defaultPurchaseOptionId = "missing_option",
            purchaseOptions = mapOf(
                "option1" to WebBillingPurchaseOption(
                    basePrice = WebBillingPrice(
                        amountMicros = 9990000L,
                        currency = "USD"
                    )
                )
            )
        )

        try {
            convertToStoreProduct(productResponse)
            fail("Expected PurchasesException to be thrown")
        } catch (e: PurchasesException) {
            assertThat(e.error.code).isEqualTo(PurchasesErrorCode.ProductNotAvailableForPurchaseError)
            assertThat(e.error.underlyingErrorMessage).isEqualTo("No purchase option found for product test_product")
        }
    }

    @Test
    fun `handles missing base price gracefully`() {
        val productResponse = WebBillingProductResponse(
            identifier = "no_price",
            productType = "subscription",
            title = "No Price",
            description = "Test",
            defaultPurchaseOptionId = "option1",
            purchaseOptions = mapOf(
                "option1" to WebBillingPurchaseOption(
                    base = WebBillingPhase(
                        price = null,
                        periodDuration = "P1M"
                    )
                )
            )
        )

        try {
            val result = convertToStoreProduct(productResponse)
            fail("Expected PurchasesException to be thrown, but got result: $result")
        } catch (e: PurchasesException) {
            assertThat(e.error.code).isEqualTo(PurchasesErrorCode.ProductNotAvailableForPurchaseError)
            assertThat(e.error.underlyingErrorMessage).isEqualTo("Base price is required for test subscription products")
        }
    }

    @Test
    fun `handles missing trial phase data gracefully`() {
        val productResponse = WebBillingProductResponse(
            identifier = "incomplete_trial",
            productType = "subscription",
            title = "Incomplete Trial",
            description = "Test",
            defaultPurchaseOptionId = "option1",
            purchaseOptions = mapOf(
                "option1" to WebBillingPurchaseOption(
                    base = WebBillingPhase(
                        price = WebBillingPrice(
                            amountMicros = 9990000L,
                            currency = "USD"
                        ),
                        periodDuration = "P1M",
                    ),
                    trial = WebBillingPhase(
                        cycleCount = 1, // No period provided
                    )
                )
            )
        )

        val result = convertToStoreProduct(productResponse)

        assertThat(result.subscriptionOptions?.freeTrial).isNull()
    }

    @Test
    fun `handles missing intro price data gracefully`() {
        val productResponse = WebBillingProductResponse(
            identifier = "incomplete_intro",
            productType = "subscription",
            title = "Incomplete Intro",
            description = "Test",
            defaultPurchaseOptionId = "option1",
            purchaseOptions = mapOf(
                "option1" to WebBillingPurchaseOption(
                    base = WebBillingPhase(
                        price = WebBillingPrice(
                            amountMicros = 9990000L,
                            currency = "USD"
                        ),
                        periodDuration = "P1M"
                    ),
                    introPrice = WebBillingPhase(
                        price = null, // No price provided
                        periodDuration = "P1M",
                        cycleCount = 3
                    )
                )
            )
        )

        val result = convertToStoreProduct(productResponse)

        assertThat(result.subscriptionOptions?.introOffer).isNull()
    }

    @Test
    fun `formatPrice formats correctly`() {
        val productResponse = WebBillingProductResponse(
            identifier = "format_test",
            productType = "subscription",
            title = "Format Test",
            description = "Test",
            defaultPurchaseOptionId = "option1",
            purchaseOptions = mapOf(
                "option1" to WebBillingPurchaseOption(
                    basePrice = WebBillingPrice(
                        amountMicros = 12345678L,
                        currency = "JPY"
                    )
                )
            )
        )

        val result = convertToStoreProduct(productResponse)

        assertThat(result.price.formatted).isEqualTo("¥12")
    }

    @Test
    fun `handles zero price correctly`() {
        val productResponse = WebBillingProductResponse(
            identifier = "free_product",
            productType = "subscription",
            title = "Free Product",
            description = "Test",
            defaultPurchaseOptionId = "option1",
            purchaseOptions = mapOf(
                "option1" to WebBillingPurchaseOption(
                    basePrice = WebBillingPrice(
                        amountMicros = 0L,
                        currency = "USD"
                    )
                )
            )
        )

        val result = convertToStoreProduct(productResponse)

        assertThat(result.price.formatted).isEqualTo("$0.00")
        assertThat(result.price.amountMicros).isEqualTo(0L)
    }

    private fun convertToStoreProduct(
        productResponse: WebBillingProductResponse,
        locale: Locale = testLocale
    ): TestStoreProduct {
        return SimulatedStoreProductConverter.convertToStoreProduct(productResponse, locale)
    }
}
