package com.revenuecat.purchases.ui.revenuecatui.customercenter.navigation

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.FeedbackSurveyData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PromotionalOfferData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.dialogs.RestorePurchasesState

internal enum class CustomerCenterAnimationType {
    SLIDE_HORIZONTAL,
    FADE_DIALOG,
}

@Immutable
internal sealed class CustomerCenterDestination {
    abstract val animationType: CustomerCenterAnimationType

    object Main : CustomerCenterDestination() {
        override val animationType = CustomerCenterAnimationType.SLIDE_HORIZONTAL
    }

    data class FeedbackSurvey(
        val data: FeedbackSurveyData,
        val title: String,
    ) : CustomerCenterDestination() {
        override val animationType = CustomerCenterAnimationType.SLIDE_HORIZONTAL
    }

    data class PromotionalOffer(
        val data: PromotionalOfferData,
    ) : CustomerCenterDestination() {
        override val animationType = CustomerCenterAnimationType.SLIDE_HORIZONTAL
    }

    data class RestorePurchases(
        val state: RestorePurchasesState,
    ) : CustomerCenterDestination() {
        override val animationType = CustomerCenterAnimationType.FADE_DIALOG
    }
}
