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
        availablePackages: List<Package>,
        activelySubscribedProductIdentifiers: Set<String>,
        packageIdsInConfig: List<String>,
        default: String?,
        localization: PaywallData.LocalizedConfiguration,
        configurationType: PackageConfigurationType,
        locale: Locale,
    ): Result<TemplateConfiguration.PackageConfiguration> {
        val availablePackagesById = availablePackages.associateBy { it.identifier }
        val filteredRCPackages = packageIdsInConfig.mapNotNull {
            val rcPackage = availablePackagesById[it]
            if (rcPackage == null) {
                Logger.d("Package with id $it not found. Ignoring.")
            }
            rcPackage
        }.takeUnless { it.isEmpty() } ?: availablePackages

        if (filteredRCPackages.isEmpty()) {
            // This wont' happen because availablePackages won't be empty. Offerings can't have empty available packages
            return Result.failure(PackageConfigurationError("No packages found for ids $packageIdsInConfig"))
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
