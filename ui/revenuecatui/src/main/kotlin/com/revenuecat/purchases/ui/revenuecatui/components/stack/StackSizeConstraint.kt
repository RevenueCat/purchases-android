@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.foundation.gestures.Orientation
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.ui.revenuecatui.components.LocalizedTextPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedCarouselPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedIconPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedImagePartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedOverride
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedPackagePartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedStackPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedTabsPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedTimelineItemPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedTimelinePartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedVideoPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedWebViewPartial
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
import com.revenuecat.purchases.ui.revenuecatui.components.style.WebViewComponentStyle

internal val SizeConstraint.hasPositiveFitMinimum: Boolean
    get() = this is Fit && (min ?: 0u) > 0u

internal val SizeConstraint.allowsFlexDistribution: Boolean
    get() = this !is Fit || hasPositiveFitMinimum

/**
 * A Fit stack with a positive minimum hugs its content (Fill children sit at their minimums). Without a minimum, a
 * Fit stack with Fill children takes all the space it is offered, matching Row/Column's `weight` behavior.
 */
internal fun SizeConstraint.shouldFitMainAxis(hasFillChildren: Boolean): Boolean =
    this is Fit && (!hasFillChildren || hasPositiveFitMinimum)

internal fun Size.mainAxis(orientation: Orientation): SizeConstraint =
    if (orientation == Orientation.Horizontal) width else height

/**
 * Whether this stack needs [ConstrainedFillLayout] instead of a plain Row/Column. This is the only entry point into
 * the min/max layout code: stacks without any min/max on the main axis (i.e. every pre-existing paywall) keep using
 * Row/Column unchanged.
 *
 * Row/Column can't do two things:
 * - Redistribute leftover space when a `weight`ed child is capped by a maximum or floored by a minimum.
 * - Hug a Fit stack that has a positive minimum: any `weight` (Fill child or SPACE_* spacer) expands it to the
 *   parent's maximum instead.
 */
internal fun needsConstrainedFillLayout(
    stackSize: Size,
    distribution: FlexDistribution,
    children: List<ComponentStyle>,
    orientation: Orientation,
): Boolean {
    val mainAxis = stackSize.mainAxis(orientation)
    val hasFillChild = children.any { it.size.mainAxis(orientation) is Fill }
    val hasLimitedFillChild = children.any { child ->
        child.candidateSizes.any { it.mainAxis(orientation).isLimitedFill }
    }
    return hasLimitedFillChild ||
        (mainAxis.hasPositiveFitMinimum && (hasFillChild || distribution.usesAllAvailableSpace))
}

private val SizeConstraint.isLimitedFill: Boolean
    get() = this is Fill && (min != null || max != null)

/**
 * Every size this component may resolve to: its base size plus the size of every override, since overrides are only
 * resolved by the component itself at composition time.
 */
private val ComponentStyle.candidateSizes: Sequence<Size>
    get() = sequenceOf(size) + overrideSizes()

// A flat, exhaustive mapping over the sealed ComponentStyle hierarchy.
@Suppress("CyclomaticComplexMethod")
private fun ComponentStyle.overrideSizes(): Sequence<Size> = when (this) {
    is StackComponentStyle -> overrides.sizes()
    is TextComponentStyle -> overrides.sizes()
    is ImageComponentStyle -> overrides.sizes()
    is IconComponentStyle -> overrides.sizes()
    is VideoComponentStyle -> overrides.sizes()
    is CarouselComponentStyle -> overrides.sizes()
    is TabsComponentStyle -> overrides.sizes()
    is TimelineComponentStyle -> overrides.sizes()
    is WebViewComponentStyle -> overrides.sizes()
    is ButtonComponentStyle -> stackComponentStyle.overrideSizes()
    is PackageComponentStyle -> stackComponentStyle.overrideSizes()
    is StickyFooterComponentStyle -> stackComponentStyle.overrideSizes()
    is HeaderComponentStyle -> stackComponentStyle.overrideSizes()
    is CountdownComponentStyle -> countdownStackComponentStyle.overrideSizes()
    is TabControlButtonComponentStyle -> stack.overrideSizes()
    is TabControlStyle.Buttons -> stack.overrideSizes()
    is TabControlStyle.Toggle -> stack.overrideSizes()
    is TabControlToggleComponentStyle -> emptySequence()
}

private fun List<PresentedOverride<*>>.sizes(): Sequence<Size> =
    asSequence().mapNotNull { it.properties.size }

private val PresentedPartial<*>.size: Size?
    get() = when (this) {
        is PresentedStackPartial -> partial.size
        is LocalizedTextPartial -> partial.size
        is PresentedImagePartial -> partial.size
        is PresentedIconPartial -> partial.size
        is PresentedVideoPartial -> partial.size
        is PresentedCarouselPartial -> partial.size
        is PresentedTabsPartial -> partial.size
        is PresentedTimelinePartial -> partial.size
        is PresentedWebViewPartial,
        is PresentedTimelineItemPartial,
        is PresentedPackagePartial,
        -> null
    }
