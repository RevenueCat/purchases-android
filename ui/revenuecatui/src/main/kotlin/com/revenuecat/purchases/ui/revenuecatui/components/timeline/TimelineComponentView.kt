@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
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

    Column(
        modifier = modifier
            .size(timelineState.size)
            .padding(timelineState.margin)
            .padding(timelineState.padding),
    ) {
        style.items.map {
            TimelineItem(
                it,
                timelineState,
                state,
                previewImageLoader = previewImageLoader,
            )
        }
    }
}

@Suppress("LongMethod", "DestructuringDeclarationWithTooManyEntries")
@Composable
private fun TimelineItem(
    item: TimelineComponentStyle.ItemStyle,
    timelineState: TimelineComponentState,
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
    previewImageLoader: ImageLoader? = null,
) {
    ConstraintLayout(
        modifier = modifier,
    ) {
        val (icon, title, description, connectorRefs, itemSpacingRefs) = createRefs()

        val bottomContentBarrier = createBottomBarrier(icon, title, description)

        Spacer(
            modifier = Modifier.height(timelineState.itemSpacing.dp)
                .constrainAs(itemSpacingRefs) {
                    top.linkTo(bottomContentBarrier)
                },
        )

        item.connector?.let { connector ->
            val offsets = remember(item.icon.size, connector) {
                val itemIconWidth = item.icon.size.width as? SizeConstraint.Fixed
                val itemIconHeight = item.icon.size.height as? SizeConstraint.Fixed
                val connectorVerticalOffset = itemIconHeight?.let {
                    it.value.toInt().dp / 2
                } ?: 0.dp
                val connectorStartOffset = itemIconWidth?.let {
                    (it.value.toInt() - connector.width).dp / 2
                } ?: 0.dp
                (connectorStartOffset to connectorVerticalOffset)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(connector.margin)
                    .constrainAs(connectorRefs) {
                        start.linkTo(parent.start, margin = offsets.first)
                        width = Dimension.value(connector.width.dp)
                        height = Dimension.percent(1f)
                        top.linkTo(icon.top, margin = offsets.second)
                    }
                    .overlay(connector.color.forCurrentTheme),
            )
        }

        IconComponentView(
            style = item.icon,
            state = state,
            modifier = Modifier.constrainAs(icon) {
                when (timelineState.iconAlignment) {
                    TimelineComponent.IconAlignment.Title -> {
                        top.linkTo(title.top)
                        bottom.linkTo(title.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(title.start)
                    }
                    TimelineComponent.IconAlignment.TitleAndDescription -> {
                        top.linkTo(title.top)
                        bottom.linkTo(description.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(title.start)
                    }
                }
            },
            previewImageLoader = previewImageLoader,
        )

        TextComponentView(
            style = item.title,
            state = state,
            modifier = Modifier.constrainAs(title) {
                top.linkTo(parent.top)
                start.linkTo(icon.end, margin = timelineState.columnGutter.dp)
            },
        )

        item.description?.let {
            TextComponentView(
                style = it,
                state = state,
                modifier = Modifier.constrainAs(description) {
                    top.linkTo(title.bottom, margin = timelineState.textSpacing.dp)
                    start.linkTo(title.start)
                    end.linkTo(title.end)
                },
            )
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
