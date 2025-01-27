@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import com.revenuecat.purchases.ui.revenuecatui.extensions.applyIfNotNull
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional

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
            .conditional(timelineState.size.height is SizeConstraint.Fit) {
                height(IntrinsicSize.Min)
            }
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

@Composable
private fun TimelineItem(
    item: TimelineComponentStyle.ItemStyle,
    timelineState: TimelineComponentState,
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
    previewImageLoader: ImageLoader? = null,
) {
    Row(modifier = modifier.height(IntrinsicSize.Min)) {
        Box {
            item.connector?.let { connector ->
                val connectorOffset = (item.icon.size.height as? SizeConstraint.Fixed)?.let {
                    it.value.toInt().dp / 2
                }
                Box(
                    modifier = Modifier
                        .align(alignment = Alignment.Center)
                        .fillMaxHeight()
                        .applyIfNotNull(connectorOffset) { offset(y = it) }
                        .width(connector.width.dp)
                        .padding(connector.margin)
                        .overlay(connector.color.forCurrentTheme),
                )
            }
            IconComponentView(
                style = item.icon,
                state = state,
                previewImageLoader = previewImageLoader,
            )
        }
        Spacer(modifier = Modifier.width(timelineState.columnGutter.dp))
        Column {
            TextComponentView(
                style = item.title,
                state = state,
                modifier = Modifier.width(IntrinsicSize.Max),
            )
            item.description?.let {
                Spacer(modifier = Modifier.height(timelineState.textSpacing.dp))
                TextComponentView(
                    style = it,
                    state = state,
                    modifier = Modifier.width(IntrinsicSize.Max),
                )
            }
            Spacer(modifier = Modifier.height(timelineState.itemSpacing.dp))
        }
    }
}

@Preview
@Composable
private fun TimelineComponentView_Preview() {
    Box {
        TimelineComponentView(
            style = previewStyle(),
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
    iconAlignment: TimelineComponent.IconAlignment = TimelineComponent.IconAlignment.Title,
    size: Size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fit),
    padding: PaddingValues = PaddingValues(all = 0.dp),
    margin: PaddingValues = PaddingValues(all = 0.dp),
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

@Composable
private fun previewItems(
    connectorMargins: PaddingValues = PaddingValues(0.dp),
): List<TimelineComponentStyle.ItemStyle> {
    return listOf(
        TimelineComponentStyle.ItemStyle(
            title = previewTextComponentStyle(
                text = "Today",
                fontWeight = FontWeight.BOLD,
            ),
            description = previewTextComponentStyle(
                text = "Description of what you get today if you subscribe",
            ),
            icon = previewIcon(),
            connector = previewConnectorStyle(margin = connectorMargins),
            rcPackage = null,
            overrides = null,
        ),
        TimelineComponentStyle.ItemStyle(
            title = previewTextComponentStyle(
                text = "Day X",
                fontWeight = FontWeight.BOLD,
                textAlign = HorizontalAlignment.LEADING,
            ),
            description = previewTextComponentStyle(
                text = "We'll remind you that your trial is ending soon",
            ),
            icon = previewIcon(),
            connector = previewConnectorStyle(margin = connectorMargins),
            rcPackage = null,
            overrides = null,
        ),
        TimelineComponentStyle.ItemStyle(
            title = previewTextComponentStyle(
                text = "Day Y",
                fontWeight = FontWeight.BOLD,
                textAlign = HorizontalAlignment.LEADING,
            ),
            description = previewTextComponentStyle(
                text = "You'll be charged. You can cancel anytime before.",
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
