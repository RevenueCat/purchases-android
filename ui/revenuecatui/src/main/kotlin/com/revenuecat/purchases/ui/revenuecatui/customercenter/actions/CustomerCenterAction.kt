package com.revenuecat.purchases.ui.revenuecatui.customercenter.actions

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal sealed class CustomerCenterAction {
    data class PathButtonPressed(val path: CustomerCenterConfigData.HelpPath) : CustomerCenterAction()
    object PerformRestore : CustomerCenterAction()
    object DismissRestoreDialog : CustomerCenterAction()
    data class ContactSupport(val email: String) : CustomerCenterAction()
    object NavigationButtonPressed : CustomerCenterAction()
}
