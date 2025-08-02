package com.revenuecat.purchasetester.ui.utils

sealed class AppScreen(val route: String) {
    object PurchaseConfigure : AppScreen("purchase_configure")
}
