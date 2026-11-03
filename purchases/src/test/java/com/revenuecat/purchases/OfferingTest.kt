package com.revenuecat.purchases

import android.os.Parcel
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

    @Test
    fun `parcel roundtrip retains workflow attribution`() {
        val original = PresentedOfferingContext("offering")
            .copyWithWorkflowContext("workflow", "step", "blob-ref")
        val parcel = Parcel.obtain()
        try {
            parcel.writeParcelable(original, 0)
            parcel.setDataPosition(0)
            @Suppress("DEPRECATION")
            val decoded = parcel.readParcelable<PresentedOfferingContext>(PresentedOfferingContext::class.java.classLoader)

            assertThat(decoded).isEqualTo(original)
        } finally {
            parcel.recycle()
        }
    }
}
