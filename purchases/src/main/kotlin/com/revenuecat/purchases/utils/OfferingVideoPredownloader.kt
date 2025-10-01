@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.utils

import android.content.Context
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.common.canUsePaywallUI
import com.revenuecat.purchases.common.checkIfVideoComponentIsEnabled
import com.revenuecat.purchases.paywalls.components.VideoComponent
import com.revenuecat.purchases.storage.DefaultFileRepository
import com.revenuecat.purchases.storage.FileRepository

internal class OfferingVideoPredownloader(
    context: Context,
    canShowPaywalls: Boolean = canUsePaywallUI,
    videoComponentIsEnabled: () -> Boolean = ::checkIfVideoComponentIsEnabled,
    private val fileRepository: FileRepository = DefaultFileRepository(context),
) {
    private val shouldPredownload: Boolean = canShowPaywalls && videoComponentIsEnabled()

    fun downloadVideos(offering: Offering) {
        if (shouldPredownload) {
            // WIP: We will add a remote flag in the offering metadata that will indicate if we should download
            // the high res videos or not. For now, we want to only download the low-res to ensure we don't rack up
            // high cloudfront costs
            offering.paywallComponents?.data?.componentsConfig?.base?.stack
                ?.filter { it is VideoComponent }
                ?.forEach { component ->
                    if (component is VideoComponent) {
                        val lowResVideos = setOfNotNull(
                            component.source.light.urlLowRes,
                            component.source.dark?.urlLowRes,
                        )
                        fileRepository.prefetch(lowResVideos.toList())
                    }
                }
        }
    }
}
