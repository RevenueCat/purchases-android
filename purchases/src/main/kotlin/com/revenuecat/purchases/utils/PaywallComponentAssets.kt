@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.utils

import android.net.Uri
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.CarouselComponent
import com.revenuecat.purchases.paywalls.components.CountdownComponent
import com.revenuecat.purchases.paywalls.components.FallbackHeaderComponent
import com.revenuecat.purchases.paywalls.components.HeaderComponent
import com.revenuecat.purchases.paywalls.components.IconComponent
import com.revenuecat.purchases.paywalls.components.ImageComponent
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.PartialComponent
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
import com.revenuecat.purchases.paywalls.components.WebViewComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls

internal data class PaywallComponentAssets(
    val imageUris: Set<Uri>,
    val webViews: Set<WebViewAsset>,
)

internal data class WebViewAsset(
    val url: String,
    val componentId: String,
    val sizeToContentWidth: Boolean,
    val sizeToContentHeight: Boolean,
)

internal fun PaywallComponentsConfig.collectAssets(): PaywallComponentAssets {
    val imageUris = background.findImageUris().toMutableSet()
    val webViews = mutableSetOf<WebViewAsset>()

    listOfNotNull(stack, header?.stack, stickyFooter?.stack).forEach { tree ->
        tree.flatten().forEach { component ->
            imageUris += component.findImageUris()
            if (component is WebViewComponent) webViews += component.toWebViewAsset()
        }
    }

    return PaywallComponentAssets(imageUris = imageUris, webViews = webViews)
}

private fun WebViewComponent.toWebViewAsset(): WebViewAsset = WebViewAsset(
    url = url,
    componentId = id,
    // The schema lets an override change only `visible`, so size is always the base component's.
    sizeToContentWidth = size.width is SizeConstraint.Fit,
    sizeToContentHeight = size.height is SizeConstraint.Fit,
)

/** Image URIs this component references directly; descendants are visited separately by [flatten]. */
@Suppress("CyclomaticComplexMethod")
private fun PaywallComponent.findImageUris(): Set<Uri> =
    when (this) {
        is StackComponent -> {
            background.findImageUris() + overrides.imageUris { it.background.findImageUris() }
        }
        is IconComponent -> {
            // Not path() (replaces the base path) nor appendPath (percent-encodes): the result must match
            // IconComponentState's raw concatenation, since the image cache keys on the exact URL string.
            setOf(
                Uri.parse(baseUrl)
                    .buildUpon()
                    .appendEncodedPath(formats.webp)
                    .build(),
            )
        }
        is CarouselComponent -> {
            background.findImageUris() + overrides.imageUris { it.background.findImageUris() }
        }
        is TabsComponent -> {
            background.findImageUris() + overrides.imageUris { it.background.findImageUris() }
        }
        is ImageComponent -> {
            source.findImageUris() + overrides.imageUris { it.source?.findImageUris().orEmpty() }
        }
        is VideoComponent -> {
            fallbackSource?.findImageUris().orEmpty() +
                overrides.imageUris { it.fallbackSource?.findImageUris().orEmpty() }
        }
        is ButtonComponent,
        is CountdownComponent,
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

private fun <T : PartialComponent> List<ComponentOverride<T>>?.imageUris(
    extract: (T) -> Set<Uri>,
): Set<Uri> = this?.flatMapTo(mutableSetOf()) { extract(it.properties) } ?: emptySet()

private fun Background?.findImageUris(): Set<Uri> {
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

private fun ThemeImageUrls.findImageUris(): Set<Uri> {
    return setOfNotNull(
        light.webpLowRes.toString().let { Uri.parse(it) },
        dark?.webpLowRes?.toString()?.let { Uri.parse(it) },
    )
}
