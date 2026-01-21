package com.revenuecat.purchases.ui.revenuecatui.helpers

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.models.GoogleSubscriptionOption
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.paywalls.components.common.PlayStoreOfferConfig
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PlayStoreOfferResolverTests {

    private val mockProductDetails: ProductDetails = mockk()

    // region No Configuration

    @Test
    fun `resolve with no offer config returns NoConfiguration with defaultOption`() {
        // Arrange
        val basePlanOption = stubGoogleSubscriptionOption(
            productId = "product_id",
            basePlanId = "monthly",
            offerId = null,
            productDetails = mockProductDetails,
        )
        val storeProduct = createGoogleStoreProduct(
            productId = "product_id",
            basePlanId = "monthly",
            productDetails = mockProductDetails,
            subscriptionOptions = listOf(basePlanOption),
        )
        val rcPackage = Package(
            identifier = "\$rc_monthly",
            packageType = PackageType.MONTHLY,
            product = storeProduct,
            presentedOfferingContext = PresentedOfferingContext("offering_id"),
        )

        // Act
        val result = PlayStoreOfferResolver.resolve(rcPackage, offerConfig = null)

        // Assert
        assertThat(result).isInstanceOf(ResolvedOffer.NoConfiguration::class.java)
        assertThat(result.subscriptionOption).isNotNull
    }

    // endregion

    // region Configured Offer Found

    @Test
    fun `resolve with valid offer id returns ConfiguredOffer`() {
        // Arrange
        val basePlanOption = stubGoogleSubscriptionOption(
            productId = "product_id",
            basePlanId = "monthly",
            offerId = null,
            productDetails = mockProductDetails,
        )
        val promoOffer = stubGoogleSubscriptionOption(
            productId = "product_id",
            basePlanId = "monthly",
            offerId = "summer_promo",
            productDetails = mockProductDetails,
            pricingPhases = listOf(
                stubFreeTrialPricingPhase(billingPeriod = Period(7, Period.Unit.DAY, "P7D")),
                stubPricingPhase(billingPeriod = Period(1, Period.Unit.MONTH, "P1M")),
            ),
        )
        val storeProduct = createGoogleStoreProduct(
            productId = "product_id",
            basePlanId = "monthly",
            productDetails = mockProductDetails,
            subscriptionOptions = listOf(basePlanOption, promoOffer),
        )
        val rcPackage = Package(
            identifier = "\$rc_monthly",
            packageType = PackageType.MONTHLY,
            product = storeProduct,
            presentedOfferingContext = PresentedOfferingContext("offering_id"),
        )
        val offerConfig = PlayStoreOfferConfig(
            offerId = "summer_promo",
        )

        // Act
        val result = PlayStoreOfferResolver.resolve(rcPackage, offerConfig)

        // Assert
        assertThat(result).isInstanceOf(ResolvedOffer.ConfiguredOffer::class.java)
        val resolvedOption = result.subscriptionOption as GoogleSubscriptionOption
        assertThat(resolvedOption.offerId).isEqualTo("summer_promo")
    }

    // endregion

    // region Offer Not Found

    @Test
    fun `resolve with invalid offer id returns NoConfiguration with defaultOption`() {
        // Arrange
        val basePlanOption = stubGoogleSubscriptionOption(
            productId = "product_id",
            basePlanId = "monthly",
            offerId = null,
            productDetails = mockProductDetails,
        )
        val existingOffer = stubGoogleSubscriptionOption(
            productId = "product_id",
            basePlanId = "monthly",
            offerId = "existing_offer",
            productDetails = mockProductDetails,
            pricingPhases = listOf(
                stubFreeTrialPricingPhase(billingPeriod = Period(3, Period.Unit.DAY, "P3D")),
                stubPricingPhase(billingPeriod = Period(1, Period.Unit.MONTH, "P1M")),
            ),
        )
        val storeProduct = createGoogleStoreProduct(
            productId = "product_id",
            basePlanId = "monthly",
            productDetails = mockProductDetails,
            subscriptionOptions = listOf(basePlanOption, existingOffer),
        )
        val rcPackage = Package(
            identifier = "\$rc_monthly",
            packageType = PackageType.MONTHLY,
            product = storeProduct,
            presentedOfferingContext = PresentedOfferingContext("offering_id"),
        )
        val offerConfig = PlayStoreOfferConfig(
            offerId = "nonexistent_offer",
        )

        // Act
        val result = PlayStoreOfferResolver.resolve(rcPackage, offerConfig)

        // Assert
        assertThat(result).isInstanceOf(ResolvedOffer.NoConfiguration::class.java)
        assertThat(result.subscriptionOption).isNotNull
    }

    // endregion

    // region Edge Cases

    @Test
    fun `resolve with product without subscription options returns NoConfiguration`() {
        // Arrange
        val storeProduct = stubINAPPStoreProduct(productId = "consumable_product")
        val rcPackage = Package(
            identifier = "consumable",
            packageType = PackageType.CUSTOM,
            product = storeProduct,
            presentedOfferingContext = PresentedOfferingContext("offering_id"),
        )
        val offerConfig = PlayStoreOfferConfig(
            offerId = "some_offer",
        )

        // Act
        val result = PlayStoreOfferResolver.resolve(rcPackage, offerConfig)

        // Assert
        assertThat(result).isInstanceOf(ResolvedOffer.NoConfiguration::class.java)
    }

    @Test
    fun `resolve finds offer among multiple offers`() {
        // Arrange
        val basePlanOption = stubGoogleSubscriptionOption(
            productId = "product_id",
            basePlanId = "monthly",
            offerId = null,
            productDetails = mockProductDetails,
        )
        val offer1 = stubGoogleSubscriptionOption(
            productId = "product_id",
            basePlanId = "monthly",
            offerId = "offer_1",
            productDetails = mockProductDetails,
        )
        val offer2 = stubGoogleSubscriptionOption(
            productId = "product_id",
            basePlanId = "monthly",
            offerId = "offer_2",
            productDetails = mockProductDetails,
        )
        val offer3 = stubGoogleSubscriptionOption(
            productId = "product_id",
            basePlanId = "monthly",
            offerId = "offer_3",
            productDetails = mockProductDetails,
        )
        val storeProduct = createGoogleStoreProduct(
            productId = "product_id",
            basePlanId = "monthly",
            productDetails = mockProductDetails,
            subscriptionOptions = listOf(basePlanOption, offer1, offer2, offer3),
        )
        val rcPackage = Package(
            identifier = "\$rc_monthly",
            packageType = PackageType.MONTHLY,
            product = storeProduct,
            presentedOfferingContext = PresentedOfferingContext("offering_id"),
        )
        val offerConfig = PlayStoreOfferConfig(
            offerId = "offer_2",
        )

        // Act
        val result = PlayStoreOfferResolver.resolve(rcPackage, offerConfig)

        // Assert
        assertThat(result).isInstanceOf(ResolvedOffer.ConfiguredOffer::class.java)
        val resolvedOption = result.subscriptionOption as GoogleSubscriptionOption
        assertThat(resolvedOption.offerId).isEqualTo("offer_2")
    }

    // endregion
}
