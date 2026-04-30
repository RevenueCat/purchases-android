@file:JvmSynthetic
@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.style.ButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.CarouselComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.CountdownComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.HeaderComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.IconComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.ImageComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.PackageComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StickyFooterComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabControlButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabControlStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabControlToggleComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabsComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TimelineComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.VideoComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState

/**
 * Walks every image-bearing node in the components tree (background, stack, header, sticky footer,
 * plus buttons/carousels/tabs/timelines/etc.) and yields each image URL — light, dark, and
 * low-resolution variants — so the caller can enqueue them into Coil's image cache up front.
 */
@JvmSynthetic
internal fun PaywallState.Loaded.Components.preloadImageUrls(): Sequence<String> = sequence {
    yieldAll(background.preloadImageUrls())
    yieldAll(stack.preloadImageUrls())
    header?.let { yieldAll(it.preloadImageUrls()) }
    stickyFooter?.let { yieldAll(it.preloadImageUrls()) }
}

@Suppress("CyclomaticComplexMethod")
private fun ComponentStyle.preloadImageUrls(): Sequence<String> = when (this) {
    is ButtonComponentStyle -> preloadButtonImageUrls()
    is CarouselComponentStyle -> preloadCarouselImageUrls()
    is CountdownComponentStyle -> sequence {
        yieldAll(countdownStackComponentStyle.preloadImageUrls())
        endStackComponentStyle?.let { yieldAll(it.preloadImageUrls()) }
        fallbackStackComponentStyle?.let { yieldAll(it.preloadImageUrls()) }
    }
    is HeaderComponentStyle -> stackComponentStyle.preloadImageUrls()
    is IconComponentStyle -> sequenceOf("$baseUrl/${formats.webp}")
    is ImageComponentStyle -> sources.values.asSequence().flatMap { it.preloadImageUrls() }
    is PackageComponentStyle -> stackComponentStyle.preloadImageUrls()
    is StackComponentStyle -> preloadStackImageUrls()
    is StickyFooterComponentStyle -> stackComponentStyle.preloadImageUrls()
    is TabControlButtonComponentStyle -> stack.preloadImageUrls()
    is TabControlToggleComponentStyle -> emptySequence()
    is TabControlStyle.Buttons -> stack.preloadImageUrls()
    is TabControlStyle.Toggle -> stack.preloadImageUrls()
    is TabsComponentStyle -> preloadTabsImageUrls()
    is TextComponentStyle -> emptySequence()
    is TimelineComponentStyle -> preloadTimelineImageUrls()
    is VideoComponentStyle -> {
        fallbackSources
            ?.values
            ?.asSequence()
            ?.flatMap { it.preloadImageUrls() }
            .orEmpty()
    }
}

private fun ButtonComponentStyle.preloadButtonImageUrls(): Sequence<String> = sequence {
    yieldAll(stackComponentStyle.preloadImageUrls())
    val sheet = (action as? ButtonComponentStyle.Action.NavigateTo)
        ?.destination as? ButtonComponentStyle.Action.NavigateTo.Destination.Sheet
    sheet?.let { yieldAll(it.stack.preloadImageUrls()) }
}

private fun CarouselComponentStyle.preloadCarouselImageUrls(): Sequence<String> = sequence {
    yieldAll(background.preloadImageUrls())
    pages.forEach { yieldAll(it.preloadImageUrls()) }
}

private fun StackComponentStyle.preloadStackImageUrls(): Sequence<String> = sequence {
    yieldAll(background.preloadImageUrls())
    badge?.let { yieldAll(it.stackStyle.preloadImageUrls()) }
    children.forEach { yieldAll(it.preloadImageUrls()) }
}

private fun TabsComponentStyle.preloadTabsImageUrls(): Sequence<String> = sequence {
    yieldAll(background.preloadImageUrls())
    yieldAll(control.preloadImageUrls())
    tabs.forEach { yieldAll(it.stack.preloadImageUrls()) }
}

private fun TimelineComponentStyle.preloadTimelineImageUrls(): Sequence<String> =
    items.asSequence().flatMap { item ->
        sequence {
            yieldAll(item.title.preloadImageUrls())
            item.description?.let { yieldAll(it.preloadImageUrls()) }
            yieldAll(item.icon.preloadImageUrls())
        }
    }

private fun BackgroundStyles?.preloadImageUrls(): Sequence<String> = when (this) {
    is BackgroundStyles.Image -> sources.preloadImageUrls()
    is BackgroundStyles.Video -> fallbackImage.preloadImageUrls()
    is BackgroundStyles.Color,
    null,
    -> emptySequence()
}

private fun ThemeImageUrls.preloadImageUrls(): Sequence<String> = sequence {
    yield(light.webpLowRes.toString())
    yield(light.webp.toString())
    dark?.let {
        yield(it.webpLowRes.toString())
        yield(it.webp.toString())
    }
}
