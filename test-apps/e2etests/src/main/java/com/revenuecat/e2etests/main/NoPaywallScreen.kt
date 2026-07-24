package com.revenuecat.e2etests.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions

private const val NO_PAYWALL_OFFERING_ID = "no_paywall"

@Composable
fun NoPaywallScreen(modifier: Modifier = Modifier) {
    var offering by remember { mutableStateOf<Offering?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var showPaywall by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            offering = Purchases.sharedInstance.awaitOfferings().getOffering(NO_PAYWALL_OFFERING_ID)
            if (offering == null) error = "Offering '$NO_PAYWALL_OFFERING_ID' not found"
        } catch (e: PurchasesException) {
            error = e.message ?: "Failed to load offerings"
        }
    }

    val loaded = offering
    if (showPaywall && loaded != null) {
        Paywall(
            options = PaywallOptions.Builder(dismissRequest = { showPaywall = false })
                .setOffering(loaded)
                .build(),
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(text = "No paywall offering", style = MaterialTheme.typography.headlineMedium)
        when {
            error != null -> Text(
                text = "Error: $error",
                color = MaterialTheme.colorScheme.error,
            )
            loaded != null -> Button(onClick = { showPaywall = true }) {
                Text("Present Paywall")
            }
            else -> CircularProgressIndicator()
        }
    }
}
