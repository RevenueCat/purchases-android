package com.revenuecat.purchases.ui.revenuecatui.defaultpaywall

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.extensions.openUriOrElse
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallWarning

@Composable
internal fun DefaultPaywallWarning(
    warning: PaywallWarning,
    warningColor: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // RevenueCat logo
        Image(
            painter = painterResource(id = R.drawable.visual_ob_create_paywall),
            contentDescription = null,
            modifier = Modifier.height(48.dp),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = warning.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Text(
                text = warning.bodyText,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        warning.helpUrl?.let { url ->
            OutlinedButton(
                onClick = {
                    context.openUriOrElse(url) { exception ->
                        val message = if (exception is ActivityNotFoundException) {
                            context.getString(R.string.no_browser_cannot_open_link)
                        } else {
                            context.getString(R.string.cannot_open_link)
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        Logger.w(message)
                    }
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = warningColor,
                ),
            ) {
                Text(
                    text = stringResource(R.string.revenuecatui_go_to_dashboard),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
