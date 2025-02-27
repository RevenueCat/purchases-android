@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.utils

import android.net.Uri
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.CarouselComponent
import com.revenuecat.purchases.paywalls.components.IconComponent
import com.revenuecat.purchases.paywalls.components.ImageComponent
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.PaywallComponent
import com.revenuecat.purchases.paywalls.components.PurchaseButtonComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
import com.revenuecat.purchases.paywalls.components.TabControlButtonComponent
import com.revenuecat.purchases.paywalls.components.TabControlComponent
import com.revenuecat.purchases.paywalls.components.TabControlToggleComponent
import com.revenuecat.purchases.paywalls.components.TabsComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.TimelineComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls

internal class OfferingImagePreDownloader(
    /**
     * We check for the existance of the paywalls SDK. If so, the Coil SDK should be available to
     * pre-download the images.
     */
    private val shouldPredownloadImages: Boolean = try {
        Class.forName("com.revenuecat.purchases.ui.revenuecatui.PaywallKt")
        true
    } catch (_: ClassNotFoundException) {
        false
    },
    private val coilImageDownloader: CoilImageDownloader,
) {
    fun preDownloadOfferingImages(offering: Offering) {
        if (!shouldPredownloadImages) {
            verboseLog("OfferingImagePreDownloader won't pre-download images")
            return
        }

        debugLog("OfferingImagePreDownloader: starting image download")

        downloadV1Images(offering)
        downloadV2Images(offering)
    }

    private fun downloadV1Images(offering: Offering) {
        offering.paywall?.let { paywallData ->
            val imageUris = paywallData.config.images.all.map {
                Uri.parse(paywallData.assetBaseURL.toString()).buildUpon().path(it).build()
            }
            imageUris.forEach {
                debugLog("Pre-downloading Paywall V1 image: $it")
                coilImageDownloader.downloadImage(it)
            }
        }
    }

    private fun downloadV2Images(offering: Offering) {
        offering.paywallComponents?.let { paywallComponents ->
            val imageUrls = findImageUrisToDownload(paywallComponents)
            imageUrls.forEach {
                debugLog("Pre-downloading Paywall V2 image: $it")
                coilImageDownloader.downloadImage(it)
            }
        }
    }

    private fun findImageUrisToDownload(paywallComponents: Offering.PaywallComponents): Set<Uri> {
        val paywallComponentsConfig = paywallComponents.data.componentsConfig.base

        return paywallComponentsConfig.stack.findImageUrisToDownload() +
            (paywallComponentsConfig.stickyFooter?.stack?.findImageUrisToDownload().orEmpty()) +
            paywallComponentsConfig.background.findImageUrisToDownload()
    }

    private fun StackComponent.findImageUrisToDownload(): Set<Uri> {
        return filter {
            it is StackComponent ||
                it is IconComponent ||
                it is CarouselComponent ||
                it is TabsComponent ||
                it is ImageComponent
        }.flatMapTo(mutableSetOf()) { component ->
            when (component) {
                is StackComponent -> {
                    component.background.findImageUrisToDownload() + component.overrides.flatMapTo(mutableSetOf()) {
                        it.properties.background.findImageUrisToDownload()
                    }
                }
                is IconComponent -> {
                    setOf(Uri.parse(component.baseUrl).buildUpon().path(component.formats.webp).build())
                }
                is CarouselComponent -> {
                    component.background.findImageUrisToDownload() + component.overrides.flatMapTo(mutableSetOf()) {
                        it.properties.background.findImageUrisToDownload()
                    }
                }
                is TabsComponent -> {
                    component.background.findImageUrisToDownload() + component.overrides.flatMapTo(mutableSetOf()) {
                        it.properties.background.findImageUrisToDownload()
                    }
                }
                is ImageComponent -> {
                    component.source.findImageUrisToDownload() + component.overrides.flatMapTo(mutableSetOf()) {
                        it.properties.source?.findImageUrisToDownload().orEmpty()
                    }
                }
                else -> emptySet()
            }
        }
    }

    private fun Background?.findImageUrisToDownload(): Set<Uri> {
        return when (this) {
            is Background.Image -> setOfNotNull(
                Uri.parse(value.light.webpLowRes.toString()),
                value.dark?.webpLowRes?.toString()?.let { Uri.parse(it) },
            )
            is Background.Color -> emptySet()
            null -> emptySet()
        }
    }

    private fun ThemeImageUrls.findImageUrisToDownload(): Set<Uri> {
        return setOfNotNull(
            light.webpLowRes.toString().let { Uri.parse(it) },
            dark?.webpLowRes?.toString()?.let { Uri.parse(it) },
        )
    }

    /**
     * Returns all PaywallComponent that satisfy the predicate.
     *
     * Implemented as breadth-first search.
     */
    @Suppress("CyclomaticComplexMethod")
    private fun PaywallComponent.filter(predicate: (PaywallComponent) -> Boolean): List<PaywallComponent> {
        val matches = mutableListOf<PaywallComponent>()
        val queue = ArrayDeque<PaywallComponent>()
        queue.add(this)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()

            if (predicate(current)) {
                matches.add(current)
            }

            when (current) {
                is StackComponent -> queue.addAll(current.components)
                is PurchaseButtonComponent -> queue.add(current.stack)
                is ButtonComponent -> queue.add(current.stack)
                is PackageComponent -> queue.add(current.stack)
                is StickyFooterComponent -> queue.add(current.stack)
                is CarouselComponent -> queue.addAll(current.pages)
                is TabControlButtonComponent -> queue.add(current.stack)
                is TabsComponent -> {
                    when (val control = current.control) {
                        is TabsComponent.TabControl.Buttons -> queue.add(control.stack)
                        is TabsComponent.TabControl.Toggle -> queue.add(control.stack)
                    }
                    queue.addAll(current.tabs.map { it.stack })
                }
                is TimelineComponent -> {
                    queue.addAll(current.items.flatMap { listOfNotNull(it.title, it.description, it.icon) })
                }

                is TabControlToggleComponent,
                is TabControlComponent,
                is ImageComponent,
                is IconComponent,
                is TextComponent,
                -> {
                    // These don't have child components.
                }
            }
        }

        return matches
    }
}
