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
import com.revenuecat.purchases.paywalls.components.PartialComponent
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
import com.revenuecat.purchases.paywalls.components.WebViewComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import java.net.URL

internal class PaywallComponentsImagePreDownloader(
    /**
     * We check for the existence of the paywalls SDK. If so, the Coil SDK should be available to
     * pre-download the images.
     */
    private val shouldPredownloadImages: Boolean = canUsePaywallUI,
    private val coilImageDownloader: CoilImageDownloader,
    private val webViewPreDownloader: WebViewPreDownloader = NoOpWebViewPreDownloader,
) {

    fun preDownloadImages(paywallComponentsConfig: PaywallComponentsConfig) {
        if (!shouldPredownloadImages) {
            verboseLog { "PaywallComponentsImagePreDownloader won't pre-download images" }
            return
        }

        val imageUrls = findImageUrisToDownload(paywallComponentsConfig)
        imageUrls.forEach {
            debugLog { "Pre-downloading Paywall V2 image: $it" }
            coilImageDownloader.downloadImage(it)
        }

        val webViewUrls = findWebViewUrlsToDownload(paywallComponentsConfig)
        webViewUrls.forEach {
            debugLog { "Pre-downloading Paywall V2 web view: $it" }
            webViewPreDownloader.preDownloadWebView(it)
        }
    }

    private fun findImageUrisToDownload(paywallComponentsConfig: PaywallComponentsConfig): Set<Uri> {
        return paywallComponentsConfig.stack.findImageUrisToDownload() +
            (paywallComponentsConfig.header?.stack?.findImageUrisToDownload().orEmpty()) +
            (paywallComponentsConfig.stickyFooter?.stack?.findImageUrisToDownload().orEmpty()) +
            paywallComponentsConfig.background.findImageUrisToDownload()
    }

    @Suppress("CyclomaticComplexMethod")
    private fun StackComponent.findImageUrisToDownload(): Set<Uri> {
        // PaywallComponent.filter is a BFS over the whole component tree. Passing { true }
        // visits every descendant; findImageUrisToDownload is then responsible for extracting
        // direct URIs per type (with the when expression as the single source of truth).
        return filter { true }
            .flatMapTo(mutableSetOf()) { component ->
                when (component) {
                    is StackComponent -> {
                        component.background.findImageUrisToDownload() +
                            component.overrides.imageUrisToDownload { it.background.findImageUrisToDownload() }
                    }
                    is IconComponent -> {
                        setOf(Uri.parse(component.baseUrl).buildUpon().path(component.formats.webp).build())
                    }
                    is CarouselComponent -> {
                        // pages are visited by BFS; only extract this component's own background
                        component.background.findImageUrisToDownload() +
                            component.overrides.imageUrisToDownload { it.background.findImageUrisToDownload() }
                    }
                    is TabsComponent -> {
                        component.background.findImageUrisToDownload() +
                            component.overrides.imageUrisToDownload { it.background.findImageUrisToDownload() }
                    }
                    is ImageComponent -> {
                        component.source.findImageUrisToDownload() +
                            component.overrides.imageUrisToDownload { it.source?.findImageUrisToDownload().orEmpty() }
                    }
                    is VideoComponent -> {
                        component.fallbackSource?.findImageUrisToDownload().orEmpty() +
                            component.overrides.imageUrisToDownload {
                                it.fallbackSource?.findImageUrisToDownload().orEmpty()
                            }
                    }
                    is ButtonComponent,
                    is CountdownComponent, // sub-stacks visited by BFS
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
                    is WebViewComponent,
                    -> emptySet()
                }
            }
    }

    private fun findWebViewUrlsToDownload(paywallComponentsConfig: PaywallComponentsConfig): Set<URL> {
        return paywallComponentsConfig.stack.findWebViewUrlsToDownload() +
            (paywallComponentsConfig.header?.stack?.findWebViewUrlsToDownload().orEmpty()) +
            (paywallComponentsConfig.stickyFooter?.stack?.findWebViewUrlsToDownload().orEmpty())
    }

    @Suppress("CyclomaticComplexMethod")
    private fun StackComponent.findWebViewUrlsToDownload(): Set<URL> {
        return filter { true }
            .flatMapTo(mutableSetOf()) { component ->
                when (component) {
                    is WebViewComponent -> component.url.toStaticWebViewUrlOrNull()?.let(::setOf).orEmpty()
                    is ButtonComponent,
                    is CarouselComponent,
                    is CountdownComponent,
                    is FallbackHeaderComponent,
                    is HeaderComponent,
                    is IconComponent,
                    is ImageComponent,
                    is PackageComponent,
                    is PurchaseButtonComponent,
                    is StackComponent,
                    is StickyFooterComponent,
                    is TabControlButtonComponent,
                    is TabControlComponent,
                    is TabControlToggleComponent,
                    is TabsComponent,
                    is TextComponent,
                    is TimelineComponent,
                    is VideoComponent,
                    -> emptySet()
                }
            }
    }

    private fun String.toStaticWebViewUrlOrNull(): URL? {
        if (contains(TEMPLATE_VARIABLE_START)) return null

        return runCatching { URL(this) }
            .getOrNull()
            ?.takeIf { it.protocol == HTTPS_SCHEME && it.host.isNotBlank() }
    }

    private fun <T : PartialComponent> List<ComponentOverride<T>>?.imageUrisToDownload(
        extract: (T) -> Set<Uri>,
    ): Set<Uri> = this?.flatMapTo(mutableSetOf()) { extract(it.properties) } ?: emptySet()

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

    private companion object {
        private const val HTTPS_SCHEME = "https"
        private const val TEMPLATE_VARIABLE_START = "{{"
    }
}
