package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterConstants
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PurchaseInformation

@Composable
internal fun PurchaseStatusBadge(
    purchaseInformation: PurchaseInformation,
    localization: CustomerCenterConfigData.Localization,
) {
    val status = determinePurchaseStatus(purchaseInformation)
    var text: String? = null
    var color: Color? = null
    when (status) {
        PurchaseStatus.EXPIRED -> {
            text = localization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.EXPIRED,
            )
            color = Color(CustomerCenterConstants.Card.COLOR_BADGE_EXPIRED)
        }
        PurchaseStatus.FREE_TRIAL_CANCELLED -> {
            text = localization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.BADGE_FREE_TRIAL_CANCELLED,
            )
            color = Color(CustomerCenterConstants.Card.COLOR_BADGE_CANCELLED)
        }
        PurchaseStatus.CANCELLED -> {
            text = localization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.BADGE_CANCELLED,
            )
            color = Color(CustomerCenterConstants.Card.COLOR_BADGE_CANCELLED)
        }
        PurchaseStatus.FREE_TRIAL -> {
            text = localization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.BADGE_FREE_TRIAL,
            )
            color = Color(CustomerCenterConstants.Card.COLOR_BADGE_FREE_TRIAL)
        }
        PurchaseStatus.ACTIVE -> {
            text = localization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.ACTIVE,
            )
            color = Color(CustomerCenterConstants.Card.COLOR_BADGE_ACTIVE)
        }
        PurchaseStatus.NONE -> {
            text = null
            color = null
        }
    }

    if (text == null || color == null) {
        return
    }

    Surface(
        shape = RoundedCornerShape(CustomerCenterConstants.Card.BADGE_CORNER_SIZE),
        color = color,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(
                horizontal = CustomerCenterConstants.Card.BADGE_HORIZONTAL_PADDING,
                vertical = CustomerCenterConstants.Card.BADGE_VERTICAL_PADDING,
            ),
        )
    }
}

private fun determinePurchaseStatus(purchaseInformation: PurchaseInformation): PurchaseStatus {
    return when {
        purchaseInformation.isExpired -> PurchaseStatus.EXPIRED
        purchaseInformation.isCancelled && purchaseInformation.isTrial -> PurchaseStatus.FREE_TRIAL_CANCELLED
        purchaseInformation.isCancelled && purchaseInformation.store != Store.PROMOTIONAL -> PurchaseStatus.CANCELLED
        purchaseInformation.isTrial -> PurchaseStatus.FREE_TRIAL
        purchaseInformation.expirationOrRenewal != null -> PurchaseStatus.ACTIVE
        else -> PurchaseStatus.NONE
    }
}

private enum class PurchaseStatus {
    EXPIRED,
    FREE_TRIAL_CANCELLED,
    CANCELLED,
    FREE_TRIAL,
    ACTIVE,
    NONE,
}
