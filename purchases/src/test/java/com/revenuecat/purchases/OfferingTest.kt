package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.utils.stubStoreProduct
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfferingTest {

    @Test
    @OptIn(InternalRevenueCatAPI::class)
    fun `presentedOfferingContext returns context from first package`() {
        val context = PresentedOfferingContext("my-offering")
        val pkg = Package(
            identifier = "\$rc_monthly",
            packageType = PackageType.MONTHLY,
            product = stubStoreProduct("monthly_product"),
            presentedOfferingContext = context,
        )
        val offering = Offering(
            identifier = "my-offering",
            serverDescription = "",
            metadata = emptyMap(),
            availablePackages = listOf(pkg),
        )

        assertThat(offering.presentedOfferingContext).isEqualTo(context)
    }

    @Test
    @OptIn(InternalRevenueCatAPI::class)
    fun `presentedOfferingContext returns null when no packages`() {
        val offering = Offering(
            identifier = "empty-offering",
            serverDescription = "",
            metadata = emptyMap(),
            availablePackages = emptyList(),
        )

        assertThat(offering.presentedOfferingContext).isNull()
    }
}
