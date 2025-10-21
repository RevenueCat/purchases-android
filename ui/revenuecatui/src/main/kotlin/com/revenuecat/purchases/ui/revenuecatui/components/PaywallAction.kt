package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.ui.revenuecatui.components.style.ButtonComponentStyle
import dev.drewhamilton.poko.Poko

/**
 * An action that can be performed by the paywall. It is split in Internal and External, which indicates whether the
 * action can be performed by the paywall itself, or whether it needs some other component to do it.
 */
internal sealed interface PaywallAction {
    sealed interface Internal : PaywallAction {
        @Poko
        class NavigateTo(@get:JvmSynthetic val destination: Destination) : Internal {
            sealed interface Destination {
                data class Sheet(
                    @get:JvmSynthetic val sheet: ButtonComponentStyle.Action.NavigateTo.Destination.Sheet,
                ) : Destination
            }
        }
    }

    sealed interface External : PaywallAction {

        object RestorePurchases : External
        object NavigateBack : External
        data class PurchasePackage(val rcPackage: Package?) : External

        @Poko
        class NavigateTo(@get:JvmSynthetic val destination: Destination) : External {
            sealed interface Destination {
                object CustomerCenter : Destination
                data class Url(
                    @get:JvmSynthetic val url: String,
                    @get:JvmSynthetic val method: ButtonComponent.UrlMethod,
                ) : Destination
            }
        }
    }
}
