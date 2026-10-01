package com.revenuecat.purchases.ui.revenuecatui.customercenter.navigation

internal class CustomerCenterNavigator {

    var onNavigateBack: (() -> Unit)? = null

    fun navigateBack() {
        onNavigateBack?.invoke()
    }
}
