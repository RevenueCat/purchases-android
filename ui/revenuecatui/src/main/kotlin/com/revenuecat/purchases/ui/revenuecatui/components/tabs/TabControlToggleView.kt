@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.tabs

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.forCurrentTheme
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabControlToggleComponentStyle

@Composable
internal fun TabControlToggleView(
    style: TabControlToggleComponentStyle,
    modifier: Modifier = Modifier,
) {
    var checked by remember { mutableStateOf(style.defaultValue) }

    Switch(
        checked = checked,
        onCheckedChange = { checked = it },
        modifier = modifier
            .size(style.size),
        colors = SwitchDefaults.colors(
            // The default colors here are the same ones the Switch uses already.
            checkedThumbColor = style.thumbColorOn.forCurrentTheme.solidOrElse(MaterialTheme.colorScheme.onPrimary),
            checkedTrackColor = style.trackColorOn.forCurrentTheme.solidOrElse(MaterialTheme.colorScheme.primary),
            uncheckedThumbColor = style.thumbColorOff.forCurrentTheme.solidOrElse(MaterialTheme.colorScheme.outline),
            uncheckedTrackColor = style.trackColorOff.forCurrentTheme
                .solidOrElse(MaterialTheme.colorScheme.surfaceContainerHighest),
        ),
    )
}

private fun ColorStyle.solidOrElse(defaultColor: Color): Color = when (this) {
    is ColorStyle.Gradient -> defaultColor
    is ColorStyle.Solid -> color
}

private class CheckedPreviewProvider : PreviewParameterProvider<Boolean> {
    override val values: Sequence<Boolean>
        get() = sequenceOf(true, false)
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
    )
}
