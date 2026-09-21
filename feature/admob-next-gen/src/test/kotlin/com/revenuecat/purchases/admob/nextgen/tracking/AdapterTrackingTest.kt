@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen.tracking

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ads.events.AdCaptureMethod
import com.revenuecat.purchases.ads.events.AdTracker
import com.revenuecat.purchases.ads.events.types.AdDisplayedData
import com.revenuecat.purchases.ads.events.types.AdFailedToLoadData
import com.revenuecat.purchases.ads.events.types.AdLoadedData
import com.revenuecat.purchases.ads.events.types.AdOpenedData
import com.revenuecat.purchases.ads.events.types.AdRevenueData
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class AdapterTrackingTest {
    private val adTracker = mockk<AdTracker>(relaxed = true)

    @Test
    fun `loaded events use adapter capture`() {
        val data = mockk<AdLoadedData>()

        adTracker.trackFromAdapter(data)

        verify(exactly = 1) { adTracker.trackAdLoaded(data, AdCaptureMethod.ADAPTER) }
    }

    @Test
    fun `failed-to-load events use adapter capture`() {
        val data = mockk<AdFailedToLoadData>()

        adTracker.trackFromAdapter(data)

        verify(exactly = 1) { adTracker.trackAdFailedToLoad(data, AdCaptureMethod.ADAPTER) }
    }

    @Test
    fun `displayed events use adapter capture`() {
        val data = mockk<AdDisplayedData>()

        adTracker.trackFromAdapter(data)

        verify(exactly = 1) { adTracker.trackAdDisplayed(data, AdCaptureMethod.ADAPTER) }
    }

    @Test
    fun `opened events use adapter capture`() {
        val data = mockk<AdOpenedData>()

        adTracker.trackFromAdapter(data)

        verify(exactly = 1) { adTracker.trackAdOpened(data, AdCaptureMethod.ADAPTER) }
    }

    @Test
    fun `revenue events use adapter capture`() {
        val data = mockk<AdRevenueData>()

        adTracker.trackFromAdapter(data)

        verify(exactly = 1) { adTracker.trackAdRevenue(data, AdCaptureMethod.ADAPTER) }
    }
}
