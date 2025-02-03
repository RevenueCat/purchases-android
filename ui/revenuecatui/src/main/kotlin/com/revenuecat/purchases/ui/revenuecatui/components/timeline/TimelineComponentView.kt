@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstrainedLayoutReference
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintLayoutBaseScope.HorizontalAnchor
import androidx.constraintlayout.compose.Dimension
import coil.ImageLoader
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

@Suppress("LongMethod", "CyclomaticComplexMethod", "DestructuringDeclarationWithTooManyEntries")
@Composable
internal fun TimelineComponentView(
    style: TimelineComponentStyle,
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
    previewImageLoader: ImageLoader? = null,
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
        for ((index, item) in timelineState.items.withIndex()) {
            val isLastItem = index == timelineState.items.size - 1
            val (iconRef, titleRef, descriptionRef, itemSpacingRef) = createRefs()

            val bottomContentBarrier = createBottomBarrier(iconRef, titleRef, descriptionRef)

            val currentPreviousItem = itemBarriers.lastOrNull()

            iconRefs.add(iconRef)
            itemBarriers.add(createBottomBarrier(itemSpacingRef))

            Spacer(
                modifier = Modifier.height(timelineState.itemSpacing.dp)
                    .constrainAs(itemSpacingRef) {
                        top.linkTo(bottomContentBarrier)
                        if (isLastItem) {
                            bottom.linkTo(parent.bottom)
                        }
                    },
            )

            IconComponentView(
                style = item.icon,
                state = state,
                modifier = Modifier.constrainAs(iconRef) {
                    when (timelineState.iconAlignment) {
                        TimelineComponent.IconAlignment.Title -> {
                            top.linkTo(titleRef.top)
                            bottom.linkTo(titleRef.bottom)
                            start.linkTo(parent.start)
                            end.linkTo(titleRef.start)
                        }
                        TimelineComponent.IconAlignment.TitleAndDescription -> {
                            top.linkTo(titleRef.top)
                            bottom.linkTo(descriptionRef.bottom)
                            start.linkTo(parent.start)
                            end.linkTo(titleRef.start)
                        }
                    }
                },
                previewImageLoader = previewImageLoader,
            )

            TextComponentView(
                style = item.title,
                state = state,
                modifier = Modifier.constrainAs(titleRef) {
                    top.linkTo(currentPreviousItem ?: parent.top)
                    start.linkTo(iconRef.end, margin = timelineState.columnGutter.dp)
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
                    val itemIconWidth = item.icon.size.width as? SizeConstraint.Fixed
                    val itemIconHeight = item.icon.size.height as? SizeConstraint.Fixed
                    val connectorVerticalOffset = itemIconHeight?.let {
                        it.value.toInt().dp / 2
                    } ?: 0.dp
                    val connectorStartOffset = itemIconWidth?.let {
                        (it.value.toInt() - (item.connector?.width ?: 0)).dp / 2
                    } ?: 0.dp
                    (connectorStartOffset to connectorVerticalOffset)
                }
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
                                bottom.linkTo(parent.bottom)
                            } else {
                                bottom.linkTo(nextIconRef!!.top)
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
    Box {
        TimelineComponentView(
            style = previewStyle(iconAlignment = TimelineComponent.IconAlignment.Title),
            state = previewEmptyState(),
            previewImageLoader = previewImageLoader(),
        )
    }
}

@Preview
@Composable
private fun TimelineComponentView_Align_TitleAndDescription_Preview() {
    Box {
        TimelineComponentView(
            style = previewStyle(iconAlignment = TimelineComponent.IconAlignment.TitleAndDescription),
            state = previewEmptyState(),
            previewImageLoader = previewImageLoader(),
        )
    }
}

@Preview
@Composable
private fun TimelineComponentView_Connector_Margin_Preview() {
    Box {
        TimelineComponentView(
            style = previewStyle(
                iconAlignment = TimelineComponent.IconAlignment.TitleAndDescription,
                items = previewItems(connectorMargins = PaddingValues(0.dp, 12.dp, 0.dp, 12.dp)),
            ),
            state = previewEmptyState(),
            previewImageLoader = previewImageLoader(),
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun previewStyle(
    itemSpacing: Int = 24,
    textSpacing: Int = 4,
    columnGutter: Int = 8,
    iconAlignment: TimelineComponent.IconAlignment = TimelineComponent.IconAlignment.TitleAndDescription,
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
        size = size,
        padding = padding,
        margin = margin,
        items = items,
        rcPackage = null,
        tabIndex = null,
        overrides = null,
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
                fontWeight = FontWeight.BOLD,
            ),
            description = previewTextComponentStyle(
                text = "Description of what you get today if you subscribe",
                horizontalAlignment = HorizontalAlignment.LEADING,
            ),
            icon = previewIcon(),
            connector = previewConnectorStyle(margin = connectorMargins),
            rcPackage = null,
            tabIndex = null,
            overrides = null,
        ),
        TimelineComponentStyle.ItemStyle(
            title = previewTextComponentStyle(
                text = "Day X",
                horizontalAlignment = HorizontalAlignment.LEADING,
                fontWeight = FontWeight.BOLD,
                textAlign = HorizontalAlignment.LEADING,
            ),
            description = previewTextComponentStyle(
                text = "We'll remind you that your trial is ending soon",
                horizontalAlignment = HorizontalAlignment.LEADING,
            ),
            icon = previewIcon(),
            connector = previewConnectorStyle(margin = connectorMargins),
            rcPackage = null,
            tabIndex = null,
            overrides = null,
        ),
        TimelineComponentStyle.ItemStyle(
            title = previewTextComponentStyle(
                text = "Day Y",
                horizontalAlignment = HorizontalAlignment.LEADING,
                fontWeight = FontWeight.BOLD,
                textAlign = HorizontalAlignment.LEADING,
            ),
            description = previewTextComponentStyle(
                text = "You'll be charged. You can cancel anytime before.",
                horizontalAlignment = HorizontalAlignment.LEADING,
            ),
            icon = previewIcon(color = Color.Black, backgroundColor = Color(color = 0xFF0FD483)),
            connector = previewConnectorStyle(
                margin = connectorMargins,
                color = ColorInfo.Gradient.Linear(
                    degrees = 90f,
                    listOf(
                        ColorInfo.Gradient.Point(color = Color(color = 0x000FD483).toArgb(), percent = 0f),
                        ColorInfo.Gradient.Point(color = Color(color = 0xFF0FD483).toArgb(), percent = 100f),
                    ),
                ).toColorStyle(),
            ),
            rcPackage = null,
            tabIndex = null,
            overrides = null,
        ),
    )
}

@Composable
private fun previewIcon(
    color: Color = Color.White,
    backgroundColor: Color = Color(color = 0xFF576CDB),
): IconComponentStyle {
    return previewIconComponentStyle(
        size = Size(width = SizeConstraint.Fixed(20u), height = SizeConstraint.Fixed(20u)),
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
