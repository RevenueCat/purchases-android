@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.iconcomponent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.properties.MaskShape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.background
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.border
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.shadow
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.previewEmptyState
import com.revenuecat.purchases.ui.revenuecatui.components.previewIconComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.previewImageLoader
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.forCurrentTheme
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberBorderStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberShadowStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.IconComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.RemoteImage
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.applyIfNotNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.ProvidePreviewImageLoader

@JvmSynthetic
@Composable
internal fun IconComponentView(
    style: IconComponentStyle,
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
) {
    val iconState = rememberUpdatedIconComponentState(
        style = style,
        paywallState = state,
    )

    if (!iconState.visible) {
        return
    }

    val borderStyle = iconState.border?.let { rememberBorderStyle(border = it) }
    val shadowStyle = iconState.shadow?.let { rememberShadowStyle(shadow = it) }
    val composeShape by remember(iconState.shape) { derivedStateOf { iconState.shape ?: RectangleShape } }
    val backgroundColor = iconState.backgroundColorStyles?.forCurrentTheme
    val tintColor = iconState.tintColor?.forCurrentTheme
    val colorFilter by remember(tintColor) {
        derivedStateOf {
            // TODO Support gradient tints
            (tintColor as? ColorStyle.Solid)?.let { ColorFilter.tint(it.color) }
        }
    }
    Box(
        modifier = modifier
            .padding(iconState.margin)
            .applyIfNotNull(shadowStyle) { shadow(it, composeShape) }
            .clip(composeShape)
            .applyIfNotNull(borderStyle) { border(it, composeShape).padding(it.width) },
    ) {
        RemoteImage(
            urlString = iconState.url,
            modifier = Modifier
                .size(iconState.size)
                .applyIfNotNull(backgroundColor) { background(it, composeShape) }
                .padding(iconState.padding),
            colorFilter = colorFilter,
        )
    }
}

@Preview
@Composable
private fun IconComponentView_Preview() {
    ProvidePreviewImageLoader(previewImageLoader()) {
        Box(modifier = Modifier.background(Color.LightGray)) {
            IconComponentView(
                style = previewIconComponentStyle(
                    size = Size(
                        width = SizeConstraint.Fixed(200u),
                        height = SizeConstraint.Fixed(200u),
                    ),
                ),
                state = previewEmptyState(),
            )
        }
    }
}

@Preview
@Composable
private fun IconComponentView_Preview_FixedFixedFitMargin() {
    ProvidePreviewImageLoader(previewImageLoader()) {
        Box(modifier = Modifier.background(Color.Blue)) {
            IconComponentView(
                style = previewIconComponentStyle(
                    size = Size(width = Fixed(24u), height = Fixed(24u)),
                    shape = MaskShape.Rectangle(),
                    paddingValues = PaddingValues(all = 0.dp),
                    marginValues = PaddingValues(end = 8.dp),
                    border = null,
                    shadow = null,
                ),
                state = previewEmptyState(),
            )
        }
    }
}
