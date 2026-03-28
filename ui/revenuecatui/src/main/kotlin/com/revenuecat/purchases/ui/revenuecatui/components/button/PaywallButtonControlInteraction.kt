@file:OptIn(com.revenuecat.purchases.InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.components.button

import com.revenuecat.purchases.ui.revenuecatui.components.style.ButtonComponentStyle

/**
 * Maps non-purchase button actions to `component_value` strings (parity with iOS `ButtonComponentViewModel.Action`).
 */
internal data class ButtonControlInteraction(
    val value: String,
    val url: String? = null,
)

@Suppress("CyclomaticComplexMethod")
internal fun ButtonComponentStyle.Action.controlInteraction(localeUrl: String?): ButtonControlInteraction? =
    when (this) {
        is ButtonComponentStyle.Action.RestorePurchases ->
            ButtonControlInteraction(value = "restore_purchases")
        is ButtonComponentStyle.Action.NavigateBack ->
            ButtonControlInteraction(value = "navigate_back")
        is ButtonComponentStyle.Action.NavigateTo -> destination.controlInteraction(localeUrl)
        is ButtonComponentStyle.Action.PurchasePackage,
        is ButtonComponentStyle.Action.WebCheckout,
        is ButtonComponentStyle.Action.WebProductSelection,
        is ButtonComponentStyle.Action.CustomWebCheckout,
        -> null
    }

@Suppress("CyclomaticComplexMethod")
private fun ButtonComponentStyle.Action.NavigateTo.Destination.controlInteraction(
    localeUrl: String?,
): ButtonControlInteraction =
    when (this) {
        is ButtonComponentStyle.Action.NavigateTo.Destination.CustomerCenter ->
            ButtonControlInteraction(value = "navigate_to_customer_center")
        is ButtonComponentStyle.Action.NavigateTo.Destination.Url ->
            ButtonControlInteraction(value = controlInteractionValue, url = localeUrl)
        is ButtonComponentStyle.Action.NavigateTo.Destination.Sheet ->
            ButtonControlInteraction(value = "navigate_to_sheet")
    }

/**
 * True for purchase / web checkout actions — these must not emit `paywall_control_interaction`
 * (parity with iOS `PurchaseButtonComponentView`).
 */
internal fun ButtonComponentStyle.Action.isPurchaseRelated(): Boolean =
    when (this) {
        is ButtonComponentStyle.Action.PurchasePackage,
        is ButtonComponentStyle.Action.WebCheckout,
        is ButtonComponentStyle.Action.WebProductSelection,
        is ButtonComponentStyle.Action.CustomWebCheckout,
        -> true
        is ButtonComponentStyle.Action.RestorePurchases,
        is ButtonComponentStyle.Action.NavigateBack,
        is ButtonComponentStyle.Action.NavigateTo,
        -> false
    }
