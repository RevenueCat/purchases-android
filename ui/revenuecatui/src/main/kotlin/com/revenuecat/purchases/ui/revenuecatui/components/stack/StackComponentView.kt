@file:JvmSynthetic
@file:Suppress("TooManyFunctions")

package com.revenuecat.purchases.ui.revenuecatui.components.stack

import android.content.res.Configuration
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import com.revenuecat.purchases.paywalls.components.CountdownComponent
import com.revenuecat.purchases.paywalls.components.properties.Badge
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.CornerRadiuses
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.WithOptionalBackgroundOverlay
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toHorizontalAlignmentOrNull
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toShape
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toVerticalAlignmentOrNull
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.background
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.border
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.scrollable
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.shadow
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.previewEmptyState
import com.revenuecat.purchases.ui.revenuecatui.components.previewStackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.previewTextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ShadowStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberBackgroundStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberBorderStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberShadowStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.BadgeStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.ImageComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.VideoComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.applyIfNotNull
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallComponentInteractionTracker
import kotlin.math.roundToInt
import androidx.compose.ui.geometry.Size as ComposeSize

/**
 * Renders a [StackComponentStyle].
 *
 * @param clickHandler Action dispatcher invoked by descendants when they fire a [PaywallAction]
 *   (e.g. a child Button asking the screen to navigate back). This is *outbound*: the stack itself
 *   does not call this; it just propagates from descendants up to the screen-level handler.
 *   Distinct from [onStackClick].
 * @param onStackClick Click callback when this entire stack is tapped. `null` makes the stack
 *   non-clickable (no ripple, no gesture detection). When non-null, the stack draws a
 *   shape-clipped Material ripple via a sibling layout so the ripple follows the stack's rounded
 *   corners without clipping nested children that extend outside the parent's bounds (e.g. badges
 *   with offsets, shadows). Distinct from [clickHandler].
 * @param enabled When `false`, the stack stays semantically a button (kept clickable for
 *   accessibility) but ignores clicks and shows no ripple. Only meaningful when [onStackClick] is
 *   non-null. Use for transient disabled states such as "purchase in progress" or
 *   "already-selected package".
 */
@Suppress("LongMethod", "LongParameterList")
@Composable
internal fun StackComponentView(
    style: StackComponentStyle,
    state: PaywallState.Loaded.Components,
    clickHandler: suspend (PaywallAction) -> Unit,
    modifier: Modifier = Modifier,
    onStackClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    contentAlpha: Float = 1f,
    componentInteractionTracker: PaywallComponentInteractionTracker = PaywallComponentInteractionTracker { _ -> },
) {
    // Get a StackComponentState that calculates the overridden properties we should use.
    val stackState = rememberUpdatedStackComponentState(
        style = style,
        paywallState = state,
    )

    if (!stackState.visible) {
        return
    }

    val badge = stackState.badge
    if (badge != null) {
        when (badge.style) {
            Badge.Style.Overlay -> {
                StackWithOverlaidBadge(
                    stackState,
                    state,
                    badge.stackStyle,
                    badge.alignment,
                    clickHandler,
                    componentInteractionTracker,
                    contentAlpha,
                    modifier,
                    onStackClick = onStackClick,
                    enabled = enabled,
                    interactionSource = interactionSource,
                )
            }

            Badge.Style.EdgeToEdge -> {
                when (badge.alignment) {
                    TwoDimensionalAlignment.TOP,
                    TwoDimensionalAlignment.BOTTOM,
                    -> StackWithLongEdgeToEdgeBadge(
                        stackState,
                        state,
                        badge.stackStyle,
                        badge.alignment.isTop,
                        clickHandler,
                        componentInteractionTracker,
                        contentAlpha,
                        modifier,
                        onStackClick = onStackClick,
                        enabled = enabled,
                        interactionSource = interactionSource,
                    )

                    else
                    -> StackWithShortEdgeToEdgeBadge(
                        stackState,
                        state,
                        badge.stackStyle,
                        badge.alignment,
                        clickHandler,
                        componentInteractionTracker,
                        contentAlpha,
                        modifier,
                        onStackClick = onStackClick,
                        enabled = enabled,
                        interactionSource = interactionSource,
                    )
                }
            }

            Badge.Style.Nested ->
                MainStackComponent(
                    stackState = stackState,
                    state = state,
                    clickHandler = clickHandler,
                    componentInteractionTracker = componentInteractionTracker,
                    contentAlpha = contentAlpha,
                    modifier = modifier,
                    onStackClick = onStackClick,
                    enabled = enabled,
                    interactionSource = interactionSource,
                    nestedBadge = badge,
                )
        }
    } else {
        MainStackComponent(
            stackState = stackState,
            state = state,
            clickHandler = clickHandler,
            componentInteractionTracker = componentInteractionTracker,
            contentAlpha = contentAlpha,
            modifier = modifier,
            onStackClick = onStackClick,
            enabled = enabled,
            interactionSource = interactionSource,
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun StackWithOverlaidBadge(
    stackState: StackComponentState,
    state: PaywallState.Loaded.Components,
    badgeStack: StackComponentStyle,
    alignment: TwoDimensionalAlignment,
    clickHandler: suspend (PaywallAction) -> Unit,
    componentInteractionTracker: PaywallComponentInteractionTracker,
    contentAlpha: Float,
    modifier: Modifier = Modifier,
    onStackClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    Box(modifier = modifier) {
        MainStackComponent(
            stackState = stackState,
            state = state,
            clickHandler = clickHandler,
            componentInteractionTracker = componentInteractionTracker,
            contentAlpha = contentAlpha,
            onStackClick = onStackClick,
            enabled = enabled,
            interactionSource = interactionSource,
        )
        val mainStackBorderWidthPx = with(LocalDensity.current) {
            stackState.border?.width?.toPx()
        }
        OverlaidBadge(
            badgeStack,
            state,
            componentInteractionTracker,
            alignment,
            mainStackBorderWidthPx,
            modifier = Modifier.padding(stackState.margin),
        )
    }
}

/**
 * @param topBadge Whether the badge should be placed on the top or bottom of the stack.
 */
@Suppress("LongMethod", "LongParameterList", "CyclomaticComplexMethod")
@Composable
private fun StackWithLongEdgeToEdgeBadge(
    stackState: StackComponentState,
    state: PaywallState.Loaded.Components,
    badgeStack: StackComponentStyle,
    topBadge: Boolean,
    clickHandler: suspend (PaywallAction) -> Unit,
    componentInteractionTracker: PaywallComponentInteractionTracker,
    contentAlpha: Float,
    modifier: Modifier = Modifier,
    onStackClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    val shadowStyle = stackState.shadow?.let { rememberShadowStyle(shadow = it) }
    val composeShape by remember(stackState.shape) { derivedStateOf { stackState.shape.toShape() } }

    SubcomposeLayout(
        modifier = modifier
            .applyIfNotNull(shadowStyle) { shadow(it, composeShape) },
    ) { constraints ->
        // Subcompose and measure the stack
        val stackMeasurable = subcompose("stack") {
            MainStackComponent(
                stackState,
                state,
                clickHandler,
                componentInteractionTracker,
                contentAlpha,
                shouldApplyShadow = false,
                onStackClick = onStackClick,
                enabled = enabled,
                interactionSource = interactionSource,
            )
        }.first()
        val stackPlaceable = stackMeasurable.measure(constraints)

        // Subcompose and measure the badge
        val badgeMeasurable = subcompose("badge") {
            StackComponentView(
                // The background and border is applied to the parent container so we null it out here.
                // We make the badge use all the available width without increasing the size of the main content.
                badgeStack.copy(
                    background = null,
                    size = Size(width = Fill, height = badgeStack.size.height),
                    border = null,
                    margin = PaddingValues(0.dp),
                ),
                state,
                clickHandler,
                componentInteractionTracker = componentInteractionTracker,
            )
        }.first()
        val badgePlaceable = badgeMeasurable.measure(constraints)
        val badgeHeight = badgePlaceable.height

        // Decide the final size of this layout
        val totalWidth = stackPlaceable.width
        val totalHeight = stackPlaceable.height + badgeHeight

        // Subcompose the background
        val backgroundMeasurable = subcompose("background") {
            val backgroundStyle = badgeStack.background?.let { rememberBackgroundStyle(background = it) }
            val borderStyle = badgeStack.border?.let { rememberBorderStyle(border = it) }
            val shadowStyle = badgeStack.shadow?.let { rememberShadowStyle(shadow = it) }
            val backgroundShape = when (val badgeCornerRadiuses = badgeStack.shape.cornerRadiuses) {
                is CornerRadiuses.Percentage -> {
                    (badgeStack.shape.toShape() as? RoundedCornerShape)?.let { composeShape ->
                        RoundedCornerShape(
                            topStart = composeShape.topStart.makeAbsolute(stackPlaceable, LocalDensity.current),
                            topEnd = composeShape.topEnd.makeAbsolute(stackPlaceable, LocalDensity.current),
                            bottomEnd = composeShape.bottomEnd.makeAbsolute(stackPlaceable, LocalDensity.current),
                            bottomStart = composeShape.bottomStart.makeAbsolute(stackPlaceable, LocalDensity.current),
                        )
                    } ?: RectangleShape
                }

                is CornerRadiuses.Dp -> {
                    when (val mainStackCornerRadiuses = stackState.shape.cornerRadiuses) {
                        is CornerRadiuses.Dp -> if (topBadge) {
                            Shape.Rectangle(
                                corners = CornerRadiuses.Dp(
                                    topLeading = badgeCornerRadiuses.topLeading,
                                    topTrailing = badgeCornerRadiuses.topTrailing,
                                    bottomLeading = mainStackCornerRadiuses.bottomLeading,
                                    bottomTrailing = mainStackCornerRadiuses.bottomTrailing,
                                ),
                            ).toShape()
                        } else {
                            Shape.Rectangle(
                                corners = CornerRadiuses.Dp(
                                    topLeading = mainStackCornerRadiuses.topLeading,
                                    topTrailing = mainStackCornerRadiuses.topTrailing,
                                    bottomLeading = badgeCornerRadiuses.bottomLeading,
                                    bottomTrailing = badgeCornerRadiuses.bottomTrailing,
                                ),
                            ).toShape()
                        }

                        is CornerRadiuses.Percentage ->
                            (stackState.shape.toShape() as? RoundedCornerShape)?.let { composeShape ->
                                if (topBadge) {
                                    RoundedCornerShape(
                                        topStart = CornerSize(badgeCornerRadiuses.topLeading.dp),
                                        topEnd = CornerSize(badgeCornerRadiuses.topTrailing.dp),
                                        bottomEnd = composeShape.bottomEnd.makeAbsolute(
                                            stackPlaceable,
                                            LocalDensity.current,
                                        ),
                                        bottomStart = composeShape.bottomStart.makeAbsolute(
                                            stackPlaceable,
                                            LocalDensity.current,
                                        ),
                                    )
                                } else {
                                    RoundedCornerShape(
                                        topStart = composeShape.topStart.makeAbsolute(
                                            stackPlaceable,
                                            LocalDensity.current,
                                        ),
                                        topEnd = composeShape.topEnd.makeAbsolute(stackPlaceable, LocalDensity.current),
                                        bottomEnd = CornerSize(badgeCornerRadiuses.bottomTrailing.dp),
                                        bottomStart = CornerSize(badgeCornerRadiuses.bottomLeading.dp),
                                    )
                                }
                            } ?: RectangleShape
                    }
                }
            }

            val backgroundModifier = remember(badgeStack, backgroundStyle, borderStyle, shadowStyle) {
                Modifier
                    .padding(badgeStack.margin)
                    .applyIfNotNull(backgroundStyle) { background(it, backgroundShape) }
                    .applyIfNotNull(backgroundShape) { clip(it) }
                    .applyIfNotNull(borderStyle) { border(it, badgeStack.shape.toShape()) }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(backgroundModifier),
            )
        }.first()
        val backgroundPlaceable = backgroundMeasurable.measure(
            // We know exactly how big this needs to be.
            Constraints.fixed(width = totalWidth, height = totalHeight),
        )

        // Lay it all out
        layout(totalWidth, totalHeight) {
            backgroundPlaceable.placeRelative(0, 0)

            var yPosition = 0

            if (topBadge) {
                badgePlaceable.placeRelative(x = 0, y = yPosition)
                yPosition += badgePlaceable.height

                stackPlaceable.placeRelative(x = 0, y = yPosition)
                yPosition += stackPlaceable.height
            } else {
                stackPlaceable.placeRelative(x = 0, y = yPosition)
                yPosition += stackPlaceable.height

                badgePlaceable.placeRelative(x = 0, y = yPosition)
                yPosition += badgePlaceable.height
            }
        }
    }
}

@Suppress("LongParameterList", "LongMethod")
@Composable
private fun StackWithShortEdgeToEdgeBadge(
    stackState: StackComponentState,
    state: PaywallState.Loaded.Components,
    badgeStack: StackComponentStyle,
    alignment: TwoDimensionalAlignment,
    clickHandler: suspend (PaywallAction) -> Unit,
    componentInteractionTracker: PaywallComponentInteractionTracker,
    contentAlpha: Float,
    modifier: Modifier = Modifier,
    onStackClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    val adjustedCornerRadiuses: CornerRadiuses = when (val badgeRectangleCorners = badgeStack.shape.cornerRadiuses) {
        is CornerRadiuses.Percentage -> {
            when (alignment) {
                TwoDimensionalAlignment.TOP_LEADING -> CornerRadiuses.Percentage(
                    topLeading = 0,
                    topTrailing = 0,
                    bottomLeading = 0,
                    bottomTrailing = badgeRectangleCorners.bottomTrailing,
                )

                TwoDimensionalAlignment.TOP_TRAILING -> CornerRadiuses.Percentage(
                    topLeading = 0,
                    topTrailing = 0,
                    bottomLeading = badgeRectangleCorners.bottomLeading,
                    bottomTrailing = 0,
                )

                TwoDimensionalAlignment.BOTTOM_LEADING -> CornerRadiuses.Percentage(
                    topLeading = 0,
                    topTrailing = badgeRectangleCorners.topTrailing,
                    bottomLeading = 0,
                    bottomTrailing = 0,
                )

                TwoDimensionalAlignment.BOTTOM_TRAILING -> CornerRadiuses.Percentage(
                    topLeading = badgeRectangleCorners.topLeading,
                    topTrailing = 0,
                    bottomLeading = 0,
                    bottomTrailing = 0,
                )

                else -> CornerRadiuses.Percentage(all = 0)
            }
        }

        is CornerRadiuses.Dp -> {
            when (alignment) {
                TwoDimensionalAlignment.TOP_LEADING -> CornerRadiuses.Dp(
                    topLeading = 0.0,
                    topTrailing = 0.0,
                    bottomLeading = 0.0,
                    bottomTrailing = badgeRectangleCorners.bottomTrailing,
                )

                TwoDimensionalAlignment.TOP_TRAILING -> CornerRadiuses.Dp(
                    topLeading = 0.0,
                    topTrailing = 0.0,
                    bottomLeading = badgeRectangleCorners.bottomLeading,
                    bottomTrailing = 0.0,
                )

                TwoDimensionalAlignment.BOTTOM_LEADING -> CornerRadiuses.Dp(
                    topLeading = 0.0,
                    topTrailing = badgeRectangleCorners.topTrailing,
                    bottomLeading = 0.0,
                    bottomTrailing = 0.0,
                )

                TwoDimensionalAlignment.BOTTOM_TRAILING -> CornerRadiuses.Dp(
                    topLeading = badgeRectangleCorners.topLeading,
                    topTrailing = 0.0,
                    bottomLeading = 0.0,
                    bottomTrailing = 0.0,
                )

                else -> CornerRadiuses.Dp(all = 0.0)
            }
        }
    }
    MainStackComponent(
        stackState = stackState,
        state = state,
        clickHandler = clickHandler,
        componentInteractionTracker = componentInteractionTracker,
        contentAlpha = contentAlpha,
        modifier = modifier,
        onStackClick = onStackClick,
        enabled = enabled,
        interactionSource = interactionSource,
    ) {
        StackComponentView(
            badgeStack.copy(shape = Shape.Rectangle(adjustedCornerRadiuses)),
            state,
            clickHandler,
            componentInteractionTracker = componentInteractionTracker,
            modifier = Modifier.align(alignment.toAlignment()),
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun BoxScope.OverlaidBadge(
    badgeStack: StackComponentStyle,
    state: PaywallState.Loaded.Components,
    componentInteractionTracker: PaywallComponentInteractionTracker,
    alignment: TwoDimensionalAlignment,
    mainStackBorderWidthPx: Float?,
    modifier: Modifier = Modifier,
) {
    StackComponentView(
        badgeStack,
        state,
        {},
        componentInteractionTracker = componentInteractionTracker,
        modifier = modifier
            .align(alignment.toAlignment())
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.placeRelative(
                        x = 0,
                        y = getOverlaidBadgeOffsetY(placeable.height, alignment, mainStackBorderWidthPx ?: 0f),
                    )
                }
            },
    )
}

@Suppress("LongMethod", "LongParameterList", "CyclomaticComplexMethod")
@Composable
private fun MainStackComponent(
    stackState: StackComponentState,
    state: PaywallState.Loaded.Components,
    clickHandler: suspend (PaywallAction) -> Unit,
    componentInteractionTracker: PaywallComponentInteractionTracker,
    contentAlpha: Float,
    modifier: Modifier = Modifier,
    onStackClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    nestedBadge: BadgeStyle? = null,
    shouldApplyShadow: Boolean = true,
    overlay: (@Composable BoxScope.() -> Unit)? = null,
) {
    val safeDrawingInsets = WindowInsets.safeDrawing

    // Show the right container composable depending on the dimension.
    val stack: @Composable (Modifier) -> Unit = { rootModifier ->
        val scrollState = stackState.scrollOrientation?.let { rememberScrollState() }

        // Columns and Rows don't draw anything if they don't have any children. A Box does. We want users to be able
        // to draw "boxes" using whatever stack they please, for instance to create dividers.
        if (stackState.children.isEmpty()) {
            Box(
                modifier = modifier
                    .size(stackState.size)
                    .then(rootModifier),
            )
        } else {
            when (val dimension = stackState.dimension) {
                is Dimension.Horizontal -> HorizontalStack(
                    size = stackState.size,
                    dimension = dimension,
                    spacing = stackState.spacing,
                    modifier = modifier
                        .size(stackState.size, verticalAlignment = dimension.alignment.toAlignment())
                        .applyIfNotNull(scrollState, stackState.scrollOrientation) { state, orientation ->
                            scrollable(state, orientation)
                        }
                        .then(rootModifier),
                ) {
                    items(stackState.children) { _, child ->
                        ComponentView(
                            style = child,
                            state = state,
                            onClick = clickHandler,
                            componentInteractionTracker = componentInteractionTracker,
                            modifier = Modifier
                                .conditional(child.size.width == Fill) { Modifier.weight(1f) }
                                .conditional(stackState.applyTopWindowInsets && !child.shouldIgnoreTopWindowInsets) {
                                    windowInsetsPadding(safeDrawingInsets.only(WindowInsetsSides.Top))
                                }
                                .alpha(contentAlpha),
                        )
                    }
                }

                is Dimension.Vertical -> VerticalStack(
                    size = stackState.size,
                    dimension = dimension,
                    spacing = stackState.spacing,
                    modifier = modifier
                        .size(stackState.size, horizontalAlignment = dimension.alignment.toAlignment())
                        .applyIfNotNull(scrollState, stackState.scrollOrientation) { state, orientation ->
                            scrollable(state, orientation)
                        }
                        .then(rootModifier),
                ) {
                    items(stackState.children) { index, child ->
                        ComponentView(
                            style = child,
                            state = state,
                            onClick = clickHandler,
                            componentInteractionTracker = componentInteractionTracker,
                            modifier = Modifier
                                .conditional(child.size.height == Fill) { Modifier.weight(1f) }
                                .conditional(
                                    // In a Vertical container, we only want to apply topSystemBarsPadding to the first
                                    // child, except when that child has `ignoreTopWindowInsets` set to true.
                                    stackState.applyTopWindowInsets &&
                                        index == 0 &&
                                        !child.shouldIgnoreTopWindowInsets,
                                ) {
                                    windowInsetsPadding(safeDrawingInsets.only(WindowInsetsSides.Top))
                                }
                                .alpha(contentAlpha),
                        )
                    }
                }

                is Dimension.ZLayer -> {
                    // Pre-compute the top safe-drawing inset in px for use as a fallback
                    // when no header height is available. Captured at composition time so
                    // the Modifier.layout closure can branch at layout time.
                    val topInsetPx = if (stackState.applyTopWindowInsets && !stackState.ignoreHeaderHeight) {
                        safeDrawingInsets.getTop(LocalDensity.current)
                    } else {
                        0
                    }
                    Box(
                        modifier = modifier
                            .size(
                                size = stackState.size,
                                horizontalAlignment = dimension.alignment.toHorizontalAlignmentOrNull(),
                                verticalAlignment = dimension.alignment.toVerticalAlignmentOrNull(),
                            )
                            .applyIfNotNull(scrollState, stackState.scrollOrientation) { state, orientation ->
                                scrollable(state, orientation)
                            }
                            .then(rootModifier),
                        contentAlignment = dimension.alignment.toAlignment(),
                    ) {
                        stackState.children.forEach { child ->
                            val applyTopInsets =
                                stackState.applyTopWindowInsets && !child.shouldIgnoreTopWindowInsets
                            ComponentView(
                                style = child,
                                state = state,
                                onClick = clickHandler,
                                componentInteractionTracker = componentInteractionTracker,
                                modifier = Modifier
                                    .conditional(applyTopInsets && !stackState.ignoreHeaderHeight) {
                                        // Read header height at layout time. If set (hero case),
                                        // it already includes status bar padding. Otherwise fall
                                        // back to the safe-drawing top inset.
                                        headerOrInsetsTopPadding(state, topInsetPx)
                                    }
                                    .conditional(applyTopInsets && stackState.ignoreHeaderHeight) {
                                        windowInsetsPadding(safeDrawingInsets.only(WindowInsetsSides.Top))
                                    }
                                    .alpha(contentAlpha),
                            )
                        }
                    }
                }
            }
        }
    }

    val backgroundStyle = stackState.background?.let { rememberBackgroundStyle(background = it) }
    val composeShape by remember(stackState.shape) { derivedStateOf { stackState.shape.toShape() } }
    val borderStyle = stackState.border?.let { rememberBorderStyle(border = it) }
    val shadowStyle = if (shouldApplyShadow) {
        stackState.shadow?.let { rememberShadowStyle(shadow = it) }
    } else {
        null
    }

    val outerShapeModifier = remember(backgroundStyle, shadowStyle) {
        Modifier
            .padding(stackState.margin)
            .applyIfNotNull(shadowStyle) { shadow(it, composeShape) }
            .applyIfNotNull(backgroundStyle) { background(it, composeShape) }
    }

    val borderModifier = remember(stackState, borderStyle) {
        Modifier
            .applyIfNotNull(borderStyle) {
                border(it, composeShape)
                    .padding(it.width)
            }
    }

    val innerShapeModifier = remember(stackState, borderStyle) {
        Modifier
            .padding(stackState.padding)
    }

    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val indication = LocalIndication.current
    val clickModifier = if (onStackClick != null) {
        Modifier.clickable(
            interactionSource = resolvedInteractionSource,
            indication = indication,
            enabled = enabled,
            onClick = onStackClick,
        )
    } else {
        Modifier
    }
    val plainPathClickModifier = if (onStackClick != null) {
        Modifier.clickable(
            interactionSource = resolvedInteractionSource,
            indication = null,
            enabled = enabled,
            onClick = onStackClick,
        )
    } else {
        Modifier
    }

    if (nestedBadge == null && overlay == null) {
        if (backgroundStyle is BackgroundStyle.Video) {
            // Video backgrounds require a Box wrapper with explicit sizing
            WithOptionalBackgroundOverlay(
                state = state,
                background = backgroundStyle,
                shape = composeShape,
                modifier = modifier
                    .size(stackState.size)
                    .then(outerShapeModifier)
                    .clip(composeShape)
                    .then(clickModifier)
                    .then(borderModifier),
            ) {
                stack(
                    Modifier
                        .then(innerShapeModifier)
                        .conditional(stackState.applyBottomWindowInsets) {
                            windowInsetsPadding(safeDrawingInsets.only(WindowInsetsSides.Bottom))
                        }
                        .conditional(stackState.applyHorizontalWindowInsets) {
                            windowInsetsPadding(safeDrawingInsets.only(WindowInsetsSides.Horizontal))
                        },
                )
            }
        } else if (onStackClick != null) {
            // Draw the ripple on a sibling Box so the shape clip bounds only the ripple, not
            // nested content that may extend outside the parent (e.g. badges with offsets).
            Box {
                stack(
                    outerShapeModifier
                        .then(plainPathClickModifier)
                        .then(borderModifier)
                        .then(innerShapeModifier)
                        .conditional(stackState.applyBottomWindowInsets) {
                            windowInsetsPadding(safeDrawingInsets.only(WindowInsetsSides.Bottom))
                        }
                        .conditional(stackState.applyHorizontalWindowInsets) {
                            windowInsetsPadding(safeDrawingInsets.only(WindowInsetsSides.Horizontal))
                        },
                )
                Box(
                    Modifier
                        .matchParentSize()
                        .padding(stackState.margin)
                        .clip(composeShape)
                        .indication(resolvedInteractionSource, indication),
                )
            }
        } else {
            stack(
                outerShapeModifier
                    .then(borderModifier)
                    .then(innerShapeModifier)
                    .conditional(stackState.applyBottomWindowInsets) {
                        windowInsetsPadding(safeDrawingInsets.only(WindowInsetsSides.Bottom))
                    }
                    .conditional(stackState.applyHorizontalWindowInsets) {
                        windowInsetsPadding(safeDrawingInsets.only(WindowInsetsSides.Horizontal))
                    },
            )
        }
    } else if (nestedBadge != null) {
        Box(
            modifier = modifier
                .then(outerShapeModifier)
                .clip(composeShape)
                .then(clickModifier)
                .then(borderModifier),
        ) {
            WithOptionalBackgroundOverlay(state, background = backgroundStyle) {
                stack(Modifier.then(innerShapeModifier))
            }

            StackComponentView(
                nestedBadge.stackStyle,
                state,
                clickHandler = {},
                modifier = Modifier
                    .align(nestedBadge.alignment.toAlignment()),
                componentInteractionTracker = componentInteractionTracker,
            )
        }
    } else if (overlay != null) {
        Box(
            modifier = modifier
                .then(outerShapeModifier)
                .clip(composeShape)
                .then(clickModifier),
        ) {
            WithOptionalBackgroundOverlay(state, background = backgroundStyle) {
                stack(borderModifier.then(innerShapeModifier))
            }
            overlay()
        }
    }
}

private val TwoDimensionalAlignment.isTop: Boolean
    get() = when (this) {
        TwoDimensionalAlignment.TOP_LEADING,
        TwoDimensionalAlignment.TOP,
        TwoDimensionalAlignment.TOP_TRAILING,
        -> true

        TwoDimensionalAlignment.CENTER,
        TwoDimensionalAlignment.LEADING,
        TwoDimensionalAlignment.TRAILING,
        TwoDimensionalAlignment.BOTTOM,
        TwoDimensionalAlignment.BOTTOM_LEADING,
        TwoDimensionalAlignment.BOTTOM_TRAILING,
        -> false
    }

private fun getOverlaidBadgeOffsetY(
    height: Int,
    alignment: TwoDimensionalAlignment,
    mainStackBorderWidthPx: Float = 0f,
) = when (alignment) {
    TwoDimensionalAlignment.CENTER,
    TwoDimensionalAlignment.LEADING,
    TwoDimensionalAlignment.TRAILING,
    -> 0

    TwoDimensionalAlignment.TOP,
    TwoDimensionalAlignment.TOP_LEADING,
    TwoDimensionalAlignment.TOP_TRAILING,
    -> (-((height.toFloat() - mainStackBorderWidthPx) / 2)).roundToInt()

    TwoDimensionalAlignment.BOTTOM,
    TwoDimensionalAlignment.BOTTOM_LEADING,
    TwoDimensionalAlignment.BOTTOM_TRAILING,
    -> ((height.toFloat() - mainStackBorderWidthPx) / 2).roundToInt()
}

/**
 * Make this CornerSize absolute, based on the provided [placeable]. This is useful for turning relative
 * (percentage-based) corners into absolute ones.
 */
private fun CornerSize.makeAbsolute(placeable: Placeable, density: Density) =
    makeAbsolute(
        shapeSize = ComposeSize(
            width = placeable.width.toFloat(),
            height = placeable.height.toFloat(),
        ),
        density = density,
    )

/**
 * Make this CornerSize absolute, based on the provided [shapeSize]. This is useful for turning relative
 * (percentage-based) corners into absolute ones.
 */
private fun CornerSize.makeAbsolute(shapeSize: ComposeSize, density: Density) =
    CornerSize(size = toPx(shapeSize, density))

internal val FlexDistribution.usesAllAvailableSpace: Boolean
    get() = when (this) {
        FlexDistribution.SPACE_AROUND,
        FlexDistribution.SPACE_BETWEEN,
        FlexDistribution.SPACE_EVENLY,
        -> true

        FlexDistribution.START,
        FlexDistribution.END,
        FlexDistribution.CENTER,
        -> false
    }

private val ComponentStyle.shouldIgnoreTopWindowInsets: Boolean
    get() = when (this) {
        is ImageComponentStyle -> ignoreTopWindowInsets
        is VideoComponentStyle -> ignoreTopWindowInsets
        else -> false
    }

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun StackComponentView_Preview_Vertical() {
    Box(
        modifier = Modifier.padding(all = 32.dp),
    ) {
        StackComponentView(
            style = StackComponentStyle(
                children = previewChildren(),
                dimension = Dimension.Vertical(
                    alignment = HorizontalAlignment.CENTER,
                    distribution = FlexDistribution.START,
                ),
                visible = true,
                size = Size(width = Fit, height = Fit),
                spacing = 16.dp,
                background = BackgroundStyles.Color(
                    ColorStyles(
                        light = ColorStyle.Solid(Color.Red),
                        dark = ColorStyle.Solid(Color.Yellow),
                    ),
                ),
                padding = PaddingValues(all = 16.dp),
                margin = PaddingValues(all = 16.dp),
                shape = Shape.Rectangle(CornerRadiuses.Dp(all = 20.0)),
                border = BorderStyles(width = 2.dp, colors = ColorStyles(light = ColorStyle.Solid(Color.Blue))),
                shadow = ShadowStyles(
                    colors = ColorStyles(ColorStyle.Solid(Color.Black)),
                    radius = 10.dp,
                    x = 0.dp,
                    y = 3.dp,
                ),
                badge = null,
                scrollOrientation = null,
                rcPackage = null,
                tabIndex = null,
                countdownDate = null,
                countFrom = CountdownComponent.CountFrom.DAYS,
                overrides = emptyList(),
            ),
            state = previewEmptyState(),
            clickHandler = {},
        )
    }
}

@Suppress("MagicNumber")
@Preview
@Composable
private fun StackComponentView_Preview_Scroll_VerticalStack_VerticalScroll() {
    val children = (0..30).map {
        previewTextComponentStyle(
            text = "Hello $it",
            backgroundColor = ColorStyles(
                light = ColorStyle.Solid(Color.Blue),
            ),
            visible = true,
            size = Size(width = Fit, height = Fit),
            padding = Padding(top = 8.0, bottom = 8.0, leading = 8.0, trailing = 8.0),
        )
    }
    Box(
        modifier = Modifier.padding(all = 32.dp),
    ) {
        StackComponentView(
            style = StackComponentStyle(
                children = children,
                dimension = Dimension.Vertical(
                    alignment = HorizontalAlignment.CENTER,
                    distribution = FlexDistribution.START,
                ),
                visible = true,
                size = Size(width = Fit, height = Fit),
                spacing = 16.dp,
                background = BackgroundStyles.Color(
                    ColorStyles(
                        light = ColorStyle.Solid(Color.Red),
                        dark = ColorStyle.Solid(Color.Yellow),
                    ),
                ),
                padding = PaddingValues(all = 16.dp),
                margin = PaddingValues(all = 16.dp),
                shape = Shape.Rectangle(CornerRadiuses.Dp(all = 20.0)),
                border = BorderStyles(width = 2.dp, colors = ColorStyles(light = ColorStyle.Solid(Color.Blue))),
                shadow = ShadowStyles(
                    colors = ColorStyles(ColorStyle.Solid(Color.Black)),
                    radius = 10.dp,
                    x = 0.dp,
                    y = 3.dp,
                ),
                badge = null,
                scrollOrientation = Orientation.Vertical,
                rcPackage = null,
                tabIndex = null,
                countdownDate = null,
                countFrom = CountdownComponent.CountFrom.DAYS,
                overrides = emptyList(),
            ),
            state = previewEmptyState(),
            clickHandler = {},
        )
    }
}

private class BadgeAlignmentProvider : PreviewParameterProvider<TwoDimensionalAlignment> {
    override val values = listOf(
        TwoDimensionalAlignment.TOP_LEADING,
        TwoDimensionalAlignment.TOP,
        TwoDimensionalAlignment.TOP_TRAILING,
        TwoDimensionalAlignment.BOTTOM_LEADING,
        TwoDimensionalAlignment.BOTTOM,
        TwoDimensionalAlignment.BOTTOM_TRAILING,
    ).asSequence()
}

@Preview
@Composable
private fun StackComponentView_Preview_Overlay_Badge(
    @PreviewParameter(BadgeAlignmentProvider::class) alignment: TwoDimensionalAlignment,
) {
    Box(
        modifier = Modifier.padding(all = 32.dp),
    ) {
        val badgeShape = Shape.Rectangle(
            corners = CornerRadiuses.Dp(
                topLeading = 20.0,
                topTrailing = 20.0,
                bottomLeading = 20.0,
                bottomTrailing = 20.0,
            ),
        )
        StackComponentView(
            style = StackComponentStyle(
                children = previewChildren(),
                dimension = Dimension.Vertical(
                    alignment = HorizontalAlignment.CENTER,
                    distribution = FlexDistribution.START,
                ),
                visible = true,
                size = Size(width = Fixed(200u), height = Fit),
                spacing = 16.dp,
                background = BackgroundStyles.Color(
                    ColorStyles(
                        light = ColorStyle.Solid(Color.Red),
                    ),
                ),
                padding = PaddingValues(all = 12.dp),
                margin = PaddingValues(all = 12.dp),
                shape = Shape.Rectangle(CornerRadiuses.Dp(all = 20.0)),
                border = BorderStyles(width = 10.dp, colors = ColorStyles(light = ColorStyle.Solid(Color.Blue))),
                shadow = ShadowStyles(
                    colors = ColorStyles(ColorStyle.Solid(Color.Black)),
                    radius = 20.dp,
                    x = 0.dp,
                    y = 5.dp,
                ),
                badge = previewBadge(
                    Badge.Style.Overlay,
                    alignment,
                    badgeShape,
                    margin = PaddingValues(horizontal = 8.dp),
                ),
                scrollOrientation = null,
                rcPackage = null,
                tabIndex = null,
                countdownDate = null,
                countFrom = CountdownComponent.CountFrom.DAYS,
                overrides = emptyList(),
            ),
            state = previewEmptyState(),
            clickHandler = { },
        )
    }
}

@Preview
@Composable
private fun StackComponentView_Preview_EdgeToEdge_Badge(
    @PreviewParameter(BadgeAlignmentProvider::class) alignment: TwoDimensionalAlignment,
) {
    Box(
        modifier = Modifier.padding(all = 32.dp),
    ) {
        val badgeShape = Shape.Rectangle(
            corners = CornerRadiuses.Dp(all = 20.0),
        )
        StackComponentView(
            style = previewStackComponentStyle(
                children = previewChildren(),
                badge = previewBadge(Badge.Style.EdgeToEdge, alignment, badgeShape),
                shadow = ShadowStyles(
                    colors = ColorStyles(ColorStyle.Solid(Color.Black)),
                    radius = 20.dp,
                    x = 0.dp,
                    y = 5.dp,
                ),
            ),
            state = previewEmptyState(),
            clickHandler = { },
        )
    }
}

@Preview
@Composable
private fun StackComponentView_Preview_Pill_EdgeToEdge_Badge(
    @PreviewParameter(BadgeAlignmentProvider::class) alignment: TwoDimensionalAlignment,
) {
    Box(
        modifier = Modifier.padding(all = 32.dp),
    ) {
        StackComponentView(
            style = StackComponentStyle(
                children = previewChildren(),
                dimension = Dimension.Vertical(
                    alignment = HorizontalAlignment.CENTER,
                    distribution = FlexDistribution.START,
                ),
                visible = true,
                size = Size(width = Fixed(200u), height = Fit),
                spacing = 16.dp,
                background = BackgroundStyles.Color(
                    ColorStyles(
                        light = ColorStyle.Solid(Color.Red),
                    ),
                ),
                padding = PaddingValues(all = 0.dp),
                margin = PaddingValues(all = 0.dp),
                shape = Shape.Pill,
                border = BorderStyles(width = 2.dp, colors = ColorStyles(light = ColorStyle.Solid(Color.Blue))),
                shadow = ShadowStyles(
                    colors = ColorStyles(ColorStyle.Solid(Color.Black)),
                    radius = 20.dp,
                    x = 0.dp,
                    y = 5.dp,
                ),
                badge = previewBadge(Badge.Style.EdgeToEdge, alignment, Shape.Pill),
                scrollOrientation = null,
                rcPackage = null,
                tabIndex = null,
                countdownDate = null,
                countFrom = CountdownComponent.CountFrom.DAYS,
                overrides = emptyList(),
            ),
            state = previewEmptyState(),
            clickHandler = { },
        )
    }
}

@Preview
@Composable
private fun StackComponentView_Preview_Nested_Badge(
    @PreviewParameter(BadgeAlignmentProvider::class) alignment: TwoDimensionalAlignment,
) {
    Box(
        modifier = Modifier.padding(all = 32.dp),
    ) {
        val badgeShape = Shape.Rectangle(
            corners = CornerRadiuses.Dp(
                topLeading = 20.0,
                topTrailing = 20.0,
                bottomLeading = 20.0,
                bottomTrailing = 20.0,
            ),
        )
        StackComponentView(
            style = StackComponentStyle(
                children = previewChildren(),
                dimension = Dimension.Vertical(
                    alignment = HorizontalAlignment.CENTER,
                    distribution = FlexDistribution.START,
                ),
                visible = true,
                size = Size(width = Fixed(200u), height = Fit),
                spacing = 16.dp,
                background = BackgroundStyles.Color(
                    ColorStyles(
                        light = ColorStyle.Solid(Color.Red),
                    ),
                ),
                padding = PaddingValues(all = 20.dp),
                margin = PaddingValues(all = 0.dp),
                shape = Shape.Rectangle(CornerRadiuses.Dp(all = 20.0)),
                border = BorderStyles(width = 10.dp, colors = ColorStyles(light = ColorStyle.Solid(Color.Yellow))),
                shadow = ShadowStyles(
                    colors = ColorStyles(ColorStyle.Solid(Color.Black)),
                    radius = 20.dp,
                    x = 0.dp,
                    y = 5.dp,
                ),
                badge = previewBadge(Badge.Style.Nested, alignment, badgeShape),
                scrollOrientation = null,
                rcPackage = null,
                tabIndex = null,
                countdownDate = null,
                countFrom = CountdownComponent.CountFrom.DAYS,
                overrides = emptyList(),
            ),
            state = previewEmptyState(),
            clickHandler = { },
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun StackComponentView_Preview_Horizontal() {
    Box(
        modifier = Modifier.padding(all = 32.dp),
    ) {
        StackComponentView(
            style = StackComponentStyle(
                children = previewChildren(),
                dimension = Dimension.Horizontal(
                    alignment = VerticalAlignment.CENTER,
                    distribution = FlexDistribution.START,
                ),
                visible = true,
                size = Size(width = Fit, height = Fit),
                spacing = 16.dp,
                background = BackgroundStyles.Color(
                    ColorStyles(
                        light = ColorStyle.Solid(Color.Red),
                        dark = ColorStyle.Solid(Color.Yellow),
                    ),
                ),
                padding = PaddingValues(all = 16.dp),
                margin = PaddingValues(all = 16.dp),
                shape = Shape.Rectangle(CornerRadiuses.Dp(all = 20.0)),
                border = BorderStyles(width = 2.dp, colors = ColorStyles(light = ColorStyle.Solid(Color.Blue))),
                shadow = ShadowStyles(
                    colors = ColorStyles(ColorStyle.Solid(Color.Black)),
                    radius = 30.dp,
                    x = 0.dp,
                    y = 5.dp,
                ),
                badge = null,
                scrollOrientation = null,
                rcPackage = null,
                tabIndex = null,
                countdownDate = null,
                countFrom = CountdownComponent.CountFrom.DAYS,
                overrides = emptyList(),
            ),
            state = previewEmptyState(),
            clickHandler = { },
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun StackComponentView_Preview_Children_Extend_Over_Parent() {
    Box(
        modifier = Modifier
            .padding(all = 32.dp)
            .background(Color.Gray),
    ) {
        StackComponentView(
            style = StackComponentStyle(
                children = listOf(
                    previewStackComponentStyle(
                        children = previewChildren(),
                        shadow = ShadowStyles(
                            colors = ColorStyles(ColorStyle.Solid(Color.Black)),
                            radius = 10.dp,
                            x = 0.dp,
                            y = 3.dp,
                        ),
                        badge = previewBadge(
                            Badge.Style.Overlay,
                            TwoDimensionalAlignment.TOP_TRAILING,
                            Shape.Rectangle(),
                        ),
                    ),
                ),
                dimension = Dimension.Horizontal(
                    alignment = VerticalAlignment.CENTER,
                    distribution = FlexDistribution.START,
                ),
                visible = true,
                size = Size(width = Fit, height = Fit),
                spacing = 16.dp,
                background = BackgroundStyles.Color(
                    ColorStyles(
                        light = ColorStyle.Solid(Color.Red),
                        dark = ColorStyle.Solid(Color.Yellow),
                    ),
                ),
                padding = PaddingValues(all = 0.dp),
                margin = PaddingValues(all = 16.dp),
                shape = Shape.Rectangle(CornerRadiuses.Dp(all = 20.0)),
                border = null,
                shadow = null,
                badge = null,
                scrollOrientation = null,
                rcPackage = null,
                tabIndex = null,
                countdownDate = null,
                countFrom = CountdownComponent.CountFrom.DAYS,
                overrides = emptyList(),
            ),
            state = previewEmptyState(),
            clickHandler = { },
        )
    }
}

@Suppress("MagicNumber")
@Preview
@Composable
private fun StackComponentView_Preview_Scroll_HorizontalStack_HorizontalScroll() {
    val children = (0..10).map {
        previewTextComponentStyle(
            text = "Hello $it",
            backgroundColor = ColorStyles(
                light = ColorStyle.Solid(Color.Blue),
            ),
            visible = true,
            size = Size(width = Fit, height = Fit),
            padding = Padding(top = 8.0, bottom = 8.0, leading = 8.0, trailing = 8.0),
        )
    }
    Box(
        modifier = Modifier.padding(all = 32.dp),
    ) {
        StackComponentView(
            style = StackComponentStyle(
                children = children,
                dimension = Dimension.Horizontal(
                    alignment = VerticalAlignment.CENTER,
                    distribution = FlexDistribution.START,
                ),
                visible = true,
                size = Size(width = Fit, height = Fit),
                spacing = 16.dp,
                background = BackgroundStyles.Color(
                    ColorStyles(
                        light = ColorStyle.Solid(Color.Red),
                        dark = ColorStyle.Solid(Color.Yellow),
                    ),
                ),
                padding = PaddingValues(all = 16.dp),
                margin = PaddingValues(all = 16.dp),
                shape = Shape.Rectangle(CornerRadiuses.Dp(all = 20.0)),
                border = BorderStyles(width = 2.dp, colors = ColorStyles(light = ColorStyle.Solid(Color.Blue))),
                shadow = ShadowStyles(
                    colors = ColorStyles(ColorStyle.Solid(Color.Black)),
                    radius = 10.dp,
                    x = 0.dp,
                    y = 5.dp,
                ),
                badge = null,
                scrollOrientation = Orientation.Horizontal,
                rcPackage = null,
                tabIndex = null,
                countdownDate = null,
                countFrom = CountdownComponent.CountFrom.DAYS,
                overrides = emptyList(),
            ),
            state = previewEmptyState(),
            clickHandler = { },
        )
    }
}

@Suppress("LongMethod")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun StackComponentView_Preview_ZLayer() {
    Box(
        modifier = Modifier.padding(all = 32.dp),
    ) {
        StackComponentView(
            style = StackComponentStyle(
                children = listOf(
                    previewTextComponentStyle(
                        text = "Hello",
                        backgroundColor = ColorStyles(
                            light = ColorStyle.Solid(Color.Yellow),
                            dark = ColorStyle.Solid(Color.Red),
                        ),
                        size = Size(width = Fit, height = Fit),
                        padding = Padding(top = 8.0, bottom = 8.0, leading = 8.0, trailing = 8.0),
                        margin = Padding(top = 0.0, bottom = 24.0, leading = 0.0, trailing = 24.0),
                    ),
                    previewTextComponentStyle(
                        text = "World",
                        backgroundColor = ColorStyles(
                            light = ColorStyle.Solid(Color.Blue),
                        ),
                        size = Size(width = Fit, height = Fit),
                        padding = Padding(top = 8.0, bottom = 8.0, leading = 8.0, trailing = 8.0),
                    ),
                ),
                dimension = Dimension.ZLayer(alignment = TwoDimensionalAlignment.BOTTOM_TRAILING),
                visible = true,
                size = Size(width = Fit, height = Fit),
                spacing = 16.dp,
                background = BackgroundStyles.Color(
                    ColorStyles(
                        light = ColorStyle.Solid(Color.Red),
                        dark = ColorStyle.Solid(Color.Yellow),
                    ),
                ),
                padding = PaddingValues(all = 16.dp),
                margin = PaddingValues(all = 16.dp),
                shape = Shape.Rectangle(CornerRadiuses.Dp(all = 20.0)),
                border = BorderStyles(width = 2.dp, colors = ColorStyles(light = ColorStyle.Solid(Color.Blue))),
                shadow = ShadowStyles(
                    colors = ColorStyles(ColorStyle.Solid(Color.Black)),
                    radius = 20.dp,
                    x = 5.dp,
                    y = 5.dp,
                ),
                badge = null,
                scrollOrientation = null,
                rcPackage = null,
                tabIndex = null,
                countdownDate = null,
                countFrom = CountdownComponent.CountFrom.DAYS,
                overrides = emptyList(),
            ),
            state = previewEmptyState(),
            clickHandler = { },
        )
    }
}

@Preview
@Composable
private fun StackComponentView_Preview_HorizontalChildrenFillWidth() {
    StackComponentView(
        style = StackComponentStyle(
            children = listOf(
                previewTextComponentStyle(
                    text = "Hello",
                    backgroundColor = ColorStyles(ColorStyle.Solid(Color.Yellow)),
                    size = Size(width = Fill, height = Fit),
                ),
                previewTextComponentStyle(
                    text = "World",
                    backgroundColor = ColorStyles(ColorStyle.Solid(Color.Blue)),
                    size = Size(width = Fill, height = Fit),
                ),
            ),
            dimension = Dimension.Horizontal(
                alignment = VerticalAlignment.CENTER,
                distribution = FlexDistribution.START,
            ),
            visible = true,
            size = Size(width = Fixed(200u), height = Fit),
            spacing = 16.dp,
            background = BackgroundStyles.Color(ColorStyles(light = ColorStyle.Solid(Color.Red))),
            padding = PaddingValues(all = 16.dp),
            margin = PaddingValues(all = 16.dp),
            shape = Shape.Rectangle(corners = null),
            border = null,
            shadow = null,
            scrollOrientation = null,
            overrides = emptyList(),
            rcPackage = null,
            tabIndex = null,
            countdownDate = null,
            countFrom = CountdownComponent.CountFrom.DAYS,
            badge = null,
        ),
        state = previewEmptyState(),
        clickHandler = { },
    )
}

@Preview
@Composable
private fun StackComponentView_Preview_VerticalChildrenFillHeight() {
    StackComponentView(
        style = StackComponentStyle(
            children = listOf(
                previewTextComponentStyle(
                    text = "Hello",
                    backgroundColor = ColorStyles(ColorStyle.Solid(Color.Yellow)),
                    size = Size(width = Fit, height = Fill),
                ),
                previewTextComponentStyle(
                    text = "World",
                    backgroundColor = ColorStyles(ColorStyle.Solid(Color.Blue)),
                    size = Size(width = Fit, height = Fill),
                ),
            ),
            dimension = Dimension.Vertical(
                alignment = HorizontalAlignment.CENTER,
                distribution = FlexDistribution.START,
            ),
            visible = true,
            size = Size(width = Fit, height = Fixed(200u)),
            spacing = 16.dp,
            background = BackgroundStyles.Color(ColorStyles(light = ColorStyle.Solid(Color.Red))),
            padding = PaddingValues(all = 16.dp),
            margin = PaddingValues(all = 16.dp),
            shape = Shape.Rectangle(),
            border = null,
            shadow = null,
            scrollOrientation = null,
            overrides = emptyList(),
            rcPackage = null,
            tabIndex = null,
            countdownDate = null,
            countFrom = CountdownComponent.CountFrom.DAYS,
            badge = null,
        ),
        state = previewEmptyState(),
        clickHandler = { },
    )
}

/**
 * Provides all FlexDistributions, in both Horizontal and Vertical dimensions.
 */
private class DistributionProvider : PreviewParameterProvider<Dimension> {
    override val values: Sequence<Dimension> = FlexDistribution.values().asSequence().flatMap { distribution ->
        sequenceOf(
            Dimension.Horizontal(alignment = VerticalAlignment.CENTER, distribution = distribution),
            Dimension.Vertical(alignment = HorizontalAlignment.CENTER, distribution = distribution),
        )
    }
}

@Preview
@Composable
private fun StackComponentView_Preview_Distribution_Without_Spacing_Fit_Size(
    @PreviewParameter(DistributionProvider::class) dimension: Dimension,
) {
    val distribution = when (dimension) {
        is Dimension.Horizontal -> dimension.distribution
        is Dimension.Vertical -> dimension.distribution
        is Dimension.ZLayer -> null
    }
    StackComponentView(
        style = StackComponentStyle(
            children = listOf(
                previewTextComponentStyle(
                    text = "Hello",
                    backgroundColor = ColorStyles(ColorStyle.Solid(Color.Yellow)),
                    size = Size(width = Fit, height = Fit),
                ),
                previewTextComponentStyle(
                    text = distribution?.name ?: "null",
                    backgroundColor = ColorStyles(ColorStyle.Solid(Color.Green)),
                    size = Size(width = Fit, height = Fit),
                ),
                previewTextComponentStyle(
                    text = "World",
                    backgroundColor = ColorStyles(ColorStyle.Solid(Color.Blue)),
                    size = Size(width = Fit, height = Fit),
                ),
            ),
            dimension = dimension,
            visible = true,
            // It's all set to Fit, because we want to see the `spacing` being interpreted as a minimum.
            size = Size(width = Fit, height = Fit),
            spacing = 0.dp,
            background = BackgroundStyles.Color(ColorStyles(light = ColorStyle.Solid(Color.Red))),
            padding = PaddingValues(all = 0.dp),
            margin = PaddingValues(all = 16.dp),
            shape = Shape.Rectangle(),
            border = null,
            shadow = null,
            badge = null,
            scrollOrientation = null,
            rcPackage = null,
            tabIndex = null,
            countdownDate = null,
            countFrom = CountdownComponent.CountFrom.DAYS,
            overrides = emptyList(),
        ),
        state = previewEmptyState(),
        clickHandler = { },
    )
}

@Preview
@Composable
private fun StackComponentView_Preview_Distribution_Without_Spacing(
    @PreviewParameter(DistributionProvider::class) dimension: Dimension,
) {
    val distribution = when (dimension) {
        is Dimension.Horizontal -> dimension.distribution
        is Dimension.Vertical -> dimension.distribution
        is Dimension.ZLayer -> null
    }
    StackComponentView(
        style = StackComponentStyle(
            children = listOf(
                previewTextComponentStyle(
                    text = "Hello",
                    backgroundColor = ColorStyles(ColorStyle.Solid(Color.Yellow)),
                    size = Size(width = Fit, height = Fit),
                ),
                previewTextComponentStyle(
                    text = distribution?.name ?: "null",
                    backgroundColor = ColorStyles(ColorStyle.Solid(Color.Green)),
                    size = Size(width = Fit, height = Fit),
                ),
                previewTextComponentStyle(
                    text = "World",
                    backgroundColor = ColorStyles(ColorStyle.Solid(Color.Blue)),
                    size = Size(width = Fit, height = Fit),
                ),
            ),
            dimension = dimension,
            visible = true,
            // It's all set to Fit, because we want to see the `spacing` being interpreted as a minimum.
            size = Size(width = Fixed(300u), height = Fixed(300u)),
            spacing = 0.dp,
            background = BackgroundStyles.Color(ColorStyles(light = ColorStyle.Solid(Color.Red))),
            padding = PaddingValues(all = 0.dp),
            margin = PaddingValues(all = 16.dp),
            shape = Shape.Rectangle(),
            border = null,
            shadow = null,
            badge = null,
            scrollOrientation = null,
            rcPackage = null,
            tabIndex = null,
            countdownDate = null,
            countFrom = CountdownComponent.CountFrom.DAYS,
            overrides = emptyList(),
        ),
        state = previewEmptyState(),
        clickHandler = { },
    )
}

@Preview
@Composable
private fun StackComponentView_Preview_Distribution_SpaceAround_With_Fill_Children() {
    StackComponentView(
        style = StackComponentStyle(
            children = listOf(
                previewTextComponentStyle(
                    text = "Hello",
                    backgroundColor = ColorStyles(ColorStyle.Solid(Color.Yellow)),
                    size = Size(width = Fill, height = Fit),
                ),
                previewTextComponentStyle(
                    text = "SPACE_AROUND",
                    backgroundColor = ColorStyles(ColorStyle.Solid(Color.Green)),
                    size = Size(width = Fit, height = Fit),
                ),
                previewTextComponentStyle(
                    text = "World",
                    backgroundColor = ColorStyles(ColorStyle.Solid(Color.Blue)),
                    size = Size(width = Fit, height = Fit),
                ),
            ),
            dimension = Dimension.Horizontal(
                alignment = VerticalAlignment.CENTER,
                distribution = FlexDistribution.SPACE_AROUND,
            ),
            visible = true,
            // It's all set to Fit, because we want to see the `spacing` being interpreted as a minimum.
            size = Size(width = Fixed(300u), height = Fixed(300u)),
            spacing = 8.dp,
            background = BackgroundStyles.Color(ColorStyles(light = ColorStyle.Solid(Color.Red))),
            padding = PaddingValues(all = 0.dp),
            margin = PaddingValues(all = 16.dp),
            shape = Shape.Rectangle(),
            border = null,
            shadow = null,
            badge = null,
            scrollOrientation = null,
            rcPackage = null,
            tabIndex = null,
            countdownDate = null,
            countFrom = CountdownComponent.CountFrom.DAYS,
            overrides = emptyList(),
        ),
        state = previewEmptyState(),
        clickHandler = { },
    )
}

@Preview
@Composable
private fun StackComponentView_Preview_Distribution_With_Spacing(
    @PreviewParameter(DistributionProvider::class) dimension: Dimension,
) {
    val distribution = when (dimension) {
        is Dimension.Horizontal -> dimension.distribution
        is Dimension.Vertical -> dimension.distribution
        is Dimension.ZLayer -> null
    }
    StackComponentView(
        style = StackComponentStyle(
            children = listOf(
                previewTextComponentStyle(
                    text = "Hello",
                    backgroundColor = ColorStyles(ColorStyle.Solid(Color.Yellow)),
                    size = Size(width = Fit, height = Fit),
                ),
                previewTextComponentStyle(
                    text = distribution?.name ?: "null",
                    backgroundColor = ColorStyles(ColorStyle.Solid(Color.Green)),
                    size = Size(width = Fit, height = Fit),
                ),
                previewTextComponentStyle(
                    text = "World",
                    backgroundColor = ColorStyles(ColorStyle.Solid(Color.Blue)),
                    size = Size(width = Fit, height = Fit),
                ),
            ),
            dimension = dimension,
            visible = true,
            // It's all set to Fit, because we want to see the `spacing` being interpreted as a minimum.
            size = Size(width = Fixed(300u), height = Fixed(300u)),
            spacing = 16.dp,
            background = BackgroundStyles.Color(ColorStyles(light = ColorStyle.Solid(Color.Red))),
            padding = PaddingValues(all = 0.dp),
            margin = PaddingValues(all = 16.dp),
            shape = Shape.Rectangle(),
            border = null,
            shadow = null,
            badge = null,
            scrollOrientation = null,
            rcPackage = null,
            tabIndex = null,
            countdownDate = null,
            countFrom = CountdownComponent.CountFrom.DAYS,
            overrides = emptyList(),
        ),
        state = previewEmptyState(),
        clickHandler = { },
    )
}

@Preview
@Composable
private fun StackComponentView_Preview_HorizontalDivider() {
    Column(
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Text("There should be a divider below this text.")
        StackComponentView(
            style = previewStackComponentStyle(
                children = emptyList(),
                visible = true,
                size = Size(width = Fill, height = Fixed(1u)),
                dimension = Dimension.Vertical(
                    alignment = HorizontalAlignment.LEADING,
                    FlexDistribution.SPACE_BETWEEN,
                ),
                spacing = 0.dp,
                // Explicitly applying vertical margin to make sure it doesn't "eat up" the divider.
                margin = PaddingValues(vertical = 40.dp),
                border = null,
                background = BackgroundStyles.Color(
                    color = ColorStyles(ColorStyle.Solid(Color(red = 0xc8, green = 0xc8, blue = 0xc8))),
                ),
            ),
            state = previewEmptyState(),
            clickHandler = { },
        )
        Text("There should be a divider above this text.")
    }
}

@Preview
@Composable
private fun StackComponentView_Preview_VerticalDivider() {
    Row(
        modifier = Modifier.height(100.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Text(
            text = "There should be a divider to the right of this text.",
            modifier = Modifier.weight(1f),
        )
        StackComponentView(
            style = previewStackComponentStyle(
                children = emptyList(),
                visible = true,
                size = Size(width = Fixed(1u), height = Fill),
                dimension = Dimension.Horizontal(alignment = VerticalAlignment.TOP, FlexDistribution.SPACE_BETWEEN),
                spacing = 0.dp,
                // Explicitly applying horizontal margin to make sure it doesn't "eat up" the divider.
                margin = PaddingValues(horizontal = 40.dp),
                border = null,
                background = BackgroundStyles.Color(
                    color = ColorStyles(ColorStyle.Solid(Color(red = 0xc8, green = 0xc8, blue = 0xc8))),
                ),
            ),
            state = previewEmptyState(),
            clickHandler = { },
        )
        Text(
            text = "There should be a divider to the left of this text.",
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview
@Composable
private fun StackComponentView_Preview_ContentAlpha() {
    StackComponentView(
        style = previewStackComponentStyle(
            children = previewChildren(),
        ),
        state = previewEmptyState(),
        clickHandler = {},
        contentAlpha = 0.6f,
    )
}

// Regression guard: when the parent stack is clickable, the ripple's shape clip must not
// also clip nested children that draw outside the parent's bounds. Here the inner stack
// has a generous shadow that should bleed past the outer's rounded edge — if a future
// change reintroduces a graphics-layer clip on the clickable layout node, the shadow will
// be visibly clipped at the outer's rounded corners in this preview.
@Preview(showBackground = true, backgroundColor = 0xFFEEEEEE)
@Composable
private fun StackComponentView_Preview_Clickable_With_Overflowing_Child_Shadow() {
    StackComponentView(
        style = previewStackComponentStyle(
            children = listOf(
                previewStackComponentStyle(
                    children = listOf(
                        previewTextComponentStyle(
                            text = "Nested",
                            size = Size(width = Fit, height = Fit),
                        ),
                    ),
                    size = Size(width = Fit, height = Fit),
                    padding = PaddingValues(all = 16.dp),
                    margin = PaddingValues(all = 0.dp),
                    background = BackgroundStyles.Color(ColorStyles(ColorStyle.Solid(Color.White))),
                    shape = Shape.Rectangle(CornerRadiuses.Dp(all = 4.0)),
                    border = null,
                    shadow = ShadowStyles(
                        colors = ColorStyles(ColorStyle.Solid(Color.Black)),
                        radius = 24.dp,
                        x = 0.dp,
                        y = 0.dp,
                    ),
                ),
            ),
            size = Size(width = Fit, height = Fit),
            padding = PaddingValues(all = 0.dp),
            margin = PaddingValues(all = 32.dp),
            shape = Shape.Rectangle(CornerRadiuses.Dp(all = 32.0)),
            background = BackgroundStyles.Color(ColorStyles(ColorStyle.Solid(Color.Transparent))),
            border = null,
        ),
        state = previewEmptyState(),
        clickHandler = { },
        onStackClick = { },
    )
}

@Composable
private fun previewChildren() = listOf(
    previewTextComponentStyle(
        text = "Hello",
        backgroundColor = ColorStyles(
            light = ColorStyle.Solid(Color.Blue),
        ),
        size = Size(width = Fit, height = Fit),
        padding = Padding(top = 8.0, bottom = 8.0, leading = 8.0, trailing = 8.0),
    ),
    previewTextComponentStyle(
        text = "World",
        backgroundColor = ColorStyles(
            light = ColorStyle.Solid(Color.Blue),
        ),
        size = Size(width = Fit, height = Fit),
        padding = Padding(top = 8.0, bottom = 8.0, leading = 8.0, trailing = 8.0),
    ),
)

@Suppress("LongMethod")
private fun previewBadge(
    style: Badge.Style,
    alignment: TwoDimensionalAlignment,
    shape: Shape,
    padding: PaddingValues = PaddingValues(all = 0.dp),
    margin: PaddingValues = PaddingValues(all = 0.dp),
): BadgeStyle {
    return BadgeStyle(
        stackStyle = StackComponentStyle(
            children = listOf(
                previewTextComponentStyle(
                    text = "Badge",
                    size = Size(width = Fit, height = Fit),
                    padding = Padding(
                        top = 8.0,
                        bottom = 8.0,
                        leading = 8.0,
                        trailing = 8.0,
                    ),
                ),
            ),
            dimension = Dimension.Vertical(
                alignment = HorizontalAlignment.CENTER,
                distribution = FlexDistribution.CENTER,
            ),
            visible = true,
            size = Size(width = Fit, height = Fit),
            spacing = 0.dp,
            background = BackgroundStyles.Color(
                ColorStyles(
                    light = ColorInfo.Gradient.Linear(
                        degrees = 45f,
                        points = listOf(
                            ColorInfo.Gradient.Point(Color.Green.toArgb(), percent = 0f),
                            ColorInfo.Gradient.Point(Color.Yellow.toArgb(), percent = 80f),
                        ),
                    ).toColorStyle(),
                ),
            ),
            padding = padding,
            margin = margin,
            shape = shape,
            border = null,
            shadow = null,
            badge = null,
            scrollOrientation = null,
            rcPackage = null,
            tabIndex = null,
            countdownDate = null,
            countFrom = CountdownComponent.CountFrom.DAYS,
            overrides = emptyList(),
        ),
        style = style,
        alignment = alignment,
    )
}

/**
 * Adds top padding read at layout time: uses [state]'s header height in pixels if set (hero case
 * where the header already accounts for the status bar), otherwise falls back to [fallbackInsetPx]
 * (the safe-drawing top inset, pre-computed at composition time).
 */
private fun Modifier.headerOrInsetsTopPadding(
    state: PaywallState.Loaded.Components,
    fallbackInsetPx: Int,
): Modifier = this.layout { measurable, constraints ->
    val topPad = if (state.headerHeightPx > 0) state.headerHeightPx else fallbackInsetPx
    val placeable = measurable.measure(constraints.offset(vertical = -topPad))
    layout(placeable.width, placeable.height + topPad) {
        placeable.place(0, topPad)
    }
}
