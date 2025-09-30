@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.utils

import android.content.Context
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.common.canUsePaywallUI
import com.revenuecat.purchases.paywalls.components.VideoComponent
import com.revenuecat.purchases.storage.DefaultFileRepository
import com.revenuecat.purchases.storage.FileRepository

internal class OfferingVideoPredownloader(
    context: Context,
    private val fileRepository: FileRepository = DefaultFileRepository(context),
) {
    private val shouldPredownload: Boolean = canUsePaywallUI

    fun downloadVideos(offering: Offering) {
        if (shouldPredownload) {
            offering.paywallComponents?.data?.componentsConfig?.base?.stack
                ?.filter { it is VideoComponent }
                ?.forEach { component ->
                    if (component is VideoComponent) {
                        val videos = setOfNotNull(
                            component.source.light.url,
                            component.source.light.urlLowRes,
                            component.source.dark?.url,
                            component.source.dark?.urlLowRes,
                        )
                        fileRepository.prefetch(videos.toList())
                    }
                }
        }
    }
}
