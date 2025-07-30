package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
    val badgeInfo = getBadgeInfo(status, localization)

    if (badgeInfo.text == null || badgeInfo.color == null) {
        return
    }

    Surface(
        shape = RoundedCornerShape(CustomerCenterConstants.Card.BADGE_CORNER_SIZE),
        color = badgeInfo.color,
        border = badgeInfo.border,
    ) {
        Text(
            text = badgeInfo.text,
            style = MaterialTheme.typography.labelLarge,
            color = badgeInfo.textColor ?: MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(
                horizontal = CustomerCenterConstants.Card.BADGE_HORIZONTAL_PADDING,
                vertical = CustomerCenterConstants.Card.BADGE_VERTICAL_PADDING,
            ),
        )
    }
}

private data class BadgeInfo(
    val text: String?,
    val color: Color?,
    val border: BorderStroke? = null,
    val textColor: Color? = null,
)

private fun getBadgeInfo(
    status: PurchaseStatus,
    localization: CustomerCenterConfigData.Localization,
): BadgeInfo {
    return when (status) {
        PurchaseStatus.EXPIRED -> BadgeInfo(
            text = localization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.EXPIRED,
            ),
            color = Color(CustomerCenterConstants.Card.COLOR_BADGE_EXPIRED),
        )
        PurchaseStatus.FREE_TRIAL_CANCELLED -> BadgeInfo(
            text = localization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.BADGE_FREE_TRIAL_CANCELLED,
            ),
            color = Color(CustomerCenterConstants.Card.COLOR_BADGE_CANCELLED),
        )
        PurchaseStatus.CANCELLED -> BadgeInfo(
            text = localization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.BADGE_CANCELLED,
            ),
            color = Color(CustomerCenterConstants.Card.COLOR_BADGE_CANCELLED),
        )
        PurchaseStatus.FREE_TRIAL -> BadgeInfo(
            text = localization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.BADGE_FREE_TRIAL,
            ),
            color = Color(CustomerCenterConstants.Card.COLOR_BADGE_FREE_TRIAL),
        )
        PurchaseStatus.LIFETIME -> BadgeInfo(
            text = localization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.BADGE_LIFETIME,
            ),
            color = Color.Transparent,
            border = BorderStroke(
                1.dp,
                Color(CustomerCenterConstants.Card.COLOR_LIFETIME_BORDER).copy(
                    alpha = CustomerCenterConstants.Card.LIFETIME_BORDER_ALPHA,
                ),
            ),
        )
        PurchaseStatus.ACTIVE -> BadgeInfo(
            text = localization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.ACTIVE,
            ),
            color = Color(CustomerCenterConstants.Card.COLOR_BADGE_ACTIVE),
        )
        PurchaseStatus.NONE -> BadgeInfo(text = null, color = null)
    }
}

private fun determinePurchaseStatus(purchaseInformation: PurchaseInformation): PurchaseStatus {
    return when {
        purchaseInformation.isExpired -> PurchaseStatus.EXPIRED
        purchaseInformation.isCancelled && purchaseInformation.isTrial -> PurchaseStatus.FREE_TRIAL_CANCELLED
        purchaseInformation.isCancelled && purchaseInformation.store != Store.PROMOTIONAL -> PurchaseStatus.CANCELLED
        purchaseInformation.isTrial -> PurchaseStatus.FREE_TRIAL
        purchaseInformation.isLifetime -> PurchaseStatus.LIFETIME
        purchaseInformation.expirationOrRenewal != null -> PurchaseStatus.ACTIVE
        else -> PurchaseStatus.NONE
    }
}

private enum class PurchaseStatus {
    EXPIRED,
    FREE_TRIAL_CANCELLED,
    CANCELLED,
    FREE_TRIAL,
    LIFETIME,
    ACTIVE,
    NONE,
}
