package com.revenuecat.purchases.ui.revenuecatui.customercenter.actions

import com.revenuecat.purchases.customercenter.CustomActionData
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PurchaseInformation

internal sealed class CustomerCenterAction {
    data class PathButtonPressed(
        val path: CustomerCenterConfigData.HelpPath,
        val purchaseInformation: PurchaseInformation?,
    ) : CustomerCenterAction()
    data class SelectPurchase(val purchase: PurchaseInformation) : CustomerCenterAction()
    object PerformRestore : CustomerCenterAction()
    object DismissRestoreDialog : CustomerCenterAction()
    data class ContactSupport(val email: String) : CustomerCenterAction()
    data class PurchasePromotionalOffer(val subscriptionOption: SubscriptionOption) : CustomerCenterAction()
    data class DismissPromotionalOffer(val originalPath: CustomerCenterConfigData.HelpPath) : CustomerCenterAction()
    data class OpenURL(val url: String) : CustomerCenterAction()
    data class CustomActionSelected(val customActionData: CustomActionData) : CustomerCenterAction()
    object NavigationButtonPressed : CustomerCenterAction()
    object ShowPaywall : CustomerCenterAction()
    object ShowVirtualCurrencyBalances : CustomerCenterAction()
    object ShowSupportTicketCreation : CustomerCenterAction()
    object DismissSupportTicketSuccessSnackbar : CustomerCenterAction()
}
