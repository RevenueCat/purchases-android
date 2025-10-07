package com.revenuecat.e2etests.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions

@Suppress("LongMethod")
@Composable
fun MainPage(modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedOffering by remember { mutableStateOf<Offering?>(null) }
    var selectedPackage by remember { mutableStateOf<Package?>(null) }
    var paywallOffering by remember { mutableStateOf<Offering?>(null) }

    // If a paywall offering is selected, show the paywall full screen
    if (paywallOffering != null) {
        BackHandler {
            paywallOffering = null
            selectedOffering = null
        }
        Paywall(
            options = PaywallOptions.Builder(dismissRequest = {
                paywallOffering = null
                selectedOffering = null
            })
                .setOffering(paywallOffering)
                .setShouldDisplayDismissButton(true)
                .build(),
        )
    } else if (selectedPackage != null) {
        // If a package is selected, show the package details screen
        PackageScreen(
            rcPackage = selectedPackage!!,
            onBackClick = { selectedPackage = null },
        )
    } else if (selectedOffering != null) {
        // If an offering is selected, show the offering details screen
        OfferingScreen(
            offering = selectedOffering!!,
            onBackClick = { selectedOffering = null },
            onShowPaywall = { offering -> paywallOffering = offering },
            onPackageClick = { pkg -> selectedPackage = pkg },
        )
    } else {
        // Otherwise show the main navigation
        Scaffold(
            modifier = modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Info, contentDescription = "Customer Info") },
                        label = { Text("Customer Info") },
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Offerings") },
                        label = { Text("Offerings") },
                    )
                }
            },
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                when (selectedTab) {
                    0 -> CustomerInfoPage()
                    1 -> OfferingsPage(
                        onOfferingClick = { offering -> selectedOffering = offering },
                    )
                }
            }
        }
    }
}
