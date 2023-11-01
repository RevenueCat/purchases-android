package com.revenuecat.purchases.ui.revenuecatui.data.processed

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.models.Price
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
        nonSubscriptionProductIdentifiers: Set<String>,
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
            // This won't happen because availablePackages won't be empty. Offerings can't have empty available packages
            return Result.failure(PackageConfigurationError("No packages found for ids $packageIdsInConfig"))
        }
        val mostExpensivePricePerMonth = mostExpensivePricePerMonth(filteredRCPackages)

        val packageInfos = filteredRCPackages.map {
            val currentlySubscribed = when (it.packageType) {
                PackageType.ANNUAL,
                PackageType.SIX_MONTH,
                PackageType.THREE_MONTH,
                PackageType.TWO_MONTH,
                PackageType.MONTHLY,
                PackageType.WEEKLY,
                -> activelySubscribedProductIdentifiers.contains(it.product.id)

                PackageType.LIFETIME, PackageType.CUSTOM,
                -> nonSubscriptionProductIdentifiers.contains(it.product.id)

                PackageType.UNKNOWN -> false
            }

            val discountRelativeToMostExpensivePerMonth = productDiscount(
                it.product.pricePerMonth(),
                mostExpensivePricePerMonth,
            )
            TemplateConfiguration.PackageInfo(
                rcPackage = it,
                localization = ProcessedLocalizedConfiguration.create(
                    variableDataProvider,
                    VariableProcessor.Context(discountRelativeToMostExpensivePerMonth),
                    localization,
                    it,
                    locale,
                ),
                currentlySubscribed = currentlySubscribed,
                discountRelativeToMostExpensivePerMonth = discountRelativeToMostExpensivePerMonth,
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

    private fun mostExpensivePricePerMonth(packages: List<Package>): Price? {
        return packages
            .mapNotNull { it.product.pricePerMonth() }
            .maxByOrNull { it.amountMicros }
    }

    private fun productDiscount(pricePerMonth: Price?, mostExpensive: Price?): Double? {
        return pricePerMonth?.amountMicros?.let { price ->
            mostExpensive?.amountMicros?.let { expensive ->
                if (price >= expensive) {
                    null
                } else {
                    (expensive - price).toDouble() / expensive.toDouble()
                }
            }
        }
    }
}
