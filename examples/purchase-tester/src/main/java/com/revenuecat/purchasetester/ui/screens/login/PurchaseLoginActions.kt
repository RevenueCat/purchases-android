package com.revenuecat.purchasetester.ui.screens.login

sealed class PurchaseLoginActions {
    data class OnLogin(val userId: String) : PurchaseLoginActions()
    object OnAnonymousUser : PurchaseLoginActions()
    object OnResetSdk : PurchaseLoginActions()
    object OnNavigateToLogs : PurchaseLoginActions()
    object OnNavigateToProxy : PurchaseLoginActions()
    object OnErrorDismissed : PurchaseLoginActions()
}