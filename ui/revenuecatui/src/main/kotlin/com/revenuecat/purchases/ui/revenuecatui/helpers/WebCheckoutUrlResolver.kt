@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.helpers

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.utils.upsertQueryParameters
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
    launchWebCheckout.resolvedUrl?.let { return it }

    val customUrl = launchWebCheckout.customUrl
    val behavior = launchWebCheckout.paramBehavior
    val (packageToUse, packageParam) = when (behavior) {
        is PaywallAction.External.LaunchWebCheckout.ParamBehavior.Append ->
            (behavior.rcPackage ?: selectedPackageInfo?.rcPackage) to behavior.packageParam
        is PaywallAction.External.LaunchWebCheckout.ParamBehavior.DoNotAppend ->
            null to null
    }
    val fromCustomUrl = customUrl?.let {
        resolveCustomCheckoutUrl(it, behavior, packageToUse, packageParam)
    }
    return fromCustomUrl ?: packageToUse?.webCheckoutURL?.toString() ?: offering.webCheckoutURL?.toString()
}

private fun PaywallState.Loaded.Components.resolveCustomCheckoutUrl(
    customUrl: String,
    behavior: PaywallAction.External.LaunchWebCheckout.ParamBehavior,
    packageToUse: Package?,
    packageParam: String?,
): String? {
    val uri = try {
        URI(customUrl)
    } catch (e: URISyntaxException) {
        Logger.e("Invalid custom URI: $customUrl", e)
        null
    }
    return uri?.upsertQueryParameters(
        buildMap {
            put("rc_source", "app")
            if (behavior is PaywallAction.External.LaunchWebCheckout.ParamBehavior.Append) {
                behavior.appUserIdParam?.let { putIfAbsent(it, appUserID) }
                behavior.envParam?.let { putIfAbsent(it, "production") }
                if (packageParam != null && packageToUse != null) {
                    putIfAbsent(packageParam, packageToUse.identifier)
                }
            }
        },
    )?.toString()
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
