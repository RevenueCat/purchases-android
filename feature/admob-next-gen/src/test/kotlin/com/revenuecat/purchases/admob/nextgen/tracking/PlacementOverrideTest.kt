package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.revenuecat.purchases.admob.nextgen.AdMobNextGenStrings
import com.revenuecat.purchases.admob.nextgen.Logger
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class PlacementOverrideTest {

    @Before
    fun setUp() {
        mockkObject(Logger)
        every { Logger.w(any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkObject(Logger)
    }

    @Test
    fun `updates placement on tracking callback`() {
        val callback = trackingCallback(initialPlacement = "load-placement")

        callback.applyPlacementOverride("show-placement")

        assertEquals("show-placement", callback.placement)
    }

    @Test
    fun `null override clears placement on tracking callback`() {
        val callback = trackingCallback(initialPlacement = "load-placement")

        callback.applyPlacementOverride(null)

        assertNull(callback.placement)
    }

    @Test
    fun `non-tracking callback is ignored`() {
        val callback = object : InterstitialAdEventCallback {}

        callback.applyPlacementOverride("show-placement")

        verify(exactly = 1) { Logger.w(AdMobNextGenStrings.PLACEMENT_OVERRIDE_IGNORED) }
    }

    private fun trackingCallback(initialPlacement: String?) = TrackingInterstitialAdEventCallback(
        initialDelegate = null,
        initialPlacement = initialPlacement,
        adUnitId = "ad-unit",
        responseInfoProvider = { mockk<ResponseInfo>() },
    )
}
