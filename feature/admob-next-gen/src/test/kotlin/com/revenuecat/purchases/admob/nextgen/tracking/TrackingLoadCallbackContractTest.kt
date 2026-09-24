package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import org.junit.Test

class TrackingLoadCallbackContractTest {

    @Test
    fun `load callback overrides every SDK callback`() {
        assertOverridesAllSdkCallbacks(AdLoadCallback::class.java, TrackingAdLoadCallback::class.java)
    }

    @Test
    fun `banner refresh callback overrides every SDK callback`() {
        assertOverridesAllSdkCallbacks(BannerAdRefreshCallback::class.java, TrackingBannerAdRefreshCallback::class.java)
    }

    @Test
    fun `native loader callback overrides every SDK callback`() {
        assertOverridesAllSdkCallbacks(NativeAdLoaderCallback::class.java, TrackingNativeAdLoaderCallback::class.java)
    }
}
