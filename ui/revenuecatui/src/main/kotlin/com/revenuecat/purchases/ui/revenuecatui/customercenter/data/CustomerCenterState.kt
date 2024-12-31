package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.dialogs.RestorePurchasesState

internal sealed class CustomerCenterState(
    open val navigationButtonType: NavigationButtonType = NavigationButtonType.CLOSE,
) {

    enum class NavigationButtonType {
        BACK, CLOSE
    }

    object NotLoaded : CustomerCenterState()

    object Loading : CustomerCenterState()

    data class Error(
        val error: PurchasesError,
    ) : CustomerCenterState()

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    data class Success(
        val customerCenterConfigData: CustomerCenterConfigData,
        val purchaseInformation: PurchaseInformation? = null,
        val showRestoreDialog: Boolean = false,
        val restorePurchasesState: RestorePurchasesState = RestorePurchasesState.INITIAL,
        val feedbackSurveyData: FeedbackSurveyData? = null,
        val title: String? = null,
        override val navigationButtonType: NavigationButtonType = NavigationButtonType.CLOSE,
    ) : CustomerCenterState(navigationButtonType)
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal data class FeedbackSurveyData(
    val feedbackSurvey: CustomerCenterConfigData.HelpPath.PathDetail.FeedbackSurvey,
    val onOptionSelected: (CustomerCenterConfigData.HelpPath.PathDetail.FeedbackSurvey.Option?) -> Unit,
)
