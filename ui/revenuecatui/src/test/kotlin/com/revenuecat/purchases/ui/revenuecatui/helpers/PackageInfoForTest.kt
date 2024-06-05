package com.revenuecat.purchases.ui.revenuecatui.helpers

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.data.processed.ProcessedLocalizedConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.templates.template2
import java.util.Locale

internal fun Package.getPackageInfoForTest(
    currentlySubscribed: Boolean = false,
    paywallData: PaywallData = TestData.template2,
    features: List<PaywallData.LocalizedConfiguration.Feature> = emptyList(),
    tierId: String? = null,
): TemplateConfiguration.PackageInfo {
    val localizedConfiguration = tierId?.let { paywallData.tieredConfigForLocale(Locale.US)!![it] }
        ?: paywallData.configForLocale(Locale.US)!!

    val periodName = when(packageType) {
        PackageType.ANNUAL -> "Annual"
        PackageType.MONTHLY -> "Monthly"
        PackageType.TWO_MONTH -> "2 month"
        PackageType.THREE_MONTH -> "3 month"
        PackageType.SIX_MONTH -> "6 month"
        PackageType.WEEKLY -> "Weekly"
        PackageType.LIFETIME -> "Lifetime"
        PackageType.CUSTOM -> "Customer"
        else -> error("Unknown package type $packageType")
    }
    val callToAction = when(packageType) {
        PackageType.ANNUAL -> "Subscribe for $67.99/yr"
        PackageType.MONTHLY -> "Subscribe for $7.99/mth"
        PackageType.TWO_MONTH -> "Subscribe for $15.99/2 mths"
        PackageType.THREE_MONTH -> "Subscribe for $23.99/3 mths"
        PackageType.SIX_MONTH -> "Subscribe for $39.99/6 mths"
        PackageType.WEEKLY -> "Subscribe for $1.99/wk"
        PackageType.LIFETIME -> "Subscribe for $1,000"
        else -> error("Unknown package type $packageType")
    }
    val callToActionWithIntroOffer = when(packageType) {
        PackageType.ANNUAL -> "Start your 1 month free trial"
        PackageType.MONTHLY -> "Start your  free trial"
        PackageType.TWO_MONTH -> "Start your 1 month free trial"
        PackageType.THREE_MONTH -> "Start your 2 weeks free trial"
        PackageType.SIX_MONTH -> "Start your  free trial"
        PackageType.WEEKLY -> "Start your  free trial"
        PackageType.LIFETIME -> "Start your  free trial"
        else -> error("Unknown package type $packageType")
    }
    val offerDetails = when(packageType) {
        PackageType.ANNUAL -> "$67.99/yr ($5.67/mth)"
        PackageType.MONTHLY -> "$7.99/mth"
        PackageType.TWO_MONTH -> "$15.99/2 mths ($8.00/mth)"
        PackageType.THREE_MONTH -> "$23.99/3 mths ($8.00/mth)"
        PackageType.SIX_MONTH -> "$39.99/6 mths ($6.67/mth)"
        PackageType.WEEKLY -> "$1.99/wk ($7.96/mth)"
        PackageType.LIFETIME -> "$1,000"
        else -> error("Unknown package type $packageType")
    }
    val offerDetailsWithIntroOffer = when(packageType) {
        PackageType.ANNUAL -> "$67.99/yr ($5.67/mth) after 1 month trial"
        PackageType.MONTHLY -> "$7.99/mth after  trial"
        PackageType.TWO_MONTH -> "$15.99/2 mths ($8.00/mth) after 1 month trial"
        PackageType.THREE_MONTH -> "$23.99/3 mths ($8.00/mth) after 2 weeks trial"
        PackageType.SIX_MONTH -> "$39.99/6 mths ($6.67/mth) after  trial"
        PackageType.WEEKLY -> "$1.99/wk ($7.96/mth) after  trial"
        PackageType.LIFETIME -> "$1,000 after  trial"
        else -> error("Unknown package type $packageType")
    }
    val offerBadge = when(packageType) {
        PackageType.ANNUAL -> "29% off"
        PackageType.MONTHLY -> null
        PackageType.WEEKLY -> null
        PackageType.LIFETIME -> null
        else -> error("Unknown package type $packageType")
    }
    val discountRelativeToMostExpensivePerMonth = when(packageType) {
        PackageType.ANNUAL -> 0.29088448060075095
        PackageType.MONTHLY -> null
        PackageType.TWO_MONTH -> null
        PackageType.THREE_MONTH -> null
        PackageType.SIX_MONTH -> 0.16635397123202
        PackageType.WEEKLY -> null
        PackageType.LIFETIME -> null
        else -> error("Unknown package type $packageType")
    }
    val processedLocalization = ProcessedLocalizedConfiguration(
        title = localizedConfiguration.title,
        subtitle = localizedConfiguration.subtitle,
        callToAction = callToAction,
        callToActionWithIntroOffer = callToActionWithIntroOffer,
        callToActionWithMultipleIntroOffers = null,
        offerDetails = offerDetails,
        offerDetailsWithIntroOffer = offerDetailsWithIntroOffer,
        offerDetailsWithMultipleIntroOffers = null,
        offerName = periodName,
        offerBadge = offerBadge,
        features = features,
        tierName = localizedConfiguration.tierName,
    )
    return TemplateConfiguration.PackageInfo(
        rcPackage = this,
        localization = processedLocalization,
        currentlySubscribed = currentlySubscribed,
        discountRelativeToMostExpensivePerMonth = discountRelativeToMostExpensivePerMonth,
    )
}
