package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.ButtonComponent.Destination
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyMap
import dev.drewhamilton.poko.Poko

@Immutable
internal data class ButtonComponentStyle(
    @get:JvmSynthetic
    val stackComponentStyle: StackComponentStyle,
    @get:JvmSynthetic
    val action: Action,
) : ComponentStyle {

    internal sealed interface Action {
        object RestorePurchases : Action
        object NavigateBack : Action

        /**
         * @param rcPackage The package that will be purchased by this button. Will purchase the globally-selected
         * package if this is null.
         */
        data class PurchasePackage(val rcPackage: Package?) : Action

        @Poko
        class NavigateTo(@get:JvmSynthetic val destination: Destination) : Action {
            sealed interface Destination {
                object CustomerCenter : Destination
                data class Url(
                    @get:JvmSynthetic val urls: NonEmptyMap<LocaleId, String>,
                    @get:JvmSynthetic val method: ButtonComponent.UrlMethod,
                ) : Destination

                @Immutable
                data class Sheet(
                    @get:JvmSynthetic val id: String,
                    @get:JvmSynthetic val name: String?,
                    @get:JvmSynthetic val stack: ComponentStyle,
                    @get:JvmSynthetic val backgroundBlur: Boolean,
                    @get:JvmSynthetic val size: Size?,
                ) : Destination
            }
        }
    }

    override val visible: Boolean = stackComponentStyle.visible
    override val size: Size = stackComponentStyle.size
}
