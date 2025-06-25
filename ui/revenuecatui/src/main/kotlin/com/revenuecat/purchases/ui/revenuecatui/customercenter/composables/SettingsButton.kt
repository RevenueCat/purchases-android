@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.customercenter.composables

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

enum class SettingsButtonStyle {
    FILLED,
    OUTLINED,
}

@Composable
@JvmSynthetic
internal fun SettingsButton(
    title: String,
    modifier: Modifier = Modifier,
    style: SettingsButtonStyle = SettingsButtonStyle.FILLED,
    onClick: () -> Unit,
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
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                    ),
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
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }
}

@Preview
@Composable
private fun SettingsButton_Preview() {
    SettingsButton(
        title = "Cancel subscription",
        style = SettingsButtonStyle.FILLED,
    ) {}
}

@Preview
@Composable
private fun SettingsButtonOutlined_Preview() {
    SettingsButton(
        title = "Restore purchases",
        style = SettingsButtonStyle.OUTLINED,
    ) {}
}
