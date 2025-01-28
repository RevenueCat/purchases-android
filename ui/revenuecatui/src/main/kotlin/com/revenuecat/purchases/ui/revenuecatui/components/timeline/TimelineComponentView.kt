@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.TimelineComponent
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.previewEmptyState
import com.revenuecat.purchases.ui.revenuecatui.components.style.TimelineComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState

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

    Box(
        modifier = modifier
            .size(timelineState.size)
            .padding(timelineState.margin)
            .padding(timelineState.padding),
    ) {
        Text("TODO")
    }
}

@Preview
@Composable
private fun TimelineComponentView_Preview() {
    Box {
        TimelineComponentView(
            style = previewStyle(),
            state = previewEmptyState(),
        )
    }
}

@Suppress("LongParameterList")
private fun previewStyle(
    itemSpacing: Int = 20,
    textSpacing: Int = 20,
    columnGutter: Int = 20,
    iconAlignment: TimelineComponent.IconAlignment = TimelineComponent.IconAlignment.Title,
    size: Size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fit),
    padding: PaddingValues = PaddingValues(all = 0.dp),
    margin: PaddingValues = PaddingValues(all = 0.dp),
): TimelineComponentStyle {
    return TimelineComponentStyle(
        itemSpacing = itemSpacing,
        textSpacing = textSpacing,
        columnGutter = columnGutter,
        iconAlignment = iconAlignment,
        size = size,
        padding = padding,
        margin = margin,
        items = emptyList(),
        rcPackage = null,
        overrides = null,
    )
}
