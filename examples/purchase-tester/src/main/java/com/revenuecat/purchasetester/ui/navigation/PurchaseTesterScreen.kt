package com.revenuecat.purchasetester.ui.navigation

sealed class PurchaseTesterScreen(val route: String) {
    object Configure : PurchaseTesterScreen("configure")
    object Login : PurchaseTesterScreen("login")
    object Logs : PurchaseTesterScreen("logs")
    object Proxy : PurchaseTesterScreen("proxy")
    object Overview : PurchaseTesterScreen("overview")
}
