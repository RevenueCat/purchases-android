package com.revenuecat.purchases.ui.revenuecatui.customercenter.navigation

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.FeedbackSurveyData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PromotionalOfferData

internal enum class CustomerCenterAnimationType {
    SLIDE_HORIZONTAL,
}

@Immutable
internal sealed class CustomerCenterDestination {
    val animationType: CustomerCenterAnimationType = CustomerCenterAnimationType.SLIDE_HORIZONTAL
    abstract val title: String?

    data class Main(
        private val showingActivePurchasesScreen: Boolean,
        private val managementScreenTitle: String?,
    ) : CustomerCenterDestination() {
        override val title: String? = if (showingActivePurchasesScreen) managementScreenTitle else null
    }

    data class FeedbackSurvey(
        val data: FeedbackSurveyData,
        override val title: String,
    ) : CustomerCenterDestination()

    data class PromotionalOffer(
        val data: PromotionalOfferData,
    ) : CustomerCenterDestination() {
        override val title: String? = null
    }
}
