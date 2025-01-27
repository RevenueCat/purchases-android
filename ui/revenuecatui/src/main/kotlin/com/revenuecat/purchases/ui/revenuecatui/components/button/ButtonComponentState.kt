@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.button

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.text.intl.Locale
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toLocaleId
import com.revenuecat.purchases.ui.revenuecatui.components.style.ButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState

@JvmSynthetic
@Composable
internal fun rememberButtonComponentState(
    style: ButtonComponentStyle,
    paywallState: PaywallState.Loaded.Components,
): ButtonComponentState =
    rememberButtonComponentState(
        style = style,
        localeProvider = { paywallState.locale },
    )

@JvmSynthetic
@Composable
internal fun rememberButtonComponentState(
    style: ButtonComponentStyle,
    localeProvider: () -> Locale,
): ButtonComponentState = remember(style) {
    ButtonComponentState(
        style = style,
        localeProvider = localeProvider,
    )
}

@Stable
internal class ButtonComponentState(
    private val style: ButtonComponentStyle,
    private val localeProvider: () -> Locale,
) {

    @get:JvmSynthetic
    val action by derivedStateOf {
        val localeId = localeProvider().toLocaleId()

        style.action.toPaywallAction(localeId)
    }

    private fun ButtonComponentStyle.Action.toPaywallAction(localeId: LocaleId): PaywallAction =
        when (this) {
            is ButtonComponentStyle.Action.NavigateBack -> PaywallAction.NavigateBack
            is ButtonComponentStyle.Action.NavigateTo -> PaywallAction.NavigateTo(
                destination = destination.toPaywallDestination(localeId),
            )
            is ButtonComponentStyle.Action.PurchasePackage -> PaywallAction.PurchasePackage
            is ButtonComponentStyle.Action.RestorePurchases -> PaywallAction.RestorePurchases
        }

    private fun ButtonComponentStyle.Action.NavigateTo.Destination.toPaywallDestination(
        localeId: LocaleId,
    ): PaywallAction.NavigateTo.Destination =
        when (this) {
            is ButtonComponentStyle.Action.NavigateTo.Destination.CustomerCenter ->
                PaywallAction.NavigateTo.Destination.CustomerCenter

            is ButtonComponentStyle.Action.NavigateTo.Destination.Url -> PaywallAction.NavigateTo.Destination.Url(
                // We will use the URL for the default locale if there's no URL for the current locale.
                url = urls.run { getOrDefault(localeId, entry.value) },
                method = method,
            )
        }
}
