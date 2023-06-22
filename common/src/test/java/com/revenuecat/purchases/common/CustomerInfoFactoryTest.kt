package com.revenuecat.purchases.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.utils.Responses
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(AndroidJUnit4::class)
class CustomerInfoFactoryTest {

    private val defaultCustomerInfo = CustomerInfoFactory.buildCustomerInfo(
        JSONObject(Responses.validFullPurchaserResponse),
        overrideRequestDate = null,
        verificationResult = VerificationResult.NOT_REQUESTED
    )

    @Test
    fun `assigns active entitlements correctly`() {
        assertThat(defaultCustomerInfo.entitlements.active.keys).isEqualTo(setOf("pro", "forever_pro"))
    }

    @Test
    fun `assigns default schema version correctly`() {
        assertThat(defaultCustomerInfo.schemaVersion).isEqualTo(3)
    }

    @Test
    fun `assigns default request date correctly`() {
        assertThat(defaultCustomerInfo.requestDate).isEqualTo(Date(1565951442000))
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `assigns default verification result correctly`() {
        assertThat(defaultCustomerInfo.entitlements.verification).isEqualTo(VerificationResult.NOT_REQUESTED)
    }
}
