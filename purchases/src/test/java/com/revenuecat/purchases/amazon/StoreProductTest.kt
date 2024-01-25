package com.revenuecat.purchases.amazon

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class StoreProductTest {

    private val price = Price(
        formatted = "$1.00",
        amountMicros = 1_000_000,
        currencyCode = "USD"
    )

    private val period = Period.create("P1M")

    @Test
    fun `Two StoreProducts with the same properties are equal and have same hashcode`() {
        val product1 = AmazonStoreProduct(
            id = "productId",
            type = ProductType.SUBS,
            name = "title",
            title = "title",
            description = "description",
            period = period,
            price = price,
            subscriptionOptions = null,
            defaultOption = null,
            iconUrl = "iconUrl",
            freeTrialPeriod = period,
            originalProductJSON = JSONObject(),
            presentedOfferingIdentifier = "presentedOfferingIdentifier"
        )

        val product2 = AmazonStoreProduct(
            id = "productId",
            type = ProductType.SUBS,
            name = "title",
            title = "title",
            description = "description",
            period = period,
            price = price,
            subscriptionOptions = null,
            defaultOption = null,
            iconUrl = "iconUrl",
            freeTrialPeriod = period,
            originalProductJSON = JSONObject(),
            presentedOfferingIdentifier = "presentedOfferingIdentifier"
        )

        assertThat(product1).isEqualTo(product2)
        assertThat(product1.hashCode()).isEqualTo(product2.hashCode())
    }

    @Test
    fun `copyWithOfferingId copies product with offeringId`() {
        val product = AmazonStoreProduct(
            id = "productId",
            type = ProductType.SUBS,
            name = "title",
            title = "title",
            description = "description",
            period = period,
            price = price,
            subscriptionOptions = null,
            defaultOption = null,
            iconUrl = "iconUrl",
            freeTrialPeriod = period,
            originalProductJSON = JSONObject(),
            presentedOfferingIdentifier = "presentedOfferingIdentifier"
        )

        val expectedOfferingId = "newOfferingId"
        val copiedProduct = product!!.copyWithOfferingId(expectedOfferingId).amazonProduct!!

        assertThat(copiedProduct.id).isEqualTo(product.id)
        assertThat(copiedProduct.type).isEqualTo(product.type)
        assertThat(copiedProduct.price).isEqualTo(product.price)
        assertThat(copiedProduct.title).isEqualTo(product.title)
        assertThat(copiedProduct.description).isEqualTo(product.description)
        assertThat(copiedProduct.period).isEqualTo(product.period)
        assertThat(copiedProduct.subscriptionOptions).isEqualTo(product.subscriptionOptions)
        assertThat(copiedProduct.defaultOption).isEqualTo(product.defaultOption)
        assertThat(copiedProduct.iconUrl).isEqualTo(product.iconUrl)
        assertThat(copiedProduct.freeTrialPeriod).isEqualTo(product.freeTrialPeriod)
        assertThat(copiedProduct.originalProductJSON).isEqualTo(product.originalProductJSON)
        assertThat(copiedProduct.purchasingData.productId).isEqualTo(product.purchasingData.productId)
        assertThat(copiedProduct.purchasingData.productType).isEqualTo(product.purchasingData.productType)
        assertThat(
            (copiedProduct.purchasingData as AmazonPurchasingData.Product).storeProduct.presentedOfferingIdentifier)
            .isEqualTo(expectedOfferingId)
        assertThat(copiedProduct.presentedOfferingIdentifier).isEqualTo(expectedOfferingId)

    }

    @Test
    fun `formattedPricePerMonth is null for INAPP product`() {
        val product = AmazonStoreProduct(
            id = "productId",
            type = ProductType.INAPP,
            name = "title",
            title = "title",
            description = "description",
            period = null,
            price = price,
            subscriptionOptions = null,
            defaultOption = null,
            iconUrl = "iconUrl",
            freeTrialPeriod = null,
            originalProductJSON = JSONObject(),
            presentedOfferingIdentifier = "presentedOfferingIdentifier"
        )
        assertThat(product.formattedPricePerMonth(Locale.US)).isNull()
    }

    @Test
    fun `formattedPricePerMonth is correct for SUBS monthly product`() {
        val product = AmazonStoreProduct(
            id = "productId",
            type = ProductType.SUBS,
            name = "title",
            title = "title",
            description = "description",
            period = period,
            price = price,
            subscriptionOptions = null,
            defaultOption = null,
            iconUrl = "iconUrl",
            freeTrialPeriod = period,
            originalProductJSON = JSONObject(),
            presentedOfferingIdentifier = "presentedOfferingIdentifier"
        )
        assertThat(product.formattedPricePerMonth(Locale.US)).isEqualTo("$1.00")
    }

    @Test
    fun `formattedPricePerMonth is correct for SUBS annual product`() {
        val product = AmazonStoreProduct(
            id = "productId",
            type = ProductType.SUBS,
            name = "title",
            title = "title",
            description = "description",
            period = Period.create("P1Y"),
            price = price,
            subscriptionOptions = null,
            defaultOption = null,
            iconUrl = "iconUrl",
            freeTrialPeriod = period,
            originalProductJSON = JSONObject(),
            presentedOfferingIdentifier = "presentedOfferingIdentifier"
        )
        assertThat(product.formattedPricePerMonth(Locale.US)).isEqualTo("$0.08")
    }
}
