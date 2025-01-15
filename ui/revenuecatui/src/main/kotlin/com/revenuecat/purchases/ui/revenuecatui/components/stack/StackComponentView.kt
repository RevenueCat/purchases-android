@file:JvmSynthetic
@file:Suppress("TooManyFunctions")

package com.revenuecat.purchases.ui.revenuecatui.components.stack

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.properties.Badge
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.CornerRadiuses
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.FontSize
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Padding.Companion.zero
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.SystemFontFamily
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toFontWeight
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toHorizontalAlignmentOrNull
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toHorizontalArrangement
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toShape
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toTextAlign
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toVerticalAlignmentOrNull
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toVerticalArrangement
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.background
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.border
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.shadow
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberBorderStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberShadowStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.BadgeStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.applyIfNotNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.validatePaywallComponentsDataOrNull
import java.net.URL
import kotlin.math.roundToInt
import androidx.compose.ui.geometry.Size as ComposeSize

@Suppress("LongMethod")
@Composable
internal fun StackComponentView(
    style: StackComponentStyle,
    state: PaywallState.Loaded.Components,
    clickHandler: suspend (PaywallAction) -> Unit,
    modifier: Modifier = Modifier,
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
                    modifier,
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
                        modifier,
                    )
                    else
                    -> StackWithShortEdgeToEdgeBadge(
                        stackState,
                        state,
                        badge.stackStyle,
                        badge.alignment,
                        clickHandler,
                        modifier,
                    )
                }
            }

            Badge.Style.Nested ->
                MainStackComponent(stackState, state, clickHandler, modifier, badge)
        }
    } else {
        MainStackComponent(stackState, state, clickHandler, modifier)
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
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        MainStackComponent(stackState, state, clickHandler)
        OverlaidBadge(badgeStack, state, alignment)
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
    modifier: Modifier = Modifier,
) {
    SubcomposeLayout(
        modifier = modifier,
    ) { constraints ->
        // Subcompose and measure the stack
        val stackMeasurable = subcompose("stack") {
            MainStackComponent(
                stackState,
                state,
                clickHandler,
            )
        }.first()
        val stackPlaceable = stackMeasurable.measure(constraints)

        // Subcompose and measure the badge
        val badgeMeasurable = subcompose("badge") {
            StackComponentView(
                // The background and border is applied to the parent container so we null it out here.
                // We make the badge use all the available width without increasing the size of the main content.
                badgeStack.copy(
                    backgroundColor = null,
                    size = Size(width = Fill, height = badgeStack.size.height),
                    border = null,
                    margin = PaddingValues(0.dp),
                ),
                state,
                clickHandler,
            )
        }.first()
        val badgePlaceable = badgeMeasurable.measure(constraints)
        val badgeHeight = badgePlaceable.height

        val backgroundCornerRadiusExtension = when (stackState.shape) {
            is Shape.Pill -> stackPlaceable.height / 2
            is Shape.Rectangle -> {
                (
                    (stackState.shape as? Shape.Rectangle)?.let { rcShape ->
                        if (topBadge) {
                            maxOf(rcShape.corners?.topLeading ?: 0.0, rcShape.corners?.topTrailing ?: 0.0)
                        } else {
                            maxOf(rcShape.corners?.bottomLeading ?: 0.0, rcShape.corners?.bottomTrailing ?: 0.0)
                        }
                    } ?: 0.0
                    ).dp.roundToPx()
            }
        }

        // Decide the final size of this layout
        val totalWidth = stackPlaceable.width
        val totalHeight = stackPlaceable.height + badgeHeight
        val backgroundHeight = badgeHeight + backgroundCornerRadiusExtension

        // Subcompose the background
        val backgroundMeasurable = subcompose("background") {
            val backgroundColorStyle = badgeStack.backgroundColor?.let { rememberColorStyle(scheme = it) }
            val borderStyle = badgeStack.border?.let { rememberBorderStyle(border = it) }
            val shadowStyle = badgeStack.shadow?.let { rememberShadowStyle(shadow = it) }
            val backgroundShape = when (badgeStack.shape) {
                is Shape.Pill -> {
                    // We have to make sure our badge uses absolute corner sizes. If it's using relative corner sizes,
                    // i.e. pill-shaped, we need to use the stack to calculate the absolute size to use.
                    (badgeStack.shape.toShape() as? RoundedCornerShape)?.let { shape ->
                        if (topBadge) {
                            RoundedCornerShape(
                                topStart = shape.topStart.makeAbsolute(stackPlaceable, LocalDensity.current),
                                topEnd = shape.topEnd.makeAbsolute(stackPlaceable, LocalDensity.current),
                                bottomEnd = CornerSize(0.dp),
                                bottomStart = CornerSize(0.dp),
                            )
                        } else {
                            RoundedCornerShape(
                                topStart = CornerSize(0.dp),
                                topEnd = CornerSize(0.dp),
                                bottomEnd = shape.bottomEnd.makeAbsolute(stackPlaceable, LocalDensity.current),
                                bottomStart = shape.bottomStart.makeAbsolute(stackPlaceable, LocalDensity.current),
                            )
                        }
                    } ?: RectangleShape
                }
                is Shape.Rectangle -> if (topBadge) {
                    Shape.Rectangle(
                        corners = CornerRadiuses(
                            topLeading = badgeStack.shape.corners?.topLeading ?: 0.0,
                            topTrailing = badgeStack.shape.corners?.topTrailing ?: 0.0,
                            bottomLeading = 0.0,
                            bottomTrailing = 0.0,
                        ),
                    ).toShape()
                } else {
                    Shape.Rectangle(
                        corners = CornerRadiuses(
                            topLeading = 0.0,
                            topTrailing = 0.0,
                            bottomLeading = badgeStack.shape.corners?.bottomLeading ?: 0.0,
                            bottomTrailing = badgeStack.shape.corners?.bottomTrailing ?: 0.0,
                        ),
                    ).toShape()
                }
            }

            val backgroundModifier = remember(badgeStack, backgroundColorStyle, borderStyle, shadowStyle) {
                Modifier
                    .padding(badgeStack.margin)
                    .applyIfNotNull(backgroundColorStyle) { background(it, backgroundShape) }
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
            Constraints.fixed(width = totalWidth, height = backgroundHeight),
        )

        // Lay it all out
        layout(totalWidth, totalHeight) {
            if (topBadge) {
                backgroundPlaceable.placeRelative(0, 0)
            } else {
                backgroundPlaceable.placeRelative(0, totalHeight - backgroundHeight)
            }

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

@Suppress("LongParameterList")
@Composable
private fun StackWithShortEdgeToEdgeBadge(
    stackState: StackComponentState,
    state: PaywallState.Loaded.Components,
    badgeStack: StackComponentStyle,
    alignment: TwoDimensionalAlignment,
    clickHandler: suspend (PaywallAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val badgeRectangleCorners = (badgeStack.shape as? Shape.Rectangle)?.corners
        ?: CornerRadiuses(all = 20.0) // For pill badge shape default to a "medium" rounded corner.
    val badgeShape = Shape.Rectangle(
        corners = when (alignment) {
            TwoDimensionalAlignment.TOP_LEADING -> CornerRadiuses(
                topLeading = 0.0,
                topTrailing = 0.0,
                bottomLeading = 0.0,
                bottomTrailing = badgeRectangleCorners.bottomTrailing,
            )
            TwoDimensionalAlignment.TOP_TRAILING -> CornerRadiuses(
                topLeading = 0.0,
                topTrailing = 0.0,
                bottomLeading = badgeRectangleCorners.bottomLeading,
                bottomTrailing = 0.0,
            )
            TwoDimensionalAlignment.BOTTOM_LEADING -> CornerRadiuses(
                topLeading = 0.0,
                topTrailing = badgeRectangleCorners.topTrailing,
                bottomLeading = 0.0,
                bottomTrailing = 0.0,
            )
            TwoDimensionalAlignment.BOTTOM_TRAILING -> CornerRadiuses(
                topLeading = badgeRectangleCorners.topLeading,
                topTrailing = 0.0,
                bottomLeading = 0.0,
                bottomTrailing = 0.0,
            )
            else -> CornerRadiuses(all = 0.0)
        },
    )
    MainStackComponent(stackState, state, clickHandler, modifier) {
        StackComponentView(
            badgeStack.copy(shape = badgeShape),
            state,
            clickHandler,
            modifier = Modifier.align(alignment.toAlignment()),
        )
    }
}

@Composable
private fun BoxScope.OverlaidBadge(
    badgeStack: StackComponentStyle,
    state: PaywallState.Loaded.Components,
    alignment: TwoDimensionalAlignment,
    modifier: Modifier = Modifier,
) {
    StackComponentView(
        badgeStack,
        state,
        {},
        modifier = modifier
            .align(alignment.toAlignment())
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.placeRelative(
                        x = 0,
                        y = getOverlaidBadgeOffsetY(placeable.height, alignment),
                    )
                }
            },
    )
}

@Suppress("LongMethod", "LongParameterList")
@Composable
private fun MainStackComponent(
    stackState: StackComponentState,
    state: PaywallState.Loaded.Components,
    clickHandler: suspend (PaywallAction) -> Unit,
    modifier: Modifier = Modifier,
    nestedBadge: BadgeStyle? = null,
    overlay: (@Composable BoxScope.() -> Unit)? = null,
) {
    val content: @Composable ((ComponentStyle) -> Modifier) -> Unit = remember(stackState.children) {
        @Composable { modifierProvider ->
            stackState.children.forEach { child ->
                ComponentView(
                    style = child,
                    state = state,
                    onClick = clickHandler,
                    modifier = modifierProvider(child),
                )
            }
        }
    }

    // Show the right container composable depending on the dimension.
    val stack: @Composable (Modifier) -> Unit = { rootModifier ->
        when (val dimension = stackState.dimension) {
            is Dimension.Horizontal -> Row(
                modifier = modifier
                    .size(stackState.size, verticalAlignment = dimension.alignment.toAlignment())
                    .then(rootModifier),
                verticalAlignment = dimension.alignment.toAlignment(),
                horizontalArrangement = dimension.distribution.toHorizontalArrangement(
                    spacing = stackState.spacing,
                ),
            ) { content { child -> if (child.size.width == Fill) Modifier.weight(1f) else Modifier } }

            is Dimension.Vertical -> Column(
                modifier = modifier
                    .size(stackState.size, horizontalAlignment = dimension.alignment.toAlignment())
                    .then(rootModifier),
                verticalArrangement = dimension.distribution.toVerticalArrangement(
                    spacing = stackState.spacing,
                ),
                horizontalAlignment = dimension.alignment.toAlignment(),
            ) { content { child -> if (child.size.height == Fill) Modifier.weight(1f) else Modifier } }

            is Dimension.ZLayer -> Box(
                modifier = modifier
                    .size(
                        size = stackState.size,
                        horizontalAlignment = dimension.alignment.toHorizontalAlignmentOrNull(),
                        verticalAlignment = dimension.alignment.toVerticalAlignmentOrNull(),
                    )
                    .then(rootModifier),
                contentAlignment = dimension.alignment.toAlignment(),
            ) { content { child -> Modifier } }
        }
    }

    val backgroundColorStyle = stackState.backgroundColor?.let { rememberColorStyle(scheme = it) }
    val borderStyle = stackState.border?.let { rememberBorderStyle(border = it) }
    val shadowStyle = stackState.shadow?.let { rememberShadowStyle(shadow = it) }
    val composeShape by remember(stackState.shape) { derivedStateOf { stackState.shape.toShape() } }

    val outerShapeModifier = remember(backgroundColorStyle, shadowStyle) {
        Modifier
            .applyIfNotNull(shadowStyle) { shadow(it, composeShape) }
            .applyIfNotNull(backgroundColorStyle) { background(it, composeShape) }
            .clip(composeShape)
    }

    val innerShapeModifier = remember(stackState, borderStyle) {
        Modifier
            .applyIfNotNull(borderStyle) { border(it, composeShape) }
            .padding(stackState.padding)
            .padding(stackState.dimension, stackState.spacing)
    }

    val commonModifier = outerShapeModifier.then(innerShapeModifier)

    if (nestedBadge == null && overlay == null) {
        stack(outerShapeModifier.then(innerShapeModifier))
    } else if (nestedBadge != null) {
        Box(modifier = modifier.then(commonModifier)) {
            stack(Modifier)
            StackComponentView(
                nestedBadge.stackStyle,
                state,
                {},
                modifier = Modifier
                    .align(nestedBadge.alignment.toAlignment()),
            )
        }
    } else if (overlay != null) {
        Box(modifier = modifier.then(outerShapeModifier)) {
            stack(innerShapeModifier)
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

/**
 * For [FlexDistribution.SPACE_AROUND] and [FlexDistribution.SPACE_EVENLY] we need to add some extra padding, as we
 * cannot use `Arrangement` to add spacing of a minimum size before or after the content. See
 * [FlexDistribution.toHorizontalArrangement] and [FlexDistribution.toVerticalArrangement] for more info.
 */
@Stable
private fun Modifier.padding(dimension: Dimension, spacing: Dp): Modifier =
    when (dimension) {
        is Dimension.Horizontal -> {
            when (dimension.distribution) {
                FlexDistribution.START,
                FlexDistribution.END,
                FlexDistribution.CENTER,
                FlexDistribution.SPACE_BETWEEN,
                -> this
                FlexDistribution.SPACE_AROUND -> this.padding(horizontal = spacing / 2)
                FlexDistribution.SPACE_EVENLY -> this.padding(horizontal = spacing)
            }
        }
        is Dimension.Vertical -> when (dimension.distribution) {
            FlexDistribution.START,
            FlexDistribution.END,
            FlexDistribution.CENTER,
            FlexDistribution.SPACE_BETWEEN,
            -> this
            FlexDistribution.SPACE_AROUND -> this.padding(vertical = spacing / 2)
            FlexDistribution.SPACE_EVENLY -> this.padding(vertical = spacing)
        }
        is Dimension.ZLayer -> this
    }

private fun getOverlaidBadgeOffsetY(height: Int, alignment: TwoDimensionalAlignment) =
    when (alignment) {
        TwoDimensionalAlignment.CENTER,
        TwoDimensionalAlignment.LEADING,
        TwoDimensionalAlignment.TRAILING,
        -> 0
        TwoDimensionalAlignment.TOP,
        TwoDimensionalAlignment.TOP_LEADING,
        TwoDimensionalAlignment.TOP_TRAILING,
        -> (-(height.toFloat() / 2)).roundToInt()
        TwoDimensionalAlignment.BOTTOM,
        TwoDimensionalAlignment.BOTTOM_LEADING,
        TwoDimensionalAlignment.BOTTOM_TRAILING,
        -> (height.toFloat() / 2).roundToInt()
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
                size = Size(width = Fit, height = Fit),
                spacing = 16.dp,
                backgroundColor = ColorScheme(
                    light = ColorInfo.Hex(Color.Red.toArgb()),
                    dark = ColorInfo.Hex(Color.Yellow.toArgb()),
                ),
                padding = PaddingValues(all = 16.dp),
                margin = PaddingValues(all = 16.dp),
                shape = Shape.Rectangle(CornerRadiuses(all = 20.0)),
                border = Border(width = 2.0, color = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb()))),
                shadow = Shadow(
                    color = ColorScheme(ColorInfo.Hex(Color.Black.toArgb())),
                    radius = 10.0,
                    x = 0.0,
                    y = 3.0,
                ),
                badge = null,
                rcPackage = null,
                overrides = null,
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
            corners = CornerRadiuses(
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
                size = Size(width = Fixed(200u), height = Fit),
                spacing = 16.dp,
                backgroundColor = ColorScheme(
                    light = ColorInfo.Hex(Color.Red.toArgb()),
                ),
                padding = PaddingValues(all = 12.dp),
                margin = PaddingValues(all = 0.dp),
                shape = Shape.Rectangle(CornerRadiuses(all = 20.0)),
                border = Border(width = 2.0, color = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb()))),
                shadow = null,
                badge = previewBadge(Badge.Style.Overlay, alignment, badgeShape),
                rcPackage = null,
                overrides = null,
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
            corners = CornerRadiuses(
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
                size = Size(width = Fixed(200u), height = Fit),
                spacing = 16.dp,
                backgroundColor = ColorScheme(
                    light = ColorInfo.Hex(Color.Red.toArgb()),
                ),
                padding = PaddingValues(all = 0.dp),
                margin = PaddingValues(all = 0.dp),
                shape = Shape.Rectangle(CornerRadiuses(all = 20.0)),
                border = Border(width = 2.0, color = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb()))),
                shadow = null,
                badge = previewBadge(Badge.Style.EdgeToEdge, alignment, badgeShape),
                rcPackage = null,
                overrides = null,
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
                size = Size(width = Fixed(200u), height = Fit),
                spacing = 16.dp,
                backgroundColor = ColorScheme(
                    light = ColorInfo.Hex(Color.Red.toArgb()),
                ),
                padding = PaddingValues(all = 0.dp),
                margin = PaddingValues(all = 0.dp),
                shape = Shape.Pill,
                border = Border(width = 2.0, color = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb()))),
                shadow = null,
                badge = previewBadge(Badge.Style.EdgeToEdge, alignment, Shape.Pill),
                rcPackage = null,
                overrides = null,
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
            corners = CornerRadiuses(
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
                size = Size(width = Fixed(200u), height = Fit),
                spacing = 16.dp,
                backgroundColor = ColorScheme(
                    light = ColorInfo.Hex(Color.Red.toArgb()),
                ),
                padding = PaddingValues(all = 0.dp),
                margin = PaddingValues(all = 0.dp),
                shape = Shape.Rectangle(CornerRadiuses(all = 20.0)),
                border = Border(width = 2.0, color = ColorScheme(light = ColorInfo.Hex(Color.Yellow.toArgb()))),
                shadow = null,
                badge = previewBadge(Badge.Style.Nested, alignment, badgeShape),
                rcPackage = null,
                overrides = null,
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
                size = Size(width = Fit, height = Fit),
                spacing = 16.dp,
                backgroundColor = ColorScheme(
                    light = ColorInfo.Hex(Color.Red.toArgb()),
                    dark = ColorInfo.Hex(Color.Yellow.toArgb()),
                ),
                padding = PaddingValues(all = 16.dp),
                margin = PaddingValues(all = 16.dp),
                shape = Shape.Rectangle(CornerRadiuses(all = 20.0)),
                border = Border(width = 2.0, color = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb()))),
                shadow = Shadow(
                    color = ColorScheme(ColorInfo.Hex(Color.Black.toArgb())),
                    radius = 30.0,
                    x = 0.0,
                    y = 5.0,
                ),
                badge = null,
                rcPackage = null,
                overrides = null,
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
                    TextComponentStyle(
                        texts = nonEmptyMapOf(LocaleId("en_US") to "Hello"),
                        color = ColorScheme(
                            light = ColorInfo.Hex(Color.Black.toArgb()),
                        ),
                        fontSize = FontSize.BODY_M,
                        fontWeight = FontWeight.REGULAR.toFontWeight(),
                        fontFamily = null,
                        textAlign = HorizontalAlignment.CENTER.toTextAlign(),
                        horizontalAlignment = HorizontalAlignment.CENTER.toAlignment(),
                        backgroundColor = ColorScheme(
                            light = ColorInfo.Hex(Color.Yellow.toArgb()),
                            dark = ColorInfo.Hex(Color.Red.toArgb()),
                        ),
                        size = Size(width = Fit, height = Fit),
                        padding = Padding(top = 8.0, bottom = 8.0, leading = 8.0, trailing = 8.0).toPaddingValues(),
                        margin = Padding(top = 0.0, bottom = 24.0, leading = 0.0, trailing = 24.0).toPaddingValues(),
                        rcPackage = null,
                        overrides = null,
                    ),
                    TextComponentStyle(
                        texts = nonEmptyMapOf(LocaleId("en_US") to "World"),
                        color = ColorScheme(
                            light = ColorInfo.Hex(Color.Black.toArgb()),
                        ),
                        fontSize = FontSize.BODY_M,
                        fontWeight = FontWeight.REGULAR.toFontWeight(),
                        fontFamily = null,
                        textAlign = HorizontalAlignment.CENTER.toTextAlign(),
                        horizontalAlignment = HorizontalAlignment.CENTER.toAlignment(),
                        backgroundColor = ColorScheme(
                            light = ColorInfo.Hex(Color.Blue.toArgb()),
                        ),
                        size = Size(width = Fit, height = Fit),
                        padding = Padding(top = 8.0, bottom = 8.0, leading = 8.0, trailing = 8.0).toPaddingValues(),
                        margin = Padding(top = 0.0, bottom = 0.0, leading = 0.0, trailing = 0.0).toPaddingValues(),
                        rcPackage = null,
                        overrides = null,
                    ),
                ),
                dimension = Dimension.ZLayer(alignment = TwoDimensionalAlignment.BOTTOM_TRAILING),
                size = Size(width = Fit, height = Fit),
                spacing = 16.dp,
                backgroundColor = ColorScheme(
                    light = ColorInfo.Hex(Color.Red.toArgb()),
                    dark = ColorInfo.Hex(Color.Yellow.toArgb()),
                ),
                padding = PaddingValues(all = 16.dp),
                margin = PaddingValues(all = 16.dp),
                shape = Shape.Rectangle(CornerRadiuses(all = 20.0)),
                border = Border(width = 2.0, color = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb()))),
                shadow = Shadow(
                    color = ColorScheme(ColorInfo.Hex(Color.Black.toArgb())),
                    radius = 20.0,
                    x = 5.0,
                    y = 5.0,
                ),
                badge = null,
                rcPackage = null,
                overrides = null,
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
                    backgroundColor = ColorScheme(ColorInfo.Hex(Color.Yellow.toArgb())),
                    size = Size(width = Fill, height = Fit),
                ),
                previewTextComponentStyle(
                    text = "World",
                    backgroundColor = ColorScheme(ColorInfo.Hex(Color.Blue.toArgb())),
                    size = Size(width = Fill, height = Fit),
                ),
            ),
            dimension = Dimension.Horizontal(
                alignment = VerticalAlignment.CENTER,
                distribution = FlexDistribution.START,
            ),
            size = Size(width = Fixed(200u), height = Fit),
            spacing = 16.dp,
            backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
            padding = PaddingValues(all = 16.dp),
            margin = PaddingValues(all = 16.dp),
            shape = Shape.Rectangle(corners = null),
            border = null,
            shadow = null,
            overrides = null,
            rcPackage = null,
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
                    backgroundColor = ColorScheme(ColorInfo.Hex(Color.Yellow.toArgb())),
                    size = Size(width = Fit, height = Fill),
                ),
                previewTextComponentStyle(
                    text = "World",
                    backgroundColor = ColorScheme(ColorInfo.Hex(Color.Blue.toArgb())),
                    size = Size(width = Fit, height = Fill),
                ),
            ),
            dimension = Dimension.Vertical(
                alignment = HorizontalAlignment.CENTER,
                distribution = FlexDistribution.START,
            ),
            size = Size(width = Fit, height = Fixed(200u)),
            spacing = 16.dp,
            backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
            padding = PaddingValues(all = 16.dp),
            margin = PaddingValues(all = 16.dp),
            shape = Shape.Rectangle(),
            border = null,
            shadow = null,
            overrides = null,
            rcPackage = null,
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
private fun StackComponentView_Preview_Distribution(
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
                    backgroundColor = ColorScheme(ColorInfo.Hex(Color.Yellow.toArgb())),
                    size = Size(width = Fit, height = Fit),
                ),
                previewTextComponentStyle(
                    text = distribution?.name ?: "null",
                    backgroundColor = ColorScheme(ColorInfo.Hex(Color.Green.toArgb())),
                    size = Size(width = Fit, height = Fit),
                ),
                previewTextComponentStyle(
                    text = "World",
                    backgroundColor = ColorScheme(ColorInfo.Hex(Color.Blue.toArgb())),
                    size = Size(width = Fit, height = Fit),
                ),
            ),
            dimension = dimension,
            // It's all set to Fit, because we want to see the `spacing` being interpreted as a minimum.
            size = Size(width = Fit, height = Fit),
            spacing = 16.dp,
            backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
            padding = PaddingValues(all = 0.dp),
            margin = PaddingValues(all = 16.dp),
            shape = Shape.Rectangle(),
            border = null,
            shadow = null,
            badge = null,
            rcPackage = null,
            overrides = null,
        ),
        state = previewEmptyState(),
        clickHandler = { },
    )
}

@Composable
private fun previewChildren() = listOf(
    TextComponentStyle(
        texts = nonEmptyMapOf(LocaleId("en_US") to "Hello"),
        color = ColorScheme(
            light = ColorInfo.Hex(Color.Black.toArgb()),
        ),
        fontSize = FontSize.BODY_M,
        fontWeight = FontWeight.REGULAR.toFontWeight(),
        fontFamily = null,
        textAlign = HorizontalAlignment.CENTER.toTextAlign(),
        horizontalAlignment = HorizontalAlignment.CENTER.toAlignment(),
        backgroundColor = ColorScheme(
            light = ColorInfo.Hex(Color.Blue.toArgb()),
        ),
        size = Size(width = Fit, height = Fit),
        padding = Padding(top = 8.0, bottom = 8.0, leading = 8.0, trailing = 8.0).toPaddingValues(),
        margin = Padding(top = 0.0, bottom = 0.0, leading = 0.0, trailing = 0.0).toPaddingValues(),
        rcPackage = null,
        overrides = null,
    ),
    TextComponentStyle(
        texts = nonEmptyMapOf(LocaleId("en_US") to "World"),
        color = ColorScheme(
            light = ColorInfo.Hex(Color.Black.toArgb()),
        ),
        fontSize = FontSize.BODY_M,
        fontWeight = FontWeight.REGULAR.toFontWeight(),
        fontFamily = null,
        textAlign = HorizontalAlignment.CENTER.toTextAlign(),
        horizontalAlignment = HorizontalAlignment.CENTER.toAlignment(),
        backgroundColor = ColorScheme(
            light = ColorInfo.Hex(Color.Blue.toArgb()),
        ),
        size = Size(width = Fit, height = Fit),
        padding = Padding(top = 8.0, bottom = 8.0, leading = 8.0, trailing = 8.0).toPaddingValues(),
        margin = Padding(top = 0.0, bottom = 0.0, leading = 0.0, trailing = 0.0).toPaddingValues(),
        rcPackage = null,
        overrides = null,
    ),
)

@Suppress("LongParameterList")
private fun previewTextComponentStyle(
    text: String,
    color: ColorScheme = ColorScheme(ColorInfo.Hex(Color.Black.toArgb())),
    fontSize: FontSize = FontSize.BODY_M,
    fontWeight: FontWeight = FontWeight.REGULAR,
    fontFamily: String? = null,
    textAlign: HorizontalAlignment = HorizontalAlignment.CENTER,
    horizontalAlignment: HorizontalAlignment = HorizontalAlignment.CENTER,
    backgroundColor: ColorScheme? = null,
    size: Size = Size(width = Fill, height = Fit),
    padding: Padding = zero,
    margin: Padding = zero,
): TextComponentStyle {
    val weight = fontWeight.toFontWeight()
    return TextComponentStyle(
        texts = nonEmptyMapOf(LocaleId("en_US") to text),
        color = color,
        fontSize = fontSize,
        fontWeight = weight,
        fontFamily = fontFamily?.let { SystemFontFamily(it, weight) },
        textAlign = textAlign.toTextAlign(),
        horizontalAlignment = horizontalAlignment.toAlignment(),
        backgroundColor = backgroundColor,
        size = size,
        padding = padding.toPaddingValues(),
        margin = margin.toPaddingValues(),
        rcPackage = null,
        overrides = null,
    )
}

private fun previewEmptyState(): PaywallState.Loaded.Components {
    val data = PaywallComponentsData(
        templateName = "template",
        assetBaseURL = URL("https://assets.pawwalls.com"),
        componentsConfig = ComponentsConfig(
            base = PaywallComponentsConfig(
                // This would normally contain at least one StackComponent, but that's not needed for previews.
                stack = StackComponent(components = emptyList()),
                background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                stickyFooter = null,
            ),
        ),
        componentsLocalizations = nonEmptyMapOf(
            LocaleId("en_US") to nonEmptyMapOf(
                LocalizationKey("text") to LocalizationData.Text("text"),
            ),
        ),
        defaultLocaleIdentifier = LocaleId("en_US"),
    )
    val offering = Offering(
        identifier = "identifier",
        serverDescription = "serverDescription",
        metadata = emptyMap(),
        availablePackages = emptyList(),
        paywallComponents = data,
    )
    val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
    return offering.toComponentsPaywallState(validated, storefrontCountryCode = null)
}

@Suppress("LongMethod")
private fun previewBadge(style: Badge.Style, alignment: TwoDimensionalAlignment, shape: Shape): BadgeStyle {
    return BadgeStyle(
        stackStyle = StackComponentStyle(
            children = listOf(
                TextComponentStyle(
                    texts = nonEmptyMapOf(LocaleId("en_US") to "Badge"),
                    color = ColorScheme(
                        light = ColorInfo.Hex(Color.Black.toArgb()),
                    ),
                    fontSize = FontSize.BODY_M,
                    fontWeight = FontWeight.REGULAR.toFontWeight(),
                    fontFamily = null,
                    textAlign = HorizontalAlignment.CENTER.toTextAlign(),
                    horizontalAlignment = HorizontalAlignment.CENTER.toAlignment(),
                    backgroundColor = null,
                    size = Size(width = Fit, height = Fit),
                    padding = Padding(
                        top = 8.0,
                        bottom = 8.0,
                        leading = 8.0,
                        trailing = 8.0,
                    ).toPaddingValues(),
                    margin = Padding(
                        top = 0.0,
                        bottom = 0.0,
                        leading = 0.0,
                        trailing = 0.0,
                    ).toPaddingValues(),
                    rcPackage = null,
                    overrides = null,
                ),
            ),
            dimension = Dimension.Vertical(
                alignment = HorizontalAlignment.CENTER,
                distribution = FlexDistribution.CENTER,
            ),
            size = Size(width = Fit, height = Fit),
            spacing = 0.dp,
            backgroundColor = ColorScheme(
                light = ColorInfo.Gradient.Linear(
                    degrees = 45f,
                    points = listOf(
                        ColorInfo.Gradient.Point(Color.Green.toArgb(), percent = 0f),
                        ColorInfo.Gradient.Point(Color.Yellow.toArgb(), percent = 80f),
                    ),
                ),
            ),
            padding = PaddingValues(all = 0.dp),
            margin = PaddingValues(all = 0.dp),
            shape = shape,
            border = null,
            shadow = null,
            badge = null,
            rcPackage = null,
            overrides = null,
        ),
        style = style,
        alignment = alignment,
    )
}
