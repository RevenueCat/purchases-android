package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.dialogs.RestorePurchasesState

internal sealed class CustomerCenterState(
    open val dismissCustomerCenter: Boolean = false,
) {
    enum class ButtonType {
        BACK, CLOSE
    }

    object NotLoaded : CustomerCenterState()

    data class Loading(
        override val dismissCustomerCenter: Boolean = false,
    ) : CustomerCenterState(dismissCustomerCenter)

    data class Error(
        val error: PurchasesError,
        override val dismissCustomerCenter: Boolean = false,
    ) : CustomerCenterState(dismissCustomerCenter)

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    data class Success(
        val customerCenterConfigData: CustomerCenterConfigData,
        val purchaseInformation: PurchaseInformation? = null,
        val showRestoreDialog: Boolean = false,
        val restorePurchasesState: RestorePurchasesState = RestorePurchasesState.INITIAL,
        val feedbackSurveyData: FeedbackSurveyData? = null,
        val title: String? = null,
        val buttonType: ButtonType = ButtonType.CLOSE,
        override val dismissCustomerCenter: Boolean = false,
    ) : CustomerCenterState(dismissCustomerCenter)
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal data class FeedbackSurveyData(
    val feedbackSurvey: CustomerCenterConfigData.HelpPath.PathDetail.FeedbackSurvey,
    val onOptionSelected: (CustomerCenterConfigData.HelpPath.PathDetail.FeedbackSurvey.Option?) -> Unit,
)
