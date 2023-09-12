package com.revenuecat.purchases.ui.revenuecatui.data.processed

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.PaywallData
import java.util.Locale

internal object PackageConfigurationFactory {
    @Suppress("LongParameterList")
    fun createPackageConfiguration(
        packages: List<Package>,
        activelySubscribedProductIdentifiers: Set<String>,
        filter: List<String>,
        default: String?,
        localization: PaywallData.LocalizedConfiguration,
        configurationType: PackageConfigurationType,
        locale: Locale,
    ): TemplateConfiguration.PackageConfiguration {
        val packagesById = packages.associateBy { it.identifier }
        val filteredRCPackages = filter.mapNotNull { packagesById[it] }
        require(filteredRCPackages.isNotEmpty()) { "No packages found for ids $filter" }
        val packageInfos = filteredRCPackages.map {
            TemplateConfiguration.PackageInfo(
                rcPackage = it,
                localization = ProcessedLocalizedConfiguration.create(localization, it, locale),
                currentlySubscribed = activelySubscribedProductIdentifiers.contains(it.product.id),
                discountRelativeToMostExpensivePerMonth = null, // TODO-PAYWALLS: Support discount UI
            )
        }

        val firstPackage = packageInfos.first()
        val defaultPackage = packageInfos.firstOrNull { it.rcPackage.identifier == default } ?: firstPackage

        return when (configurationType) {
            PackageConfigurationType.SINGLE -> {
                TemplateConfiguration.PackageConfiguration.Single(
                    packageInfo = firstPackage,
                )
            }
            PackageConfigurationType.MULTIPLE -> {
                TemplateConfiguration.PackageConfiguration.Multiple(
                    first = firstPackage,
                    default = defaultPackage,
                    all = packageInfos,
                )
            }
        }
    }
}
