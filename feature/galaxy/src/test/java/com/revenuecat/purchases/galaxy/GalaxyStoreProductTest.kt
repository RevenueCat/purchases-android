package com.revenuecat.purchases.galaxy

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.SubscriptionOptions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GalaxyStoreProductTest : GalaxyStoreTest() {

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
        val copiedProduct = product.copyWithOfferingId(expectedOfferingId) as GalaxyStoreProduct

        assertThat(copiedProduct.id).isEqualTo(product.id)
        assertThat(copiedProduct.type).isEqualTo(product.type)
        assertThat(copiedProduct.price).isEqualTo(product.price)
        assertThat(copiedProduct.name).isEqualTo(product.name)
        assertThat(copiedProduct.title).isEqualTo(product.title)
        assertThat(copiedProduct.description).isEqualTo(product.description)
        assertThat(copiedProduct.period).isEqualTo(product.period)
        assertThat(copiedProduct.subscriptionOptions).isEqualTo(product.subscriptionOptions)
        assertThat(copiedProduct.defaultOption).isEqualTo(product.defaultOption)

        val copiedContext = requireNotNull(copiedProduct.presentedOfferingContext)
        assertThat(copiedContext.offeringIdentifier).isEqualTo(expectedOfferingId)
        assertThat(copiedContext.placementIdentifier).isEqualTo(originalPresentedOfferingContext.placementIdentifier)
        assertThat(copiedContext.targetingContext).isEqualTo(originalPresentedOfferingContext.targetingContext)
        assertThat(requireNotNull(product.presentedOfferingContext).offeringIdentifier).isEqualTo("originalOfferingId")
    }

    @Test
    fun `copyWithOfferingId sets presentedOfferingContext when it was originally null`() {
        val product = createProduct(presentedOfferingContext = null)

        val expectedOfferingId = "newOfferingId"
        val copiedProduct = product.copyWithOfferingId(expectedOfferingId) as GalaxyStoreProduct

        val copiedContext = requireNotNull(copiedProduct.presentedOfferingContext)
        assertThat(copiedContext.offeringIdentifier).isEqualTo(expectedOfferingId)
        assertThat(copiedContext.placementIdentifier).isNull()
        assertThat(copiedContext.targetingContext).isNull()
        assertThat(product.presentedOfferingContext).isNull()
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
        val copiedProduct = product.copyWithPresentedOfferingContext(newPresentedOfferingContext) as GalaxyStoreProduct

        assertThat(copiedProduct.id).isEqualTo(product.id)
        assertThat(copiedProduct.type).isEqualTo(product.type)
        assertThat(copiedProduct.price).isEqualTo(product.price)
        assertThat(copiedProduct.name).isEqualTo(product.name)
        assertThat(copiedProduct.title).isEqualTo(product.title)
        assertThat(copiedProduct.description).isEqualTo(product.description)
        assertThat(copiedProduct.period).isEqualTo(product.period)
        assertThat(copiedProduct.subscriptionOptions).isEqualTo(product.subscriptionOptions)
        assertThat(copiedProduct.defaultOption).isEqualTo(product.defaultOption)
        assertThat(requireNotNull(copiedProduct.presentedOfferingContext)).isEqualTo(newPresentedOfferingContext)
        assertThat(requireNotNull(product.presentedOfferingContext)).isEqualTo(originalPresentedOfferingContext)
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

        val copiedProduct = product.copyWithPresentedOfferingContext(null) as GalaxyStoreProduct

        assertThat(copiedProduct.presentedOfferingContext).isNull()
        assertThat(requireNotNull(product.presentedOfferingContext).offeringIdentifier).isEqualTo("originalOfferingId")
    }

    @Test
    fun `copyWithPresentedOfferingContext updates subscription options with new context`() {
        val originalPresentedOfferingContext = PresentedOfferingContext(
            offeringIdentifier = "originalOfferingId",
            placementIdentifier = "originalPlacementId",
            targetingContext = null,
        )
        val subscriptionOption1 = createGalaxySubscriptionOption(
            tags = emptyList(),
            presentedOfferingContext = originalPresentedOfferingContext,
            price = price,
            period = period,
        )
        val subscriptionOption2 = createGalaxySubscriptionOption(
            tags = listOf("second"),
            presentedOfferingContext = originalPresentedOfferingContext,
            price = price,
            period = period,
        )
        val product = createProduct(
            presentedOfferingContext = originalPresentedOfferingContext,
            subscriptionOptions = SubscriptionOptions(listOf(subscriptionOption1, subscriptionOption2)),
            defaultSubscriptionOption = subscriptionOption1,
        )

        val newPresentedOfferingContext = PresentedOfferingContext(
            offeringIdentifier = "newOfferingId",
            placementIdentifier = "newPlacementId",
            targetingContext = PresentedOfferingContext.TargetingContext(
                revision = 2,
                ruleId = "new-rule-id",
            ),
        )

        val copiedProduct = product.copyWithPresentedOfferingContext(newPresentedOfferingContext) as GalaxyStoreProduct
        assertThat(copiedProduct.presentedOfferingContext).isEqualTo(newPresentedOfferingContext)

        val copiedOptions = requireNotNull(copiedProduct.subscriptionOptions)
        assertThat(copiedOptions).hasSize(2)

        val copiedFirstOption = copiedOptions[0] as GalaxySubscriptionOption
        val copiedSecondOption = copiedOptions[1] as GalaxySubscriptionOption

        assertThat(copiedFirstOption.presentedOfferingContext).isEqualTo(newPresentedOfferingContext)
        assertThat(copiedSecondOption.presentedOfferingContext).isEqualTo(newPresentedOfferingContext)
        assertThat(copiedFirstOption.pricingPhases).isEqualTo(subscriptionOption1.pricingPhases)
        assertThat(copiedSecondOption.pricingPhases).isEqualTo(subscriptionOption2.pricingPhases)
        assertThat(copiedFirstOption.tags).isEqualTo(subscriptionOption1.tags)
        assertThat(copiedSecondOption.tags).isEqualTo(subscriptionOption2.tags)
        assertThat(copiedFirstOption.purchasingData).isEqualTo(subscriptionOption1.purchasingData)
        assertThat(copiedSecondOption.purchasingData).isEqualTo(subscriptionOption2.purchasingData)
        assertThat(copiedProduct.defaultOption).isSameAs(copiedFirstOption)

        assertThat(subscriptionOption1.presentedOfferingContext).isEqualTo(originalPresentedOfferingContext)
        assertThat(subscriptionOption2.presentedOfferingContext).isEqualTo(originalPresentedOfferingContext)
    }

    @Test
    fun `copyWithPresentedOfferingContext clears subscription option context when passed null`() {
        val originalPresentedOfferingContext = PresentedOfferingContext(
            offeringIdentifier = "originalOfferingId",
            placementIdentifier = "originalPlacementId",
            targetingContext = null,
        )
        val subscriptionOption = createGalaxySubscriptionOption(
            presentedOfferingContext = originalPresentedOfferingContext,
            price = price,
            period = period,
        )
        val product = createProduct(
            presentedOfferingContext = originalPresentedOfferingContext,
            subscriptionOptions = SubscriptionOptions(listOf(subscriptionOption)),
            defaultSubscriptionOption = subscriptionOption,
        )

        val copiedProduct = product.copyWithPresentedOfferingContext(null) as GalaxyStoreProduct

        val copiedOptions = requireNotNull(copiedProduct.subscriptionOptions)
        assertThat(copiedOptions).hasSize(1)

        val copiedOption = copiedOptions[0] as GalaxySubscriptionOption
        assertThat(copiedProduct.presentedOfferingContext).isNull()
        assertThat(copiedOption.presentedOfferingContext).isNull()
        assertThat(copiedProduct.defaultOption).isSameAs(copiedOption)

        assertThat(subscriptionOption.presentedOfferingContext).isEqualTo(originalPresentedOfferingContext)
    }

    private fun createProduct(
        presentedOfferingContext: PresentedOfferingContext? = null,
        subscriptionOptions: SubscriptionOptions? = null,
        defaultSubscriptionOption: SubscriptionOption? = null,
    ) = GalaxyStoreProduct(
        id = "productId",
        type = ProductType.SUBS,
        price = price,
        name = "title",
        title = "title",
        description = "description",
        period = period,
        subscriptionOptions = subscriptionOptions,
        defaultOption = defaultSubscriptionOption,
        presentedOfferingContext = presentedOfferingContext,
    )
}
