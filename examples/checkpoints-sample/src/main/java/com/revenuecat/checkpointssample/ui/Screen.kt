package com.revenuecat.checkpointssample.ui

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object Game : Screen("game")
}
