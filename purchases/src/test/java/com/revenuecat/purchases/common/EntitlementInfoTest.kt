package com.revenuecat.purchases.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.OwnershipType
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.VerificationResult
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date
import kotlin.time.Duration.Companion.days

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class EntitlementInfoTest {

    private val oneDayAgo = 1.days.ago()
    private val twoDaysAgo = 2.days.ago()
    private val oneDayFromNow = 1.days.fromNow()

    @Test
    fun `same entitlement info are equal`() {
        val entitlementInfo1 = createEntitlementInfo()
        val entitlementInfo2 = createEntitlementInfo()
        assertThat(entitlementInfo1).isEqualTo(entitlementInfo2)
    }

    @Test
    fun `same entitlement info with different verification are not equal`() {
        val entitlementInfo1 = createEntitlementInfo(verification = VerificationResult.NOT_REQUESTED)
        val entitlementInfo2 = createEntitlementInfo(verification = VerificationResult.FAILED)
        val entitlementInfo3 = createEntitlementInfo(verification = VerificationResult.VERIFIED)
        val entitlementInfo4 = createEntitlementInfo(verification = VerificationResult.VERIFIED_ON_DEVICE)
        assertThat(entitlementInfo1).isNotEqualTo(entitlementInfo2)
        assertThat(entitlementInfo1).isNotEqualTo(entitlementInfo3)
        assertThat(entitlementInfo1).isNotEqualTo(entitlementInfo4)
        assertThat(entitlementInfo2).isNotEqualTo(entitlementInfo3)
        assertThat(entitlementInfo2).isNotEqualTo(entitlementInfo4)
        assertThat(entitlementInfo3).isNotEqualTo(entitlementInfo4)
    }

    private fun createEntitlementInfo(
        identifier: String = "test-entitlement-info-id",
        isActive: Boolean = true,
        willRenew: Boolean = true,
        periodType: PeriodType = PeriodType.NORMAL,
        latestPurchaseDate: Date = oneDayAgo,
        originalPurchaseDate: Date = twoDaysAgo,
        expirationDate: Date? = oneDayFromNow,
        store: Store = Store.PLAY_STORE,
        productIdentifier: String = "test-product-id",
        productPlanIdentifier: String = "test-plan-id",
        isSandbox: Boolean = false,
        unsubscribeDetectedAt: Date? = null,
        billingIssueDetectedAt: Date? = null,
        ownershipType: OwnershipType = OwnershipType.PURCHASED,
        jsonObject: JSONObject = JSONObject("{}"),
        verification: VerificationResult = VerificationResult.NOT_REQUESTED
    ): EntitlementInfo {
        return EntitlementInfo(
            identifier,
            isActive,
            willRenew,
            periodType,
            latestPurchaseDate,
            originalPurchaseDate,
            expirationDate,
            store,
            productIdentifier,
            productPlanIdentifier,
            isSandbox,
            unsubscribeDetectedAt,
            billingIssueDetectedAt,
            ownershipType,
            jsonObject,
            verification
        )
    }
}
