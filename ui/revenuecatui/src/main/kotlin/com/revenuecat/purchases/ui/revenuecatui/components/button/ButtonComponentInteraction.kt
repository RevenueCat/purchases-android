package com.revenuecat.purchases.ui.revenuecatui.components.button

import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.style.ButtonComponentStyle

/**
 * Maps non-purchase button actions to `component_value` strings.
 */
internal data class ButtonComponentInteraction(
    val value: String,
    val url: String? = null,
)

internal fun ButtonComponentStyle.Action.componentInteraction(localeUrl: String?): ButtonComponentInteraction? =
    when (this) {
        is ButtonComponentStyle.Action.RestorePurchases ->
            ButtonComponentInteraction(value = "restore_purchases")
        is ButtonComponentStyle.Action.NavigateBack ->
            ButtonComponentInteraction(value = "navigate_back")
        is ButtonComponentStyle.Action.NavigateTo -> destination.componentInteraction(localeUrl)
        is ButtonComponentStyle.Action.PurchasePackage,
        is ButtonComponentStyle.Action.WebCheckout,
        is ButtonComponentStyle.Action.WebProductSelection,
        is ButtonComponentStyle.Action.CustomWebCheckout,
        is ButtonComponentStyle.Action.WorkflowTrigger,
        -> null
    }

private fun ButtonComponentStyle.Action.NavigateTo.Destination.componentInteraction(
    localeUrl: String?,
): ButtonComponentInteraction =
    when (this) {
        is ButtonComponentStyle.Action.NavigateTo.Destination.CustomerCenter ->
            ButtonComponentInteraction(value = "navigate_to_customer_center")
        is ButtonComponentStyle.Action.NavigateTo.Destination.Url ->
            ButtonComponentInteraction(value = componentInteractionValue, url = localeUrl)
        is ButtonComponentStyle.Action.NavigateTo.Destination.Sheet ->
            ButtonComponentInteraction(value = "navigate_to_sheet")
    }

internal fun PaywallAction.workflowInteraction(): ButtonComponentInteraction? =
    if (this is PaywallAction.External.WorkflowTrigger) {
        ButtonComponentInteraction(value = "workflow")
    } else {
        null
    }

/**
 * True for purchase / web checkout actions.
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
        is ButtonComponentStyle.Action.WorkflowTrigger,
        -> false
    }
