@file:Suppress("MagicNumber")

package com.revenuecat.sample.admob.ui.ads.verification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.revenuecat.sample.admob.ui.ads.verification.VerificationMessage.Severity

/**
 * Renders a [VerificationMessage] in a tinted card, mirroring the iOS `ResultCard`.
 *
 * The tint reflects the message [Severity]; a progress indicator is shown while the message
 * represents an in-progress (loading / verifying) state.
 */
@Composable
internal fun ResultCard(message: VerificationMessage) {
    val tint = tintFor(message.severity)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = tint.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (message.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = tint,
                )
            }
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private val InfoTint = Color(0xFF1E88E5)
private val SuccessTint = Color(0xFF2E7D32)
private val WarningTint = Color(0xFFF57C00)
private val ErrorTint = Color(0xFFC62828)

private fun tintFor(severity: Severity): Color = when (severity) {
    Severity.INFO -> InfoTint
    Severity.SUCCESS -> SuccessTint
    Severity.WARNING -> WarningTint
    Severity.ERROR -> ErrorTint
}
