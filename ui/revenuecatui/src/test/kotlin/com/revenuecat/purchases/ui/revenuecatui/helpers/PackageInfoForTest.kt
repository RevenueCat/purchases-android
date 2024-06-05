package com.revenuecat.purchases.ui.revenuecatui.helpers

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.ui.revenuecatui.data.processed.ProcessedLocalizedConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.templates.template2
import java.util.Locale

internal fun Package.getPackageInfoForTest(
    currentlySubscribed: Boolean = false,
): TemplateConfiguration.PackageInfo {
    val localizedConfiguration = TestData.template2.configForLocale(Locale.US)!!
    val periodName = when(packageType) {
        PackageType.ANNUAL -> "Annual"
        PackageType.MONTHLY -> "Monthly"
        PackageType.WEEKLY -> "Weekly"
        PackageType.LIFETIME -> "Lifetime"
        else -> error("Unknown package type $packageType")
    }
    val callToAction = when(packageType) {
        PackageType.ANNUAL -> "Subscribe for $67.99/yr"
        PackageType.MONTHLY -> "Subscribe for $7.99/mth"
        PackageType.WEEKLY -> "Subscribe for $1.99/wk"
        PackageType.LIFETIME -> "Subscribe for $1,000"
        else -> error("Unknown package type $packageType")
    }
    val offerDetails = when(packageType) {
        PackageType.ANNUAL -> "$67.99/yr ($5.67/mth)"
        PackageType.MONTHLY -> "$7.99/mth"
        PackageType.WEEKLY -> "$1.99/wk ($7.96/mth)"
        PackageType.LIFETIME -> "$1,000"
        else -> error("Unknown package type $packageType")
    }
    val offerDetailsWithIntroOffer = when(packageType) {
        PackageType.ANNUAL -> "$67.99/yr ($5.67/mth) after 1 month trial"
        PackageType.MONTHLY -> "$7.99/mth after  trial"
        PackageType.WEEKLY -> "$1.99/wk ($7.96/mth) after  trial"
        PackageType.LIFETIME -> "$1,000 after  trial"
        else -> error("Unknown package type $packageType")
    }
    val discountRelativeToMostExpensivePerMonth = when(packageType) {
        PackageType.ANNUAL -> 0.29088448060075095
        PackageType.MONTHLY -> null
        PackageType.WEEKLY -> null
        PackageType.LIFETIME -> null
        else -> error("Unknown package type $packageType")
    }
    val processedLocalization = ProcessedLocalizedConfiguration(
        title = localizedConfiguration.title,
        subtitle = localizedConfiguration.subtitle,
        callToAction = callToAction,
        callToActionWithIntroOffer = null,
        callToActionWithMultipleIntroOffers = null,
        offerDetails = offerDetails,
        offerDetailsWithIntroOffer = offerDetailsWithIntroOffer,
        offerDetailsWithMultipleIntroOffers = null,
        offerName = periodName,
        features = emptyList(),
        tierName = null,
    )
    return TemplateConfiguration.PackageInfo(
        rcPackage = this,
        localization = processedLocalization,
        currentlySubscribed = currentlySubscribed,
        discountRelativeToMostExpensivePerMonth = discountRelativeToMostExpensivePerMonth,
    )
}
