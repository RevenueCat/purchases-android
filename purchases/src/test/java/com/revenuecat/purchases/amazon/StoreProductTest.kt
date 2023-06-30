package com.revenuecat.purchases.amazon

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoreProductTest {

    private val price = Price(
        formatted = "$1.00",
        amountMicros = 100,
        currencyCode = "USD"
    )

    private val period = Period.create("P1M")

    @Test
    fun `Two StoreProducts with the same properties are equal and have same hashcode`() {
        val product1 = AmazonStoreProduct(
            id = "productId",
            type = ProductType.SUBS,
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
}
