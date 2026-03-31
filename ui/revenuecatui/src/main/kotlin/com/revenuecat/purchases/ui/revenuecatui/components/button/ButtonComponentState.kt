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

@Stable
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

@Stable
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
            is ButtonComponentStyle.Action.NavigateBack -> PaywallAction.External.NavigateBack
            is ButtonComponentStyle.Action.NavigateTo -> when (destination) {
                is ButtonComponentStyle.Action.NavigateTo.Destination.CustomerCenter ->
                    PaywallAction.External.NavigateTo(PaywallAction.External.NavigateTo.Destination.CustomerCenter)

                is ButtonComponentStyle.Action.NavigateTo.Destination.Url ->
                    PaywallAction.External.NavigateTo(
                        PaywallAction.External.NavigateTo.Destination.Url(
                            // We will use the URL for the default locale if there's no URL for the current locale.
                            url = destination.urls.run { getOrDefault(localeId, entry.value) },
                            method = destination.method,
                        ),
                    )

                is ButtonComponentStyle.Action.NavigateTo.Destination.Sheet ->
                    PaywallAction.Internal.NavigateTo(PaywallAction.Internal.NavigateTo.Destination.Sheet(destination))
            }

            is ButtonComponentStyle.Action.PurchasePackage ->
                PaywallAction.External.PurchasePackage(
                    rcPackage = rcPackage,
                    resolvedOffer = resolvedOffer,
                )

            is ButtonComponentStyle.Action.RestorePurchases -> PaywallAction.External.RestorePurchases

            is ButtonComponentStyle.Action.WebCheckout -> PaywallAction.External.LaunchWebCheckout(
                customUrl = null,
                openMethod = openMethod,
                autoDismiss = autoDismiss,
                packageParamBehavior = PaywallAction.External.LaunchWebCheckout.PackageParamBehavior.Append(
                    rcPackage = rcPackage,
                    packageParam = null,
                ),
            )

            is ButtonComponentStyle.Action.WebProductSelection -> PaywallAction.External.LaunchWebCheckout(
                customUrl = null,
                openMethod = openMethod,
                autoDismiss = autoDismiss,
                packageParamBehavior = PaywallAction.External.LaunchWebCheckout.PackageParamBehavior.DoNotAppend,
            )

            is ButtonComponentStyle.Action.CustomWebCheckout -> {
                val urlString = urls.run { getOrDefault(localeId, entry.value) }
                PaywallAction.External.LaunchWebCheckout(
                    customUrl = urlString,
                    openMethod = openMethod,
                    autoDismiss = autoDismiss,
                    packageParamBehavior = PaywallAction.External.LaunchWebCheckout.PackageParamBehavior.Append(
                        rcPackage = rcPackage,
                        packageParam = packageParam,
                    ),
                )
            }
        }
}
