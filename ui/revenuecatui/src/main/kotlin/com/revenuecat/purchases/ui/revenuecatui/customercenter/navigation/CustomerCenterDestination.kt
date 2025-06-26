package com.revenuecat.purchases.ui.revenuecatui.customercenter.navigation

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.FeedbackSurveyData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PromotionalOfferData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PurchaseInformation

internal enum class CustomerCenterAnimationType {
    SLIDE_HORIZONTAL,
}

@Immutable
internal sealed class CustomerCenterDestination {
    val animationType: CustomerCenterAnimationType = CustomerCenterAnimationType.SLIDE_HORIZONTAL
    abstract val title: String?

    data class Main(
        override val title: String?,
    ) : CustomerCenterDestination()

    data class FeedbackSurvey(
        val data: FeedbackSurveyData,
        override val title: String,
    ) : CustomerCenterDestination()

    data class PromotionalOffer(
        val data: PromotionalOfferData,
        val purchaseInformation: PurchaseInformation?,
    ) : CustomerCenterDestination() {
        override val title: String? = null
    }

    data class SelectedPurchaseDetail(
        val purchaseInformation: PurchaseInformation,
        override val title: String,
    ) : CustomerCenterDestination()
}
