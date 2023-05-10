package com.revenuecat.purchases

import android.net.Uri
import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.models.GoogleProrationMode
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.PurchaseType
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.utils.JSONObjectParceler
import com.revenuecat.purchases.utils.JSONObjectParceler.write
import com.revenuecat.purchases.utils.Responses
import com.revenuecat.purchases.utils.testParcelization
import org.assertj.core.api.Assertions
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(AndroidJUnit4::class)
class ParcelableTests {

    @Test
    fun `EntitlementInfo is Parcelable`() = testParcelization(getEntitlementInfo())

    @Test
    fun `EntitlementInfos is Parcelable`() = testParcelization(
        EntitlementInfos(
            mapOf("an_identifier" to getEntitlementInfo(identifier = "an_identifier")),
            verification = VerificationResult.NOT_REQUESTED
        )
    )

    @Test
    fun `CustomerInfo is Parcelable`() = testParcelization(
        CustomerInfo(
            entitlements = EntitlementInfos(
                mapOf("an_identifier" to getEntitlementInfo(identifier = "an_identifier")),
                VerificationResult.VERIFIED
            ),
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
    fun `StoreTransaction is parcelable`() {
        testParcelization(
            StoreTransaction(
                "orderId",
                listOf("productId1", "productId2"),
                ProductType.UNKNOWN,
                0L,
                "purchaseToken",
                PurchaseState.PENDING,
                true,
                null,
                JSONObject(emptyMap<String, String>()),
                "offering_a",
                "userId",
                PurchaseType.GOOGLE_PURCHASE,
                null,
                "optionId",
                replacementMode = null
            )
        )
    }

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
    fun `GoogleProrationMode is Parcelable`() {
        GoogleProrationMode.values().forEach { testParcelization(it, true) }
        val nullMode: GoogleProrationMode? = null
        testParcelization(nullMode, true)
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
            productPlanIdentifier = null,
            isSandbox = false,
            unsubscribeDetectedAt = null,
            billingIssueDetectedAt = null,
            ownershipType = OwnershipType.UNKNOWN,
            jsonObject = JSONObject(Responses.validFullPurchaserResponse),
            verification = VerificationResult.NOT_REQUESTED
        )
    }
}
