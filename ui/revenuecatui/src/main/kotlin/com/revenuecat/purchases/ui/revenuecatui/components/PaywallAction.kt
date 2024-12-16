package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.paywalls.components.ButtonComponent
import dev.drewhamilton.poko.Poko

internal sealed interface PaywallAction {
    object RestorePurchases : PaywallAction
    object NavigateBack : PaywallAction
    object PurchasePackage : PaywallAction

    @Poko class NavigateTo(@get:JvmSynthetic val destination: ButtonComponent.Destination) : PaywallAction
}
