package com.revenuecat.paywallstester.ui.screens.main.paywalls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.paywallstester.SamplePaywalls
import com.revenuecat.paywallstester.SamplePaywallsLoader
import com.revenuecat.paywallstester.ui.theme.googleFont
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialog
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialogOptions
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.fonts.CustomFontProvider
import com.revenuecat.purchases.ui.revenuecatui.fonts.FontProvider

@Composable
fun PaywallsScreen(
    samplePaywallsLoader: SamplePaywallsLoader = SamplePaywallsLoader(),
) {
    var displayPaywallState by remember { mutableStateOf<DisplayPaywallState>(DisplayPaywallState.None) }

    LazyColumn {
        items(SamplePaywalls.SampleTemplate.values()) { template ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = template.displayableName,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(8.dp),
                )
                ButtonWithEmoji(
                    onClick = {
                        val offering = samplePaywallsLoader.offeringForTemplate(template)
                        displayPaywallState = DisplayPaywallState.FullScreen(offering)
                    },
                    emoji = "\uD83D\uDCF1",
                    label = "Full screen",
                )
                ButtonWithEmoji(
                    onClick = { },
                    emoji = "\uD83D\uDD3D",
                    label = "Footer (coming soon)",
                    enabled = false,
                )
                ButtonWithEmoji(
                    onClick = { },
                    emoji = "\uD83D\uDDDC️",
                    label = "Condenser footer (coming soon)",
                    enabled = false,
                )
                ButtonWithEmoji(
                    onClick = {
                        val offering = samplePaywallsLoader.offeringForTemplate(template)
                        displayPaywallState = DisplayPaywallState.FullScreen(offering, CustomFontProvider(googleFont))
                    },
                    emoji = "\uD83C\uDD70️",
                    label = "Custom font",
                )
            }
        }
    }
    val currentState = displayPaywallState
    if (currentState is DisplayPaywallState.FullScreen) {
        PaywallDialog(
            PaywallDialogOptions.Builder(dismissRequest = {
                displayPaywallState = DisplayPaywallState.None
            })
                .setOffering(currentState.offering)
                .setListener(object : PaywallListener {
                    override fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) {
                        displayPaywallState = DisplayPaywallState.None
                    }
                })
                .setFontProvider(currentState.fontProvider)
                .build(),
        )
    }
}

private sealed class DisplayPaywallState {
    object None : DisplayPaywallState()
    data class FullScreen(
        val offering: Offering? = null,
        val fontProvider: FontProvider? = null,
    ) : DisplayPaywallState()
}

@Composable
private fun ButtonWithEmoji(
    emoji: String,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
        onClick = onClick,
        enabled = enabled,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 10.dp,
            pressedElevation = 15.dp,
            disabledElevation = 0.dp,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(text = emoji, modifier = Modifier.padding(end = 8.dp))
            Text(text = label)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PaywallsScreenPreview() {
    PaywallsScreen()
}
