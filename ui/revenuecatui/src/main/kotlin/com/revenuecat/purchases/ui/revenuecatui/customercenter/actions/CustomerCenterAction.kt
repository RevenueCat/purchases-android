package com.revenuecat.purchases.ui.revenuecatui.customercenter.actions

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal sealed class CustomerCenterAction {
    data class PathButtonPressed(
        val path: CustomerCenterConfigData.HelpPath,
        val product: StoreProduct?,
    ) : CustomerCenterAction()
    object PerformRestore : CustomerCenterAction()
    object DismissRestoreDialog : CustomerCenterAction()
    data class ContactSupport(val email: String) : CustomerCenterAction()
    data class PurchasePromotionalOffer(val subscriptionOption: SubscriptionOption) : CustomerCenterAction()
    data class DismissPromotionalOffer(val originalPath: CustomerCenterConfigData.HelpPath) : CustomerCenterAction()
    data class OpenURL(val url: String) : CustomerCenterAction()
    object NavigationButtonPressed : CustomerCenterAction()
}
