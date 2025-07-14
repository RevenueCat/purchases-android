package com.revenuecat.purchasetester.ui.components.configuration.buttons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ConfigurationButtonGroup(
    modifier: Modifier = Modifier,
    primaryButtonText: String,
    isPrimaryEnabled: Boolean,
    onPrimaryClick: () -> Unit,
    secondaryButtons: List<Pair<String, () -> Unit>> = emptyList(),
) {
    Column(modifier = modifier) {
        Button(
            onClick = onPrimaryClick,
            enabled = isPrimaryEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            Text(
                text = primaryButtonText,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimary

            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }

        if (secondaryButtons.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                secondaryButtons.forEach { (text, onClick) ->
                    OutlinedButton(
                        onClick = onClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Text(text)
                    }
                }
            }
        }
    }
}
