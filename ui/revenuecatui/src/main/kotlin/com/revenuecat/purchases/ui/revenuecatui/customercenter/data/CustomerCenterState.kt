package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.ui.revenuecatui.customercenter.dialogs.RestorePurchasesState

internal sealed class CustomerCenterState {
    enum class ButtonType {
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
        val promotionalOfferData: PromotionalOfferData? = null,
        val title: String? = null,
        val buttonType: ButtonType = ButtonType.CLOSE,
    ) : CustomerCenterState()
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal data class FeedbackSurveyData(
    val feedbackSurvey: CustomerCenterConfigData.HelpPath.PathDetail.FeedbackSurvey,
    val onOptionSelected: (CustomerCenterConfigData.HelpPath.PathDetail.FeedbackSurvey.Option?) -> Unit,
)

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal data class PromotionalOfferData(
    val promotionalOffer: CustomerCenterConfigData.HelpPath.PathDetail.PromotionalOffer,
    val offer: SubscriptionOption,
    val onAccepted: () -> Unit,
    val onDismiss: () -> Unit,
)
