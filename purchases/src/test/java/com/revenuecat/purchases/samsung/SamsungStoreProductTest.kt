package com.revenuecat.purchases.samsung

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SamsungStoreProductTest {

    private val price = Price(
        formatted = "$1.00",
        amountMicros = 1_000_000,
        currencyCode = "USD",
    )

    private val period = Period.create("P1M")

    @Test
    fun `copyWithOfferingId copies product with offeringId and preserves context details`() {
        val originalPresentedOfferingContext = PresentedOfferingContext(
            offeringIdentifier = "originalOfferingId",
            placementIdentifier = "placementId",
            targetingContext = PresentedOfferingContext.TargetingContext(
                revision = 1,
                ruleId = "rule-id",
            ),
        )
        val product = createProduct(originalPresentedOfferingContext)

        val expectedOfferingId = "newOfferingId"
        val copiedProduct = product.copyWithOfferingId(expectedOfferingId) as SamsungStoreProduct

        assertThat(copiedProduct.id).isEqualTo(product.id)
        assertThat(copiedProduct.type).isEqualTo(product.type)
        assertThat(copiedProduct.price).isEqualTo(product.price)
        assertThat(copiedProduct.name).isEqualTo(product.name)
        assertThat(copiedProduct.title).isEqualTo(product.title)
        assertThat(copiedProduct.description).isEqualTo(product.description)
        assertThat(copiedProduct.period).isEqualTo(product.period)
        assertThat(copiedProduct.subscriptionOptions).isEqualTo(product.subscriptionOptions)
        assertThat(copiedProduct.defaultOption).isEqualTo(product.defaultOption)

        assertThat(copiedProduct.presentedOfferingContext!!.offeringIdentifier).isEqualTo(expectedOfferingId)
        assertThat(copiedProduct.presentedOfferingContext.placementIdentifier)
            .isEqualTo(originalPresentedOfferingContext.placementIdentifier)
        assertThat(copiedProduct.presentedOfferingContext.targetingContext)
            .isEqualTo(originalPresentedOfferingContext.targetingContext)
        assertThat(product.presentedOfferingContext!!.offeringIdentifier).isEqualTo("originalOfferingId")
    }

    @Test
    fun `copyWithOfferingId sets presentedOfferingContext when it was originally null`() {
        val product = createProduct(presentedOfferingContext = null)

        val expectedOfferingId = "newOfferingId"
        val copiedProduct = product.copyWithOfferingId(expectedOfferingId) as SamsungStoreProduct

        assertThat(copiedProduct.presentedOfferingContext!!.offeringIdentifier).isEqualTo(expectedOfferingId)
        assertThat(copiedProduct.presentedOfferingContext.placementIdentifier).isNull()
        assertThat(copiedProduct.presentedOfferingContext.targetingContext).isNull()
    }

    @Test
    fun `copyWithPresentedOfferingContext copies product with provided context`() {
        val originalPresentedOfferingContext = PresentedOfferingContext(
            offeringIdentifier = "originalOfferingId",
            placementIdentifier = "placementId",
            targetingContext = PresentedOfferingContext.TargetingContext(
                revision = 1,
                ruleId = "rule-id",
            ),
        )
        val product = createProduct(originalPresentedOfferingContext)

        val newPresentedOfferingContext = PresentedOfferingContext(
            offeringIdentifier = "newOfferingId",
            placementIdentifier = "newPlacementId",
            targetingContext = PresentedOfferingContext.TargetingContext(
                revision = 2,
                ruleId = "new-rule-id",
            ),
        )
        val copiedProduct = product.copyWithPresentedOfferingContext(newPresentedOfferingContext) as SamsungStoreProduct

        assertThat(copiedProduct.id).isEqualTo(product.id)
        assertThat(copiedProduct.type).isEqualTo(product.type)
        assertThat(copiedProduct.price).isEqualTo(product.price)
        assertThat(copiedProduct.name).isEqualTo(product.name)
        assertThat(copiedProduct.title).isEqualTo(product.title)
        assertThat(copiedProduct.description).isEqualTo(product.description)
        assertThat(copiedProduct.period).isEqualTo(product.period)
        assertThat(copiedProduct.subscriptionOptions).isEqualTo(product.subscriptionOptions)
        assertThat(copiedProduct.defaultOption).isEqualTo(product.defaultOption)
        assertThat(copiedProduct.presentedOfferingContext).isEqualTo(newPresentedOfferingContext)
        assertThat(product.presentedOfferingContext).isEqualTo(originalPresentedOfferingContext)
    }

    @Test
    fun `copyWithPresentedOfferingContext removes context when passed null`() {
        val product = createProduct(
            PresentedOfferingContext(
                offeringIdentifier = "originalOfferingId",
                placementIdentifier = "placementId",
                targetingContext = null,
            ),
        )

        val copiedProduct = product.copyWithPresentedOfferingContext(null) as SamsungStoreProduct

        assertThat(copiedProduct.presentedOfferingContext).isNull()
        assertThat(product.presentedOfferingContext!!.offeringIdentifier).isEqualTo("originalOfferingId")
    }

    private fun createProduct(
        presentedOfferingContext: PresentedOfferingContext? = null,
    ) = SamsungStoreProduct(
        id = "productId",
        type = ProductType.SUBS,
        price = price,
        name = "title",
        title = "title",
        description = "description",
        period = period,
        subscriptionOptions = null,
        defaultOption = null,
        presentedOfferingContext = presentedOfferingContext,
    )
}
