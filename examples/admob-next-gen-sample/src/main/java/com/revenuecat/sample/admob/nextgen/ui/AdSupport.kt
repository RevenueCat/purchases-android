package com.revenuecat.sample.admob.nextgen.ui

import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun preloadStatusCallback(
    scope: CoroutineScope,
    onAdPreloaded: () -> Unit,
    updateStatus: (String) -> Unit,
): PreloadCallback = object : PreloadCallback {
    override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {
        scope.launch { onAdPreloaded() }
    }

    override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) {
        scope.launch { updateStatus("Preload failed: ${adError.message}") }
    }

    override fun onAdsExhausted(preloadId: String) {
        scope.launch { updateStatus("Buffer exhausted: $preloadId") }
    }
}
