package com.revenuecat.purchases.ui.revenuecatui.data.processed

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.errors.PackageConfigurationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import java.util.Locale

internal object PackageConfigurationFactory {
    @Suppress("LongParameterList")
    fun createPackageConfiguration(
        variableDataProvider: VariableDataProvider,
        packages: List<Package>,
        activelySubscribedProductIdentifiers: Set<String>,
        filter: List<String>,
        default: String?,
        localization: PaywallData.LocalizedConfiguration,
        configurationType: PackageConfigurationType,
        locale: Locale,
    ): Result<TemplateConfiguration.PackageConfiguration> {
        val packagesById = packages.associateBy { it.identifier }
        val filteredRCPackages = filter.mapNotNull {
            val rcPackage = packagesById[it]
            if (rcPackage == null) {
                Logger.d("Package with id $it not found. Ignoring.")
            }
            rcPackage
        }
        if (filteredRCPackages.isEmpty()) {
            return Result.failure(PackageConfigurationError("No packages found for ids $filter"))
        }
        val packageInfos = filteredRCPackages.map {
            TemplateConfiguration.PackageInfo(
                rcPackage = it,
                localization = ProcessedLocalizedConfiguration.create(variableDataProvider, localization, it, locale),
                currentlySubscribed = activelySubscribedProductIdentifiers.contains(it.product.id),
                discountRelativeToMostExpensivePerMonth = null, // TODO-PAYWALLS: Support discount UI
            )
        }

        val firstPackage = packageInfos.first()
        val defaultPackage = packageInfos.firstOrNull { it.rcPackage.identifier == default } ?: firstPackage

        return Result.success(
            when (configurationType) {
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
            },
        )
    }
}