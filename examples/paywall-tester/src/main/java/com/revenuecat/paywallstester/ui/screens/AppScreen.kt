package com.revenuecat.paywallstester.ui.screens

sealed class AppScreen(val route: String) {
    object Main : AppScreen("main")
    object Paywall : AppScreen("paywall")
}
