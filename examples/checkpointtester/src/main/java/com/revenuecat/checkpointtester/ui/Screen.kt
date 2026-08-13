package com.revenuecat.checkpointtester.ui

sealed class Screen(val route: String, val title: String) {
    data object UseCases : Screen("use_cases", "Use cases")
    data object ListenerLog : Screen("listener_log", "Listener log")
    data object HardPaywall : Screen("hard_paywall", "Hard paywall")
    data object SoftPaywall : Screen("soft_paywall", "Soft paywall")
    data object Onboarding : Screen("onboarding", "Onboarding")
    data object EntitlementGate : Screen("entitlement_gate", "Entitlement gate")
    data object CustomCheckpoint : Screen("custom_checkpoint", "Custom checkpoint")
}
