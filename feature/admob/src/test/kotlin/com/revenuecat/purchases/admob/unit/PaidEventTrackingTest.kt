@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob

import com.google.android.gms.ads.OnPaidEventListener
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.ads.events.types.AdFormat
import org.junit.Assert.assertNotNull
import org.junit.Test

class PaidEventTrackingTest {

    @Test
    fun `setUpPaidEventTracking installs listener`() {
        var captured: OnPaidEventListener? = null

        setUpPaidEventTracking(
            setListener = { captured = it },
            adFormat = AdFormat.INTERSTITIAL,
            placement = "home_interstitial",
            adUnitId = "test-ad-unit",
            responseInfoProvider = { throw AssertionError("Not expected in listener wiring test") },
            delegate = null,
        )

        assertNotNull(captured)
    }
}
