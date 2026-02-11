@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.utils

import android.content.Context
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.common.canUsePaywallUI
import com.revenuecat.purchases.models.Checksum
import com.revenuecat.purchases.paywalls.components.VideoComponent
import com.revenuecat.purchases.paywalls.components.properties.ThemeVideoUrls
import com.revenuecat.purchases.storage.DefaultFileRepository
import com.revenuecat.purchases.storage.FileRepository
import java.net.URL

internal class OfferingVideoPredownloader(
    context: Context,
    canShowPaywalls: Boolean = canUsePaywallUI,
    private val fileRepository: FileRepository = DefaultFileRepository(context),
) {
    private val shouldPredownload: Boolean = canShowPaywalls

    public fun downloadVideos(offering: Offering) {
        if (shouldPredownload) {
            // WIP: We will add a remote flag in the offering metadata that will indicate if we should
            // pre-download videos or not. For now, we want to only download the low-res to ensure we
            // don't rack up high cloudfront costs
            offering.paywallComponents?.data?.componentsConfig?.base?.stack
                ?.filter { it is VideoComponent }
                ?.forEach { component ->
                    if (component is VideoComponent) {
                        fileRepository.prefetch(component.source.checkedUrls())
                    }
                }
        }
    }
}

private fun ThemeVideoUrls.checkedUrls(): List<Pair<URL, Checksum?>> = listOfNotNull(
    light.url to light.checksum,
    dark?.url?.let { it to dark.checksum },
    light.urlLowRes?.let { it to light.checksumLowRes },
    dark?.urlLowRes?.let { it to dark.checksumLowRes },
)
