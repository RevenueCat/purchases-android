@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.utils

import android.content.Context
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.models.Checksum
import com.revenuecat.purchases.paywalls.PaywallAssetWarming
import com.revenuecat.purchases.paywalls.components.VideoComponent
import com.revenuecat.purchases.paywalls.components.properties.ThemeVideoUrls
import com.revenuecat.purchases.storage.DefaultFileRepository
import com.revenuecat.purchases.storage.FileRepository
import java.net.URL

internal class OfferingVideoPredownloader(
    context: Context,
    private val assetWarming: PaywallAssetWarming,
    private val fileRepository: FileRepository = DefaultFileRepository(context),
) {

    fun downloadVideos(offering: Offering) {
        if (!assetWarming.isAvailable) return
        val stack = offering.baseComponentsConfig()?.stack ?: return
        // WIP: We will add a remote flag in the offering metadata that will indicate if we should
        // pre-download videos or not. For now, we want to only download the low-res to ensure we
        // don't rack up high cloudfront costs
        stack.flatten()
            .filterIsInstance<VideoComponent>()
            .forEach { fileRepository.prefetch(it.source.checkedUrls()) }
    }
}

private fun ThemeVideoUrls.checkedUrls(): List<Pair<URL, Checksum?>> = listOfNotNull(
    light.url to light.checksum,
    dark?.url?.let { it to dark.checksum },
    light.urlLowRes?.let { it to light.checksumLowRes },
    dark?.urlLowRes?.let { it to dark.checksumLowRes },
)
