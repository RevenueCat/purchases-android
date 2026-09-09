package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.revenuecat.purchases.ads.events.types.AdFormat

/**
 * Infers the format of a failed native-loader request when the request makes that unambiguous.
 *
 * Google does not include the requested ad type in a failed load result. A request that only accepts banners can
 * safely be attributed to [AdFormat.BANNER]. Mixed, empty, and native-only requests remain [AdFormat.NATIVE].
 */
internal fun NativeAdRequest.failureAdFormat(): AdFormat {
    val requestedAdTypes = nativeAdTypes
    return if (requestedAdTypes.isNotEmpty() && requestedAdTypes.all { it == NativeAd.NativeAdType.BANNER }) {
        AdFormat.BANNER
    } else {
        AdFormat.NATIVE
    }
}
