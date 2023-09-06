package com.revenuecat.paywallstester.ui.screens.main.paywalls

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun PaywallsScreen() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
        Text(text = "Paywalls screen")
    }
}

@Preview(showBackground = true)
@Composable
fun PaywallsScreenPreview() {
    PaywallsScreen()
}
