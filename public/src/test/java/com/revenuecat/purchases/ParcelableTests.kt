package com.revenuecat.purchases

import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.parceler.JSONObjectParceler
import com.revenuecat.purchases.parceler.JSONObjectParceler.write
import com.revenuecat.purchases.parceler.SkuDetailsParceler
import com.revenuecat.purchases.parceler.SkuDetailsParceler.write
import com.revenuecat.purchases.utils.Responses
import com.revenuecat.purchases.utils.stubSkuDetails
import org.assertj.core.api.Assertions
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(AndroidJUnit4::class)
class ParcelableTests {

    @Test
    fun `Package is Parcelable`() = testParcelization(
        Package(
            identifier = "test_package",
            packageType = PackageType.MONTHLY,
            product = stubSkuDetails(),
            offering = "test"
        )
    )

    @Test
    fun `Offering is Parcelable`() {
        val aPackage = Package(
            identifier = "test_package",
            packageType = PackageType.MONTHLY,
            product = stubSkuDetails(),
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

    @Test
    fun `EntitlementInfo is Parcelable`() = testParcelization(getEntitlementInfo())

    @Test
    fun `EntitlementInfos is Parcelable`() = testParcelization(
        EntitlementInfos(mapOf("an_identifier" to getEntitlementInfo(identifier = "an_identifier")))
    )

    @Test
    fun `PurchaserInfo is Parcelable`() = testParcelization(
        PurchaserInfo(
            entitlements = EntitlementInfos(mapOf("an_identifier" to getEntitlementInfo(identifier = "an_identifier"))),
            purchasedNonSubscriptionSkus = setOf(),
            allExpirationDatesByProduct = mapOf("a_product" to Date(System.currentTimeMillis())),
            allPurchaseDatesByProduct = mapOf("a_product" to Date(System.currentTimeMillis())),
            requestDate = Date(System.currentTimeMillis()),
            jsonObject = JSONObject(Responses.validFullPurchaserResponse),
            schemaVersion = 0,
            firstSeen = Date(System.currentTimeMillis()),
            originalAppUserId = "original_app_user_id",
            managementURL = Uri.parse("https://management.com"),
            originalPurchaseDate = Date(System.currentTimeMillis())
        )
    )

    @Test
    fun `JSONObjectParceler works`() {
        val expected = JSONObject(Responses.validFullPurchaserResponse)

        val parcel = Parcel.obtain()
        expected.write(parcel, 0)
        parcel.setDataPosition(0)

        val created = JSONObjectParceler.create(parcel)

        Assertions.assertThat(expected.toString()).isEqualTo(created.toString())
    }

    @Test
    fun `SkuDetailsParceler works`() {
        val expected = stubSkuDetails()

        val parcel = Parcel.obtain()
        expected.write(parcel, 0)
        parcel.setDataPosition(0)

        val created = SkuDetailsParceler.create(parcel)

        Assertions.assertThat(expected).isEqualTo(created)
    }

    private inline fun <reified T : Parcelable> testParcelization(value: T) {
        val key = "key"

        // We don't have a good way to access the CREATOR (especially if the
        // tested Parcelable uses @Parcelize annotation), so we have to wrap
        // it in a bundle.
        val inputBundle = Bundle()
        inputBundle.putParcelable(key, value)

        val parcel = Parcel.obtain()
        try {
            inputBundle.writeToParcel(parcel, 0)

            // rewind the parcel to be ready to be read
            parcel.setDataPosition(0)

            // extract the bundle and a wrapped parcelable
            val outputBundle = parcel.readBundle()!!
            outputBundle.classLoader = T::class.java.classLoader
            val outputValue = outputBundle.getParcelable<T>(key)

            // assert that the parcelization succeeded
            assertNotSame(inputBundle, outputBundle)
            assertNotSame(value, outputValue)
            assertEquals(value, outputValue)
        } finally {
            parcel.recycle()
        }
    }

    private fun getEntitlementInfo(
        identifier: String = "an_identifier"
    ): EntitlementInfo {
        val purchaseDate = Date(System.currentTimeMillis() - 10_000)
        val expirationDate = Date(System.currentTimeMillis() + 1_000_000)
        return EntitlementInfo(
            identifier = identifier,
            isActive = true,
            willRenew = false,
            periodType = PeriodType.NORMAL,
            latestPurchaseDate = purchaseDate,
            originalPurchaseDate = purchaseDate,
            expirationDate = expirationDate,
            store = Store.PLAY_STORE,
            productIdentifier = "product_identifier",
            isSandbox = false,
            unsubscribeDetectedAt = null,
            billingIssueDetectedAt = null
        )
    }
}
