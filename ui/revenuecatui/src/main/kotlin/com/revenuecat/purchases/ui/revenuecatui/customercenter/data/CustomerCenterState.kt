package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.ui.revenuecatui.customercenter.dialogs.RestorePurchasesState

internal sealed class CustomerCenterState(
    @get:JvmSynthetic open val navigationButtonType: NavigationButtonType = NavigationButtonType.CLOSE,
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
        @get:JvmSynthetic val customerCenterConfigData: CustomerCenterConfigData,
        @get:JvmSynthetic val purchaseInformation: PurchaseInformation? = null,
        @get:JvmSynthetic val showRestoreDialog: Boolean = false,
        @get:JvmSynthetic val restorePurchasesState: RestorePurchasesState = RestorePurchasesState.INITIAL,
        @get:JvmSynthetic val feedbackSurveyData: FeedbackSurveyData? = null,
        @get:JvmSynthetic val promotionalOfferData: PromotionalOfferData? = null,
        @get:JvmSynthetic val title: String? = null,
        @get:JvmSynthetic override val navigationButtonType: NavigationButtonType = NavigationButtonType.CLOSE,
    ) : CustomerCenterState(navigationButtonType)
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal data class FeedbackSurveyData(
    @get:JvmSynthetic val feedbackSurvey: CustomerCenterConfigData.HelpPath.PathDetail.FeedbackSurvey,
    @get:JvmSynthetic val onOptionSelected: (CustomerCenterConfigData.HelpPath.PathDetail.FeedbackSurvey.Option?) -> Unit,
)

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal data class PromotionalOfferData(
    @get:JvmSynthetic val configuredPromotionalOffer: CustomerCenterConfigData.HelpPath.PathDetail.PromotionalOffer,
    @get:JvmSynthetic val subscriptionOption: SubscriptionOption,
    @get:JvmSynthetic val originalPath: CustomerCenterConfigData.HelpPath,
)
