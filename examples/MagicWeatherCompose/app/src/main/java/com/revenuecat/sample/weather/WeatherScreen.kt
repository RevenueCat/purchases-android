package com.revenuecat.sample.weather

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun WeatherScreen(navigateToPaywallCallback: () -> Unit) {
    Column {
        Text(text = "Weather")
        Button(onClick = navigateToPaywallCallback) {
            Text(text = "Paywall")
        }
    }
}
