package com.revenuecat.purchases.ui.revenuecatui.customercenter.navigation

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.FeedbackSurveyData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PromotionalOfferData

internal enum class CustomerCenterAnimationType {
    SLIDE_HORIZONTAL,
}

@Immutable
internal sealed class CustomerCenterDestination {
    abstract val animationType: CustomerCenterAnimationType
    abstract val hierarchyLevel: Int

    object Main : CustomerCenterDestination() {
        override val animationType = CustomerCenterAnimationType.SLIDE_HORIZONTAL
        override val hierarchyLevel = 0
    }

    data class FeedbackSurvey(
        val data: FeedbackSurveyData,
        val title: String,
    ) : CustomerCenterDestination() {
        override val animationType = CustomerCenterAnimationType.SLIDE_HORIZONTAL
        override val hierarchyLevel = 1
    }

    data class PromotionalOffer(
        val data: PromotionalOfferData,
    ) : CustomerCenterDestination() {
        override val animationType = CustomerCenterAnimationType.SLIDE_HORIZONTAL
        override val hierarchyLevel = 1
    }
}
