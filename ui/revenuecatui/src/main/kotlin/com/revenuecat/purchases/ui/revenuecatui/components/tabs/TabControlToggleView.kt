@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.tabs

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.previewEmptyState
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.forCurrentTheme
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabControlToggleComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.Switch
import com.revenuecat.purchases.ui.revenuecatui.composables.SwitchDefaults
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState

@Composable
internal fun TabControlToggleView(
    style: TabControlToggleComponentStyle,
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
) {
    val checked by remember { derivedStateOf { state.selectedTabIndex > 0 } }

    Switch(
        checked = checked,
        onCheckedChange = { state.update(selectedTabIndex = if (it) 1 else 0) },
        modifier = modifier
            .size(style.size),
        colors = SwitchDefaults.colors(
            // The default colors here are the same ones the Switch uses already.
            checkedThumbColor = style.thumbColorOn.forCurrentTheme,
            checkedTrackColor = style.trackColorOn.forCurrentTheme,
            uncheckedThumbColor = style.thumbColorOff.forCurrentTheme,
            uncheckedTrackColor = style.trackColorOff.forCurrentTheme,
        ),
    )
}

private class CheckedPreviewProvider : PreviewParameterProvider<Boolean> {
    override val values: Sequence<Boolean> = sequenceOf(false, true)
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TabControlToggleView_Preview(
    @PreviewParameter(CheckedPreviewProvider::class) checked: Boolean,
) {
    TabControlToggleView(
        style = TabControlToggleComponentStyle(
            defaultValue = checked,
            thumbColorOn = ColorStyles(
                light = ColorStyle.Solid(color = Color.Red),
                dark = ColorStyle.Solid(color = Color.Blue),
            ),
            thumbColorOff = ColorStyles(
                light = ColorStyle.Solid(color = Color.Blue),
                dark = ColorStyle.Solid(color = Color.Red),
            ),
            trackColorOn = ColorStyles(
                light = ColorStyle.Solid(color = Color.Yellow),
                dark = ColorStyle.Solid(color = Color.Green),
            ),
            trackColorOff = ColorStyles(
                light = ColorStyle.Solid(color = Color.Green),
                dark = ColorStyle.Solid(color = Color.Yellow),
            ),
        ),
        state = previewEmptyState(),
    )
}

@Preview
@Composable
private fun TabControlToggleView_Gradient_Preview() {
    val pointsRgb = listOf(
        ColorInfo.Gradient.Point(
            color = Color.Red.toArgb(),
            percent = 0f,
        ),
        ColorInfo.Gradient.Point(
            color = Color.Green.toArgb(),
            percent = 35f,
        ),
        ColorInfo.Gradient.Point(
            color = Color.Blue.toArgb(),
            percent = 100f,
        ),
    )

    TabControlToggleView(
        style = TabControlToggleComponentStyle(
            defaultValue = false,
            thumbColorOn = ColorStyles(
                light = ColorInfo.Gradient.Radial(
                    points = pointsRgb,
                ).toColorStyle(),
            ),
            thumbColorOff = ColorStyles(
                light = ColorInfo.Gradient.Radial(
                    points = pointsRgb,
                ).toColorStyle(),
            ),
            trackColorOn = ColorStyles(
                light = ColorInfo.Gradient.Linear(
                    degrees = 90f,
                    points = pointsRgb,
                ).toColorStyle(),
            ),
            trackColorOff = ColorStyles(
                light = ColorInfo.Gradient.Linear(
                    degrees = 90f,
                    points = pointsRgb,
                ).toColorStyle(),
            ),
        ),
        state = previewEmptyState(),
    )
}
