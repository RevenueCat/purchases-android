@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstrainedLayoutReference
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintLayoutBaseScope.HorizontalAnchor
import androidx.constraintlayout.compose.Dimension
import com.revenuecat.purchases.paywalls.components.TimelineComponent
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.ui.revenuecatui.components.iconcomponent.IconComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.overlay
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.previewEmptyState
import com.revenuecat.purchases.ui.revenuecatui.components.previewIconComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.previewImageLoader
import com.revenuecat.purchases.ui.revenuecatui.components.previewTextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.forCurrentTheme
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.IconComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TimelineComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.text.TextComponentView
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.dpOrNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.ProvidePreviewImageLoader

@Suppress("LongMethod", "CyclomaticComplexMethod", "DestructuringDeclarationWithTooManyEntries")
@Composable
internal fun TimelineComponentView(
    style: TimelineComponentStyle,
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
) {
    val timelineState = rememberUpdatedTimelineComponentState(
        style = style,
        paywallState = state,
    )

    if (!timelineState.visible) {
        return
    }

    ConstraintLayout(
        modifier = modifier
            .size(timelineState.size)
            .padding(timelineState.margin)
            .padding(timelineState.padding),
    ) {
        val itemBarriers = mutableListOf<HorizontalAnchor>()
        val iconRefs = mutableListOf<ConstrainedLayoutReference>()
        val biggestIconWidth: Dp? by remember {
            derivedStateOf {
                timelineState.items
                    .maxOfOrNull { it.icon.size.width.dpOrNull() ?: 0.dp }
            }
        }
        for (item in timelineState.items) {
            val (iconRef, titleRef, descriptionRef, itemSpacingRef) = createRefs()

            val bottomContentBarrier = createBottomBarrier(iconRef, titleRef, descriptionRef)
            val iconEndBarrier = createEndBarrier(iconRef, margin = timelineState.columnGutter.dp)

            val currentPreviousItem = itemBarriers.lastOrNull()

            iconRefs.add(iconRef)
            itemBarriers.add(createBottomBarrier(itemSpacingRef))

            Spacer(
                modifier = Modifier.height(timelineState.itemSpacing.dp)
                    .constrainAs(itemSpacingRef) {
                        top.linkTo(bottomContentBarrier)
                    },
            )

            Box(
                modifier = Modifier.constrainAs(iconRef) {
                    when (timelineState.iconAlignment) {
                        TimelineComponent.IconAlignment.Title -> {
                            top.linkTo(currentPreviousItem ?: parent.top)
                            start.linkTo(parent.start)
                        }
                        TimelineComponent.IconAlignment.TitleAndDescription -> {
                            top.linkTo(titleRef.top)
                            bottom.linkTo(descriptionRef.bottom)
                            start.linkTo(parent.start)
                        }
                    }
                    width = biggestIconWidth?.let { Dimension.value(it) } ?: Dimension.wrapContent
                },
            ) {
                IconComponentView(
                    style = item.icon,
                    state = state,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }

            TextComponentView(
                style = item.title,
                state = state,
                modifier = Modifier.constrainAs(titleRef) {
                    when (timelineState.iconAlignment) {
                        TimelineComponent.IconAlignment.Title -> {
                            centerVerticallyTo(iconRef)
                        }
                        TimelineComponent.IconAlignment.TitleAndDescription -> {
                            top.linkTo(currentPreviousItem ?: parent.top)
                        }
                    }
                    start.linkTo(iconEndBarrier)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                },
            )

            item.description?.let {
                TextComponentView(
                    style = it,
                    state = state,
                    modifier = Modifier.constrainAs(descriptionRef) {
                        top.linkTo(titleRef.bottom, margin = timelineState.textSpacing.dp)
                        start.linkTo(titleRef.start)
                        end.linkTo(titleRef.end)
                        width = Dimension.fillToConstraints
                    },
                )
            }
        }

        // Draw connectors
        for ((index, item) in timelineState.items.withIndex()) {
            val isLastItem = index == timelineState.items.size - 1
            val currentIconRef = iconRefs[index]
            val nextIconRef = iconRefs.getOrNull(index + 1)
            item.connector?.let { connector ->
                val connectorRef = createRef()
                val offsets = remember(item.icon.size, connectorRef) {
                    val itemIconHeight = item.icon.size.height as? SizeConstraint.Fixed
                    val connectorVerticalOffset = itemIconHeight?.let {
                        it.value.toInt().dp / 2
                    } ?: 0.dp
                    val connectorStartOffset = biggestIconWidth?.let {
                        (it.value.toInt() - (item.connector?.width ?: 0)).dp / 2
                    } ?: 0.dp
                    (connectorStartOffset to connectorVerticalOffset)
                }
                val nextItemIconHalfSize = (
                    timelineState.items.getOrNull(index + 1)
                        ?.icon?.size?.height?.dpOrNull() ?: 0.dp
                    ) / 2
                Box(
                    modifier = Modifier
                        .padding(item.connector?.margin ?: PaddingValues(0.dp))
                        .offset(y = offsets.second)
                        .zIndex(-1f)
                        .constrainAs(connectorRef) {
                            start.linkTo(parent.start, margin = offsets.first)
                            width = Dimension.value(item.connector?.width?.dp ?: 0.dp)
                            top.linkTo(currentIconRef.top)
                            if (isLastItem) {
                                bottom.linkTo(parent.bottom, margin = offsets.second)
                            } else {
                                // Here we know that nextIconRef won't be null because it should only be null on the
                                // last item, and in that case, we don't enter the else here.
                                bottom.linkTo(nextIconRef!!.bottom, margin = nextItemIconHalfSize + offsets.second)
                            }
                            height = Dimension.fillToConstraints
                        }
                        .overlay(connector.color.forCurrentTheme),
                )
            }
        }
    }
}

@Preview
@Composable
private fun TimelineComponentView_Align_Title_Preview() {
    ProvidePreviewImageLoader(previewImageLoader()) {
        Box(modifier = Modifier.fillMaxWidth().background(Color.White)) {
            TimelineComponentView(
                style = previewStyle(iconAlignment = TimelineComponent.IconAlignment.Title),
                state = previewEmptyState(),
            )
        }
    }
}

@Preview
@Composable
private fun TimelineComponentView_Align_TitleAndDescription_Preview() {
    ProvidePreviewImageLoader(previewImageLoader()) {
        Box(modifier = Modifier.fillMaxWidth().background(Color.White)) {
            TimelineComponentView(
                style = previewStyle(iconAlignment = TimelineComponent.IconAlignment.TitleAndDescription),
                state = previewEmptyState(),
            )
        }
    }
}

@Preview
@Composable
private fun TimelineComponentView_Connector_Margin_Preview() {
    ProvidePreviewImageLoader(previewImageLoader()) {
        Box(modifier = Modifier.fillMaxWidth().background(Color.White)) {
            TimelineComponentView(
                style = previewStyle(
                    iconAlignment = TimelineComponent.IconAlignment.TitleAndDescription,
                    items = previewItems(connectorMargins = PaddingValues(0.dp, 12.dp, 0.dp, 12.dp)),
                ),
                state = previewEmptyState(),
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun previewStyle(
    itemSpacing: Int = 24,
    textSpacing: Int = 4,
    columnGutter: Int = 8,
    iconAlignment: TimelineComponent.IconAlignment = TimelineComponent.IconAlignment.TitleAndDescription,
    visible: Boolean = true,
    size: Size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fit),
    padding: PaddingValues = PaddingValues(all = 5.dp),
    margin: PaddingValues = PaddingValues(all = 5.dp),
    items: List<TimelineComponentStyle.ItemStyle> = previewItems(),
): TimelineComponentStyle {
    return TimelineComponentStyle(
        itemSpacing = itemSpacing,
        textSpacing = textSpacing,
        columnGutter = columnGutter,
        iconAlignment = iconAlignment,
        visible = visible,
        size = size,
        padding = padding,
        margin = margin,
        items = items,
        rcPackage = null,
        tabIndex = null,
        overrides = emptyList(),
    )
}

@Suppress("LongMethod")
@Composable
private fun previewItems(
    connectorMargins: PaddingValues = PaddingValues(0.dp),
): List<TimelineComponentStyle.ItemStyle> {
    return listOf(
        TimelineComponentStyle.ItemStyle(
            title = previewTextComponentStyle(
                text = "Today",
                horizontalAlignment = HorizontalAlignment.LEADING,
                textAlign = HorizontalAlignment.LEADING,
                fontWeight = FontWeight.BOLD,
            ),
            visible = true,
            description = previewTextComponentStyle(
                text = "Description of what you get today if you subscribe with multiple lines to check wrapping",
                horizontalAlignment = HorizontalAlignment.LEADING,
                textAlign = HorizontalAlignment.LEADING,
            ),
            icon = previewIcon(),
            connector = previewConnectorStyle(margin = connectorMargins),
            rcPackage = null,
            tabIndex = null,
            overrides = emptyList(),
        ),
        TimelineComponentStyle.ItemStyle(
            title = previewTextComponentStyle(
                text = "Day X",
                horizontalAlignment = HorizontalAlignment.LEADING,
                fontWeight = FontWeight.BOLD,
                textAlign = HorizontalAlignment.LEADING,
            ),
            visible = true,
            description = previewTextComponentStyle(
                text = "We'll remind you that your trial is ending soon",
                horizontalAlignment = HorizontalAlignment.LEADING,
                textAlign = HorizontalAlignment.LEADING,
            ),
            icon = previewIcon(size = Size(width = SizeConstraint.Fixed(30u), height = SizeConstraint.Fixed(30u))),
            connector = previewConnectorStyle(margin = connectorMargins),
            rcPackage = null,
            tabIndex = null,
            overrides = emptyList(),
        ),
        TimelineComponentStyle.ItemStyle(
            title = previewTextComponentStyle(
                text = "Day Y",
                horizontalAlignment = HorizontalAlignment.LEADING,
                fontWeight = FontWeight.BOLD,
                textAlign = HorizontalAlignment.LEADING,
            ),
            visible = true,
            description = previewTextComponentStyle(
                text = "You'll be charged. You can cancel anytime before.",
                horizontalAlignment = HorizontalAlignment.LEADING,
            ),
            icon = previewIcon(color = Color.Black, backgroundColor = Color(color = 0xFF0FD483)),
            connector = previewConnectorStyle(
                margin = connectorMargins,
                color = ColorInfo.Gradient.Linear(
                    degrees = 0f,
                    listOf(
                        ColorInfo.Gradient.Point(color = Color(color = 0x000FD483).toArgb(), percent = 0f),
                        ColorInfo.Gradient.Point(color = Color(color = 0xFF0FD483).toArgb(), percent = 100f),
                    ),
                ).toColorStyle(),
            ),
            rcPackage = null,
            tabIndex = null,
            overrides = emptyList(),
        ),
    )
}

@Composable
private fun previewIcon(
    color: Color = Color.White,
    backgroundColor: Color = Color(color = 0xFF576CDB),
    size: Size = Size(width = SizeConstraint.Fixed(20u), height = SizeConstraint.Fixed(20u)),
): IconComponentStyle {
    return previewIconComponentStyle(
        size = size,
        color = ColorStyles(
            light = ColorStyle.Solid(color),
        ),
        backgroundColor = ColorStyles(
            light = ColorStyle.Solid(backgroundColor),
        ),
        paddingValues = PaddingValues(all = 4.dp),
        marginValues = PaddingValues(0.dp),
        border = null,
        shadow = null,
    )
}

private fun previewConnectorStyle(
    width: Int = 8,
    margin: PaddingValues = PaddingValues(0.dp),
    color: ColorStyle = ColorStyle.Solid(Color(color = 0xFFBCC4F1)),
): TimelineComponentStyle.ConnectorStyle {
    return TimelineComponentStyle.ConnectorStyle(
        width = width,
        margin = margin,
        color = ColorStyles(
            light = color,
        ),
    )
}
