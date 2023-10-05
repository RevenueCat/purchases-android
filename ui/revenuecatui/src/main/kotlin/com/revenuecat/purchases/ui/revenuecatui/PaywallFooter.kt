package com.revenuecat.purchases.ui.revenuecatui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ui.revenuecatui.helpers.isInPreviewMode

internal val footerRoundedBorderHeight = 12.dp

/**
 * Composable offering a minified screen Paywall UI configured from the RevenueCat dashboard.
 * You can pass in your own Composables to be displayed in the main content area.
 *
 * @param options The options to configure the PaywallView if needed.
 * @param condensed Whether to condense the composable even more.
 * @param mainContent The content to display in the main area of the PaywallView. We give a [PaddingValues] so you can
 * add that padding to your own content. This padding corresponds to the height of the rounded corner area of
 * the paywall.
 */
@Composable
fun PaywallFooter(
    options: PaywallViewOptions = PaywallViewOptions.Builder().build(),
    condensed: Boolean = false,
    mainContent: @Composable (PaddingValues) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        // This is a workaround to make the main content be able to go below the footer, so it's visible through
        // the rounded corners. We pass this padding back to the developer so they can add this padding to their content
        verticalArrangement = Arrangement.spacedBy(-footerRoundedBorderHeight),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            mainContent(PaddingValues(bottom = footerRoundedBorderHeight))
        }
        val mode = if (condensed) PaywallViewMode.FOOTER_CONDENSED else PaywallViewMode.FOOTER
        if (isInPreviewMode()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.Blue),
            )
        } else {
            PaywallView(options.changeMode(mode))
        }
    }
}

@Suppress("MagicNumber")
@Preview(showBackground = true)
@Composable
private fun PaywallFooterPreview() {
    PaywallFooter {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(it),
        ) {
            // TODO-PAYWALLS: Implement an actual sample paywall
            for (i in 1..50) {
                Text(text = "Main content $i")
            }
        }
    }
}
