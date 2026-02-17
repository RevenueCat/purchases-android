@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.customercenter.composables

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

internal data class SettingsButtonConfig(
    val enabled: Boolean = true,
    val loading: Boolean = false,
)

@Composable
@JvmSynthetic
internal fun SettingsButton(
    title: String,
    onClick: () -> Unit,
    config: SettingsButtonConfig,
    modifier: Modifier = Modifier,
    style: SettingsButtonStyle = SettingsButtonStyle.FILLED,
) {
    val shape = RoundedCornerShape(24.dp)
    val buttonModifier = modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = 48.dp)

    when (style) {
        SettingsButtonStyle.FILLED -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                contentPadding = ButtonDefaults.TextButtonContentPadding,
                enabled = config.enabled,
            ) {
                ButtonContent(
                    title = title,
                    loading = config.loading,
                    loadingColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
        SettingsButtonStyle.OUTLINED -> {
            OutlinedButton(
                onClick = onClick,
                modifier = buttonModifier,
                shape = shape,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
                contentPadding = ButtonDefaults.TextButtonContentPadding,
                enabled = config.enabled,
            ) {
                ButtonContent(
                    title = title,
                    loading = config.loading,
                    loadingColor = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ButtonContent(
    title: String,
    loading: Boolean,
    loadingColor: androidx.compose.ui.graphics.Color,
) {
    if (loading) {
        CircularProgressIndicator(
            color = loadingColor,
            strokeWidth = 2.dp,
            modifier = Modifier.size(20.dp),
        )
    } else {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

@Preview
@Composable
private fun SettingsButton_Preview() {
    SettingsButton(
        title = "Cancel subscription",
        style = SettingsButtonStyle.FILLED,
        onClick = { },
        config = SettingsButtonConfig(),
    )
}

@Preview
@Composable
private fun SettingsButtonOutlined_Preview() {
    SettingsButton(
        title = "Restore purchases",
        style = SettingsButtonStyle.OUTLINED,
        onClick = { },
        config = SettingsButtonConfig(),
    )
}

@Preview
@Composable
private fun SettingsButtonLoading_Preview() {
    SettingsButton(
        title = "Restore purchases",
        onClick = { },
        config = SettingsButtonConfig(loading = true),
    )
}

@Preview
@Composable
private fun SettingsButtonOutlinedLoading_Preview() {
    SettingsButton(
        title = "Restore purchases",
        onClick = { },
        style = SettingsButtonStyle.OUTLINED,
        config = SettingsButtonConfig(loading = true),
    )
}

@Preview
@Composable
private fun SettingsButtonDisabled_Preview() {
    SettingsButton(
        title = "Restore purchases",
        onClick = { },
        config = SettingsButtonConfig(enabled = false),
    )
}

internal enum class SettingsButtonStyle {
    FILLED,
    OUTLINED,
}
