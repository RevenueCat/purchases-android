package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.paywalls.components.ButtonComponent
import dev.drewhamilton.poko.Poko

internal sealed interface PaywallAction {
    object RestorePurchases : PaywallAction
    object NavigateBack : PaywallAction
    object PurchasePackage : PaywallAction

    @Poko class NavigateTo(@get:JvmSynthetic val destination: Destination) : PaywallAction {
        sealed interface Destination {
            object CustomerCenter : Destination
            data class Url(
                @get:JvmSynthetic val url: String,
                @get:JvmSynthetic val method: ButtonComponent.UrlMethod,
            ) : Destination
        }
    }
}
