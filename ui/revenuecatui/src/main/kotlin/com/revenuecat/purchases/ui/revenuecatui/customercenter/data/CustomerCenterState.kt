package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

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

    data class Success(
        @get:JvmSynthetic val customerCenterConfigData: CustomerCenterConfigData,
        @get:JvmSynthetic val purchaseInformation: PurchaseInformation? = null,
        @get:JvmSynthetic val supportedPathsForManagementScreen: List<CustomerCenterConfigData.HelpPath>? = null,
        @get:JvmSynthetic val restorePurchasesState: RestorePurchasesState? = null,
        @get:JvmSynthetic val feedbackSurveyData: FeedbackSurveyData? = null,
        @get:JvmSynthetic val promotionalOfferData: PromotionalOfferData? = null,
        @get:JvmSynthetic val title: String? = null,
        @get:JvmSynthetic override val navigationButtonType: NavigationButtonType = NavigationButtonType.CLOSE,
    ) : CustomerCenterState(navigationButtonType)
}

internal data class FeedbackSurveyData(
    @get:JvmSynthetic val feedbackSurvey: CustomerCenterConfigData.HelpPath.PathDetail.FeedbackSurvey,
    @get:JvmSynthetic val onAnswerSubmitted: (
        CustomerCenterConfigData.HelpPath.PathDetail.FeedbackSurvey.Option?,
    ) -> Unit,
)

internal data class PromotionalOfferData(
    @get:JvmSynthetic val configuredPromotionalOffer: CustomerCenterConfigData.HelpPath.PathDetail.PromotionalOffer,
    @get:JvmSynthetic val subscriptionOption: SubscriptionOption,
    @get:JvmSynthetic val originalPath: CustomerCenterConfigData.HelpPath,
    @get:JvmSynthetic val localizedPricingPhasesDescription: String,
)
