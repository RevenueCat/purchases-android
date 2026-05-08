@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.utils

import android.net.Uri
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.canUsePaywallUI
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.CarouselComponent
import com.revenuecat.purchases.paywalls.components.CountdownComponent
import com.revenuecat.purchases.paywalls.components.FallbackHeaderComponent
import com.revenuecat.purchases.paywalls.components.HeaderComponent
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
import com.revenuecat.purchases.paywalls.components.VideoComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls

internal class PaywallComponentsImagePreDownloader(
    /**
     * We check for the existence of the paywalls SDK. If so, the Coil SDK should be available to
     * pre-download the images.
     */
    private val shouldPredownloadImages: Boolean = canUsePaywallUI,
    private val coilImageDownloader: CoilImageDownloader,
) {

    fun preDownloadImages(
        paywallComponentsConfig: PaywallComponentsConfig,
        localizations: Map<LocaleId, Map<LocalizationKey, LocalizationData>> = emptyMap(),
    ) {
        if (!shouldPredownloadImages) {
            verboseLog { "PaywallComponentsImagePreDownloader won't pre-download images" }
            return
        }

        val imageUrls = findImageUrisToDownload(paywallComponentsConfig, localizations)
        imageUrls.forEach {
            debugLog { "Pre-downloading Paywall V2 image: $it" }
            coilImageDownloader.downloadImage(it)
        }
    }

    private fun findImageUrisToDownload(
        paywallComponentsConfig: PaywallComponentsConfig,
        localizations: Map<LocaleId, Map<LocalizationKey, LocalizationData>>,
    ): Set<Uri> {
        return paywallComponentsConfig.stack.findImageUrisToDownload() +
            (paywallComponentsConfig.header?.stack?.findImageUrisToDownload().orEmpty()) +
            (paywallComponentsConfig.stickyFooter?.stack?.findImageUrisToDownload().orEmpty()) +
            paywallComponentsConfig.background.findImageUrisToDownload() +
            localizations.findImageUrisToDownload()
    }

    private fun StackComponent.findImageUrisToDownload(): Set<Uri> {
        return filter { true }
            .flatMapTo(mutableSetOf()) { component ->
                component.findImageUrisToDownload()
            }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun PaywallComponent.findImageUrisToDownload(): Set<Uri> {
        return when (this) {
            is StackComponent -> {
                background.findImageUrisToDownload() + overrides.flatMapTo(mutableSetOf()) {
                    it.properties.background.findImageUrisToDownload()
                }
            }
            is IconComponent -> {
                setOf(Uri.parse(baseUrl).buildUpon().path(formats.webp).build())
            }
            is CarouselComponent -> {
                background.findImageUrisToDownload() +
                    pages.flatMapTo(mutableSetOf()) {
                        it.findImageUrisToDownload()
                    } +
                    overrides.flatMapTo(mutableSetOf()) {
                        it.properties.background.findImageUrisToDownload()
                    }
            }
            is TabsComponent -> {
                background.findImageUrisToDownload() + overrides.flatMapTo(mutableSetOf()) {
                    it.properties.background.findImageUrisToDownload()
                }
            }
            is ImageComponent -> {
                source.findImageUrisToDownload() + overrides.flatMapTo(mutableSetOf()) {
                    it.properties.source?.findImageUrisToDownload().orEmpty()
                }
            }
            is VideoComponent -> {
                fallbackSource?.findImageUrisToDownload().orEmpty() +
                    overrides.orEmpty().flatMapTo(mutableSetOf()) {
                        it.properties.fallbackSource?.findImageUrisToDownload().orEmpty()
                    }
            }
            is CountdownComponent -> {
                countdownStack.findImageUrisToDownload() +
                    (endStack?.findImageUrisToDownload().orEmpty()) +
                    (fallback?.findImageUrisToDownload().orEmpty())
            }
            // These components don't carry image URIs themselves; their children
            // (if any) are visited by the BFS traversal in PaywallComponent.filter.
            is ButtonComponent,
            is FallbackHeaderComponent,
            is HeaderComponent,
            is PackageComponent,
            is PurchaseButtonComponent,
            is StickyFooterComponent,
            is TabControlButtonComponent,
            is TabControlComponent,
            is TabControlToggleComponent,
            is TextComponent,
            is TimelineComponent,
            -> emptySet()
        }
    }

    private fun Background?.findImageUrisToDownload(): Set<Uri> {
        return when (this) {
            is Background.Image -> setOfNotNull(
                Uri.parse(value.light.webpLowRes.toString()),
                value.dark?.webpLowRes?.toString()?.let { Uri.parse(it) },
            )
            is Background.Video -> setOfNotNull(
                Uri.parse(fallbackImage.light.webpLowRes.toString()),
                fallbackImage.dark?.webpLowRes?.toString()?.let { Uri.parse(it) },
            )
            is Background.Color,
            is Background.Unknown,
            null,
            -> emptySet()
        }
    }

    private fun ThemeImageUrls.findImageUrisToDownload(): Set<Uri> {
        return setOfNotNull(
            light.webpLowRes.toString().let { Uri.parse(it) },
            dark?.webpLowRes?.toString()?.let { Uri.parse(it) },
        )
    }

    private fun Map<LocaleId, Map<LocalizationKey, LocalizationData>>.findImageUrisToDownload(): Set<Uri> =
        values
            .flatMap { localization ->
                localization.values.mapNotNull { value ->
                    (value as? LocalizationData.Image)?.value
                }
            }
            .flatMapTo(mutableSetOf()) { imageUrls ->
                imageUrls.findImageUrisToDownload()
            }
}
