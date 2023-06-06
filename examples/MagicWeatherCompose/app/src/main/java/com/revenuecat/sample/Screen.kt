package com.revenuecat.sample

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Paywall : Screen("paywall")
}
