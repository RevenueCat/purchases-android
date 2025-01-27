package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyMap
import dev.drewhamilton.poko.Poko

@Immutable
internal class ButtonComponentStyle(
    @get:JvmSynthetic
    val stackComponentStyle: StackComponentStyle,
    @get:JvmSynthetic
    val action: Action,
) : ComponentStyle {

    internal sealed interface Action {
        object RestorePurchases : Action
        object NavigateBack : Action
        object PurchasePackage : Action

        @Poko
        class NavigateTo(@get:JvmSynthetic val destination: Destination) : Action {
            sealed interface Destination {
                object CustomerCenter : Destination
                data class Url(
                    @get:JvmSynthetic val urls: NonEmptyMap<LocaleId, String>,
                    @get:JvmSynthetic val method: ButtonComponent.UrlMethod,
                ) : Destination
            }
        }
    }

    override val size: Size = stackComponentStyle.size
}
