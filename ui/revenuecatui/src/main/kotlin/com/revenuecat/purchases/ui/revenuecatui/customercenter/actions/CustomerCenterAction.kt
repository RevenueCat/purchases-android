package com.revenuecat.purchases.ui.revenuecatui.customercenter.actions

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal sealed class CustomerCenterAction {
    data class PathButtonPressed(val path: CustomerCenterConfigData.HelpPath) : CustomerCenterAction()
    object PerformRestore : CustomerCenterAction()
    object DismissRestoreDialog : CustomerCenterAction()
    data class ContactSupport(val email: String) : CustomerCenterAction()
    data class DisplayFeedbackSurvey(
        val feedbackSurvey: CustomerCenterConfigData.HelpPath.PathDetail.FeedbackSurvey,
        val onOptionSelected: (CustomerCenterConfigData.HelpPath.PathDetail.FeedbackSurvey.Option?) -> Unit,
    ) : CustomerCenterAction()
    object DismissFeedbackSurvey : CustomerCenterAction()
    data class DisplayPromotionalOffer(
        val promotionalOffer: CustomerCenterConfigData.HelpPath.PathDetail.PromotionalOffer,
        val onAcceptedOffer: () -> Unit,
        val onDismissedOffer: () -> Unit,
    ) : CustomerCenterAction()
    object DismissPromotionalOffer : CustomerCenterAction()
    object NavigationButtonPressed : CustomerCenterAction()
}
