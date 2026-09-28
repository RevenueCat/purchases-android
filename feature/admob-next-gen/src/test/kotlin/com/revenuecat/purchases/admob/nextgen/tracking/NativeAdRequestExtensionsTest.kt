package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.revenuecat.purchases.ads.events.types.AdFormat
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class NativeAdRequestExtensionsTest {

    @Test
    fun `failure format is banner only when request only accepts banners`() {
        assertEquals(AdFormat.BANNER, requestWith(NativeAd.NativeAdType.BANNER).failureAdFormat())
        assertEquals(AdFormat.NATIVE, requestWith(NativeAd.NativeAdType.NATIVE).failureAdFormat())
        assertEquals(
            AdFormat.NATIVE,
            requestWith(NativeAd.NativeAdType.NATIVE, NativeAd.NativeAdType.BANNER).failureAdFormat(),
        )
        assertEquals(AdFormat.NATIVE, requestWith().failureAdFormat())
    }

    private fun requestWith(vararg nativeAdTypes: NativeAd.NativeAdType): NativeAdRequest = mockk {
        every { this@mockk.nativeAdTypes } returns nativeAdTypes.toList()
    }
}
