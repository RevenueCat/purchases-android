package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun StatusBarSpacer() {
    return Spacer(
        Modifier.windowInsetsTopHeight(
            WindowInsets.statusBars,
        ),
    )
}

@Composable
fun SystemBarsSpacer() {
    return Spacer(
        Modifier.windowInsetsBottomHeight(
            WindowInsets.systemBars,
        ),
    )
}
