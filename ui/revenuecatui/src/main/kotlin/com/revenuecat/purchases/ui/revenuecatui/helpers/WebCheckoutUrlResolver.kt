@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.helpers

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.utils.appendQueryParameter
import java.net.URI
import java.net.URISyntaxException

/**
 * Resolves the checkout URL that will be opened for [launchWebCheckout] (custom URL with optional package param,
 * otherwise package Web Purchase Link, otherwise offering Web Purchase Link).
 */
@InternalRevenueCatAPI
internal fun PaywallState.Loaded.Components.resolveWebCheckoutUrlForInteraction(
    launchWebCheckout: PaywallAction.External.LaunchWebCheckout,
): String? {
    val customUrl = launchWebCheckout.customUrl
    val behavior = launchWebCheckout.packageParamBehavior
    val (packageToUse, packageParam) = when (behavior) {
        is PaywallAction.External.LaunchWebCheckout.PackageParamBehavior.Append ->
            (behavior.rcPackage ?: selectedPackageInfo?.rcPackage) to behavior.packageParam
        is PaywallAction.External.LaunchWebCheckout.PackageParamBehavior.DoNotAppend ->
            null to null
    }
    val fromCustomUrl: String? = if (customUrl != null) {
        val uri = try {
            URI(customUrl)
        } catch (e: URISyntaxException) {
            Logger.e("Invalid custom URI: $customUrl", e)
            null
        }
        uri?.let { parsed ->
            val finalUri = if (packageParam != null && packageToUse != null) {
                parsed.appendQueryParameter(packageParam, packageToUse.identifier)
            } else {
                parsed
            }
            finalUri.toString()
        }
    } else {
        null
    }
    return fromCustomUrl ?: packageToUse?.webCheckoutURL?.toString() ?: offering.webCheckoutURL?.toString()
}

/**
 * URL string for purchase-button component interaction events.
 *
 * For web-checkout actions, this matches the exact resolved URL that will be opened.
 */
@InternalRevenueCatAPI
internal fun resolvedWebCheckoutInteractionUrl(
    paywallAction: PaywallAction,
    state: PaywallState.Loaded.Components,
): String? {
    return when (paywallAction) {
        is PaywallAction.External.LaunchWebCheckout -> {
            when (paywallAction.openMethod) {
                ButtonComponent.UrlMethod.EXTERNAL_BROWSER,
                ButtonComponent.UrlMethod.IN_APP_BROWSER,
                ButtonComponent.UrlMethod.DEEP_LINK,
                ->
                    state.resolveWebCheckoutUrlForInteraction(paywallAction)
                ButtonComponent.UrlMethod.UNKNOWN -> null
            }
        }
        else -> null
    }
}
