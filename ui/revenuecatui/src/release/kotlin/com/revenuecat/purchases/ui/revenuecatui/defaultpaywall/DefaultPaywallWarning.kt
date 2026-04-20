package com.revenuecat.purchases.ui.revenuecatui.defaultpaywall

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallWarning

@Composable
internal fun DefaultPaywallWarning(
    warning: PaywallWarning,
    warningColor: Color,
    modifier: Modifier = Modifier,
) {
    warning.hashCode()
    warningColor.hashCode()
    modifier.hashCode()
    // Warning UI is only shown in debug builds.
}
