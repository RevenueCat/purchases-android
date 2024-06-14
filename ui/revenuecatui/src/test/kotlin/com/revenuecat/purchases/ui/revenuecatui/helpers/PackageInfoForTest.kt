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
    tierName: String? = null,
): TemplateConfiguration.PackageInfo {
    val localizedConfiguration = paywallData.configForLocale(Locale.US)!!
    val periodName = when(packageType) {
        PackageType.ANNUAL -> "Annual"
        PackageType.MONTHLY -> "Monthly"
        PackageType.TWO_MONTH -> "Two Months"
        PackageType.THREE_MONTH -> "Three Months"
        PackageType.SIX_MONTH -> "Six Months"
        PackageType.WEEKLY -> "Weekly"
        PackageType.LIFETIME -> "Lifetime"
        PackageType.CUSTOM -> "Customer"
        else -> error("Unknown package type $packageType")
    }
    val callToAction = when(packageType) {
        PackageType.ANNUAL -> "Subscribe for $67.99/yr"
        PackageType.MONTHLY -> "Subscribe for $7.99/mth"
        PackageType.TWO_MONTH -> "Subscribe for"
        PackageType.THREE_MONTH -> "Subscribe for"
        PackageType.SIX_MONTH -> "Subscriber for"
        PackageType.WEEKLY -> "Subscribe for $1.99/wk"
        PackageType.LIFETIME -> "Subscribe for $1,000"
        else -> error("Unknown package type $packageType")
    }
    val callToActionWithIntroOffer = when(packageType) {
        PackageType.ANNUAL -> "Start your 1 month free trial"
        PackageType.MONTHLY -> "Start your  free trial"
        PackageType.TWO_MONTH -> "Start your  free trial"
        PackageType.THREE_MONTH -> "Start your  free trial"
        PackageType.SIX_MONTH -> "Start your  free trial"
        PackageType.WEEKLY -> "Start your  free trial"
        PackageType.LIFETIME -> "Start your  free trial"
        else -> error("Unknown package type $packageType")
    }
    val offerDetails = when(packageType) {
        PackageType.ANNUAL -> "$67.99/yr ($5.67/mth)"
        PackageType.MONTHLY -> "$7.99/mth"
        PackageType.TWO_MONTH -> "/mth"
        PackageType.THREE_MONTH -> "/mth"
        PackageType.SIX_MONTH -> "/mth"
        PackageType.WEEKLY -> "$1.99/wk ($7.96/mth)"
        PackageType.LIFETIME -> "$1,000"
        else -> error("Unknown package type $packageType")
    }
    val offerDetailsWithIntroOffer = when(packageType) {
        PackageType.ANNUAL -> "$67.99/yr ($5.67/mth) after 1 month trial"
        PackageType.MONTHLY -> "$7.99/mth after  trial"
        PackageType.TWO_MONTH -> "/mth"
        PackageType.THREE_MONTH -> "/mth"
        PackageType.SIX_MONTH -> "/mth"
        PackageType.WEEKLY -> "$1.99/wk ($7.96/mth) after  trial"
        PackageType.LIFETIME -> "$1,000 after  trial"
        else -> error("Unknown package type $packageType")
    }
    val discountRelativeToMostExpensivePerMonth = when(packageType) {
        PackageType.ANNUAL -> 0.29088448060075095
        PackageType.MONTHLY -> null
        PackageType.TWO_MONTH -> null
        PackageType.THREE_MONTH -> null
        PackageType.SIX_MONTH -> null
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
        features = features,
        tierName = tierName,
    )
    return TemplateConfiguration.PackageInfo(
        rcPackage = this,
        localization = processedLocalization,
        currentlySubscribed = currentlySubscribed,
        discountRelativeToMostExpensivePerMonth = discountRelativeToMostExpensivePerMonth,
    )
}
