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
        paywallData: PaywallData,
    ): Result<TemplateConfiguration.PackageConfiguration> {
        val availablePackagesById = availablePackages.associateBy { it.identifier }
        val filteredRCPackages = packageIdsInConfig.mapNotNull {
            val rcPackage = availablePackagesById[it]
            if (rcPackage == null) {
                Logger.d("Package with id $it not found. Ignoring.")
            }
            rcPackage
        }.takeUnless {
            it.isEmpty()
        } ?: availablePackages // TODO: This is actually a little confusing when it comes to multi-tier paywalls

        if (filteredRCPackages.isEmpty()) {
            // This won't happen because availablePackages won't be empty. Offerings can't have empty available packages
            return Result.failure(PackageConfigurationError("No packages found for ids $packageIdsInConfig"))
        }
        val mostExpensivePricePerMonth = mostExpensivePricePerMonth(filteredRCPackages)

        val packageInfos = filteredRCPackages.map {
            val currentlySubscribed = it.currentlySubscribed(
                activelySubscribedProductIdentifiers,
                nonSubscriptionProductIdentifiers,
            )

            val discountRelativeToMostExpensivePerMonth = productDiscount(
                it.product.pricePerMonth(),
                mostExpensivePricePerMonth,
            )
            TemplateConfiguration.PackageInfo(
                rcPackage = it,
                localization = ProcessedLocalizedConfiguration.create(
                    variableDataProvider = variableDataProvider,
                    context = VariableProcessor.PackageContext(discountRelativeToMostExpensivePerMonth),
                    localizedConfiguration = localization,
                    rcPackage = it,
                    locale = locale,
                ),
                currentlySubscribed = currentlySubscribed,
                discountRelativeToMostExpensivePerMonth = discountRelativeToMostExpensivePerMonth,
            )
        }

        return when (configurationType) {
            PackageConfigurationType.SINGLE -> {
                val firstPackage = packageInfos.first()

                Result.success(
                    TemplateConfiguration.PackageConfiguration.Single(
                        singlePackage = firstPackage,
                    ),
                )
            }
            PackageConfigurationType.MULTIPLE -> {
                val firstPackage = packageInfos.first()
                val defaultPackage = packageInfos.firstOrNull { it.rcPackage.identifier == default } ?: firstPackage

                Result.success(
                    TemplateConfiguration.PackageConfiguration.Multiple(
                        TemplateConfiguration.PackageConfiguration.MultiPackage(
                            first = firstPackage,
                            default = defaultPackage,
                            all = packageInfos,
                        ),
                    ),
                )
            }
            PackageConfigurationType.MULTITIER -> {
                val firstTier = paywallData.config.tiers?.firstOrNull()
                    ?: return Result.failure(PackageConfigurationError("No tier found for $packageIdsInConfig"))

                val (_, localizedConfigurationByTier) = paywallData.localizedConfigurationByTier

                val all = paywallData.config.tiers?.associateWith { tier ->
                    val localizationForTier = localizedConfigurationByTier[tier.id]
                        ?: return Result.failure(PackageConfigurationError("No localization found for $tier.id"))

                    val packageInfosForTier = reprocessPackagesForTiers(
                        from = packageInfos,
                        filter = tier.packages,
                        localization = localizationForTier,
                        variableDataProvider = variableDataProvider,
                        locale = locale,
                    )

                    val firstPackage = packageInfosForTier.first()
                    val defaultPackage = packageInfosForTier.firstOrNull { it.rcPackage.identifier == tier.defaultPackage } ?: firstPackage

                    TemplateConfiguration.PackageConfiguration.MultiPackage(
                        first = firstPackage,
                        default = defaultPackage,
                        all = packageInfosForTier,
                    )
                } ?: emptyMap()

                val localizationByTierId = paywallData.localizedConfigurationByTier.second

                val tierNames = paywallData.config.tiers?.associateWith {
                    localizationByTierId[it.id]!!.tierName!! // TODO: this is bad
                } ?: emptyMap()

                Result.success(
                    TemplateConfiguration.PackageConfiguration.MultiTier(
                        firstTier = firstTier,
                        allTiers = all,
                        tierNames = tierNames,
                    ),
                )
            }
        }
    }

    private fun reprocessPackagesForTiers(
        from: List<TemplateConfiguration.PackageInfo>,
        filter: List<String>,
        localization: PaywallData.LocalizedConfiguration,
        variableDataProvider: VariableDataProvider,
        locale: Locale,
    ): List<TemplateConfiguration.PackageInfo> {
        val filtered = from.filter { filter.contains(it.rcPackage.identifier) }
        val mostExpensivePricePerMonth = mostExpensivePricePerMonth(filtered.map { it.rcPackage })

        return filtered
            .map {
                val discount = productDiscount(
                    pricePerMonth = it.rcPackage.product.pricePerMonth(),
                    mostExpensive = mostExpensivePricePerMonth,
                )

                TemplateConfiguration.PackageInfo(
                    rcPackage = it.rcPackage,
                    localization = ProcessedLocalizedConfiguration.create(
                        variableDataProvider = variableDataProvider,
                        context = VariableProcessor.PackageContext(discount),
                        localizedConfiguration = localization,
                        rcPackage = it.rcPackage,
                        locale = locale,
                    ),
                    currentlySubscribed = it.currentlySubscribed,
                    discountRelativeToMostExpensivePerMonth = discount,
                )
            }
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

private fun Package.currentlySubscribed(
    activelySubscribedProductIdentifiers: Set<String>,
    nonSubscriptionProductIdentifiers: Set<String>,
): Boolean = when (packageType) {
    PackageType.ANNUAL,
    PackageType.SIX_MONTH,
    PackageType.THREE_MONTH,
    PackageType.TWO_MONTH,
    PackageType.MONTHLY,
    PackageType.WEEKLY,
    -> activelySubscribedProductIdentifiers.contains(product.id)

    PackageType.LIFETIME, PackageType.CUSTOM,
    -> nonSubscriptionProductIdentifiers.contains(product.id)

    PackageType.UNKNOWN -> false
}
