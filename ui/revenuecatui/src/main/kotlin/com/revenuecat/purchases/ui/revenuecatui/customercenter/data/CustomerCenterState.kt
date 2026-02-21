package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.ui.revenuecatui.customercenter.dialogs.RestorePurchasesState
import com.revenuecat.purchases.ui.revenuecatui.customercenter.navigation.CustomerCenterDestination
import com.revenuecat.purchases.ui.revenuecatui.customercenter.navigation.CustomerCenterNavigationState
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencies

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
        @get:JvmSynthetic val mainScreenPaths: List<CustomerCenterConfigData.HelpPath> = emptyList(),
        @get:JvmSynthetic val detailScreenPaths: List<CustomerCenterConfigData.HelpPath> = emptyList(),
        @get:JvmSynthetic val restorePurchasesState: RestorePurchasesState? = null,
        @get:JvmSynthetic val noActiveScreenOffering: Offering? = null,
        @get:JvmSynthetic val navigationState: CustomerCenterNavigationState = CustomerCenterNavigationState(
            showingActivePurchasesScreen = purchases.isNotEmpty(),
            managementScreenTitle = customerCenterConfigData.getManagementScreen()?.title,
        ),
        @get:JvmSynthetic override val navigationButtonType: NavigationButtonType = NavigationButtonType.CLOSE,
        @get:JvmSynthetic val virtualCurrencies: VirtualCurrencies? = null,
        @get:JvmSynthetic val purchasesWithActions: Set<PurchaseInformation> = emptySet(),
        @get:JvmSynthetic val showSupportTicketSuccessSnackbar: Boolean = false,
        @get:JvmSynthetic val isRefreshing: Boolean = false,
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

internal data class CreateSupportTicketData(
    @get:JvmSynthetic val onSubmit:
    (email: String, description: String, onSuccess: () -> Unit, onError: () -> Unit) -> Unit,
    @get:JvmSynthetic val onCancel: () -> Unit,
    @get:JvmSynthetic val onClose: () -> Unit,
)
