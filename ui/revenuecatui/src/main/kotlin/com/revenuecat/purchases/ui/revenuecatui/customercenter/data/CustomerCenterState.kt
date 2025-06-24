package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.ui.revenuecatui.customercenter.dialogs.RestorePurchasesState
import com.revenuecat.purchases.ui.revenuecatui.customercenter.navigation.CustomerCenterDestination
import com.revenuecat.purchases.ui.revenuecatui.customercenter.navigation.CustomerCenterNavigationState

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
        @get:JvmSynthetic val purchases: List<PurchaseInformation> = emptyList(),
        @get:JvmSynthetic val selectedPurchase: PurchaseInformation? = null,
        @get:JvmSynthetic val supportedPathsForManagementScreen: List<CustomerCenterConfigData.HelpPath>? = null,
        @get:JvmSynthetic val restorePurchasesState: RestorePurchasesState? = null,
        private val title: String? = null,
        @get:JvmSynthetic val navigationState: CustomerCenterNavigationState = CustomerCenterNavigationState(title),
        @get:JvmSynthetic override val navigationButtonType: NavigationButtonType = NavigationButtonType.CLOSE,
    ) : CustomerCenterState(navigationButtonType) {
        val currentDestination: CustomerCenterDestination
            get() = navigationState.currentDestination
    }
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
