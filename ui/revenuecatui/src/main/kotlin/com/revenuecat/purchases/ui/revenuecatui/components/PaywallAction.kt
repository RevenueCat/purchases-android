package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.paywalls.components.ButtonComponent

internal sealed interface PaywallAction {
    object RestorePurchases : PaywallAction
    object NavigateBack : PaywallAction
    data class NavigateTo(val destination: ButtonComponent.Destination) : PaywallAction
}
