package com.revenuecat.purchases.google

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.utils.mockProductDetails
import com.revenuecat.purchases.utils.testParcelization
import org.junit.Test
import org.junit.runner.RunWith
import com.revenuecat.purchases.Package as RevenueCatPackage

@RunWith(AndroidJUnit4::class)
class ParcelableTests {
    @Test
    fun `Package is Parcelable`() = testParcelization(
        RevenueCatPackage(
            identifier = "test_package",
            packageType = PackageType.MONTHLY,
            product = mockProductDetails().toInAppStoreProduct(),
            offering = "test"
        )
    )

    @Test
    fun `Offering is Parcelable`() {
        val aPackage = RevenueCatPackage(
            identifier = "test_package",
            packageType = PackageType.MONTHLY,
            product = mockProductDetails().toInAppStoreProduct(),
            offering = "test"
        )
        testParcelization(
            Offering(
                identifier = "test",
                serverDescription = "description test",
                availablePackages = listOf(aPackage)
            )
        )
    }
}
