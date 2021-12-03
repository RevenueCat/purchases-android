package com.revenuecat.purchases.common.models

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.StoreProduct
import org.assertj.core.api.Assertions
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoreProductTest {

    @Test
    fun `Two StoreProducts with the same properties are equal`() {
        val storeProduct1 = StoreProduct(
            sku = "sku",
            type = ProductType.INAPP,
            price = "$2",
            priceAmountMicros = 2 * 1_000_000,
            priceCurrencyCode = "USD",
            originalPrice = "$2",
            originalPriceAmountMicros = 2 * 1_000_000,
            title = "TITLE",
            description = "DESCRIPTION",
            subscriptionPeriod = "P1M",
            freeTrialPeriod = "P1M",
            introductoryPrice = "$0",
            introductoryPriceAmountMicros = 0 * 1_000_000,
            introductoryPricePeriod = "P1W",
            introductoryPriceCycles = 1,
            iconUrl = "http://",
            originalJson = JSONObject("{}")
        )
        val storeProduct2 = StoreProduct(
            sku = "sku",
            type = ProductType.INAPP,
            price = "$2",
            priceAmountMicros = 2 * 1_000_000,
            priceCurrencyCode = "USD",
            originalPrice = "$2",
            originalPriceAmountMicros = 2 * 1_000_000,
            title = "TITLE",
            description = "DESCRIPTION",
            subscriptionPeriod = "P1M",
            freeTrialPeriod = "P1M",
            introductoryPrice = "$0",
            introductoryPriceAmountMicros = 0 * 1_000_000,
            introductoryPricePeriod = "P1W",
            introductoryPriceCycles = 1,
            iconUrl = "http://",
            originalJson = JSONObject("{}")
        )
        Assertions.assertThat(storeProduct1).isEqualTo(storeProduct2)
    }

    @Test
    fun `Two StoreProducts with the same properties have the same hashcode`() {
        val storeProduct1 = StoreProduct(
            sku = "sku",
            type = ProductType.INAPP,
            price = "$2",
            priceAmountMicros = 2 * 1_000_000,
            priceCurrencyCode = "USD",
            originalPrice = "$2",
            originalPriceAmountMicros = 2 * 1_000_000,
            title = "TITLE",
            description = "DESCRIPTION",
            subscriptionPeriod = "P1M",
            freeTrialPeriod = "P1M",
            introductoryPrice = "$0",
            introductoryPriceAmountMicros = 0 * 1_000_000,
            introductoryPricePeriod = "P1W",
            introductoryPriceCycles = 1,
            iconUrl = "http://",
            originalJson = JSONObject("{}")
        )
        val storeProduct2 = StoreProduct(
            sku = "sku",
            type = ProductType.INAPP,
            price = "$2",
            priceAmountMicros = 2 * 1_000_000,
            priceCurrencyCode = "USD",
            originalPrice = "$2",
            originalPriceAmountMicros = 2 * 1_000_000,
            title = "TITLE",
            description = "DESCRIPTION",
            subscriptionPeriod = "P1M",
            freeTrialPeriod = "P1M",
            introductoryPrice = "$0",
            introductoryPriceAmountMicros = 0 * 1_000_000,
            introductoryPricePeriod = "P1W",
            introductoryPriceCycles = 1,
            iconUrl = "http://",
            originalJson = JSONObject("{}")
        )
        Assertions.assertThat(storeProduct1.hashCode()).isEqualTo(storeProduct2.hashCode())
    }
}