package com.revenuecat.purchases.ui.revenuecatui.helpers

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PresentedOfferingContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaywallProductIdentifierTest {

    @Test
    fun `paywallPackageRowSelection uses canonical product id for Google subscriptions`() {
        val googleProduct = createGoogleStoreProduct(
            productId = "com.revenuecat.monthly_product",
            basePlanId = "monthly_base_plan",
        )
        val rcPackage = Package(
            identifier = "\$rc_monthly",
            packageType = PackageType.MONTHLY,
            product = googleProduct,
            presentedOfferingContext = PresentedOfferingContext(offeringIdentifier = "offering-id"),
        )

        val interaction = paywallPackageRowSelection(
            destination = rcPackage,
            origin = rcPackage,
            defaultPackage = rcPackage,
        )

        assertThat(googleProduct.id).isEqualTo("com.revenuecat.monthly_product:monthly_base_plan")
        assertThat(interaction.originProductIdentifier).isEqualTo("com.revenuecat.monthly_product")
        assertThat(interaction.destinationProductIdentifier).isEqualTo("com.revenuecat.monthly_product")
        assertThat(interaction.defaultProductIdentifier).isEqualTo("com.revenuecat.monthly_product")
    }

    @Test
    fun `paywall product identifier uses store product id for non Google products`() {
        val product = stubStoreProduct(productId = "com.revenuecat.monthly_product")

        assertThat(product.paywallProductIdentifier()).isEqualTo(product.id)
    }
}
