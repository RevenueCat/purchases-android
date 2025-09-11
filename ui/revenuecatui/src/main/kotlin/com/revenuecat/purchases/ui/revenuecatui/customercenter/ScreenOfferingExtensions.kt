package com.revenuecat.purchases.ui.revenuecatui.customercenter

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType

@InternalRevenueCatAPI
internal suspend fun CustomerCenterConfigData.Screen.resolveOfferingSuspend(
    purchases: PurchasesType,
): Offering? {
    val screenOffering = this.offering ?: return null

    val offerings = purchases.awaitOfferings()

    return when (screenOffering.type) {
        CustomerCenterConfigData.ScreenOffering.ScreenOfferingType.CURRENT -> {
            offerings.current
        }
        CustomerCenterConfigData.ScreenOffering.ScreenOfferingType.SPECIFIC -> {
            val offeringId = screenOffering.offeringId
            if (offeringId != null) {
                offerings.all[offeringId]
            } else {
                null
            }
        }
    }
}

@InternalRevenueCatAPI
internal fun CustomerCenterConfigData.Screen.resolveButtonText(
    localization: CustomerCenterConfigData.Localization,
): String {
    return offering?.buttonText
        ?: localization.commonLocalizedString(
            CustomerCenterConfigData.Localization.CommonLocalizedString.BUY_SUBSCRIPTION,
        )
}
