package com.revenuecat.paywallstester.ui.screens

sealed class AppScreen(val route: String) {
    object Main : AppScreen("main")
    object Paywall : AppScreen("paywall")
    object PaywallFooter : AppScreen("paywall_footer")

    object PaywallByPlacement : AppScreen("paywall_by_placement")
}
