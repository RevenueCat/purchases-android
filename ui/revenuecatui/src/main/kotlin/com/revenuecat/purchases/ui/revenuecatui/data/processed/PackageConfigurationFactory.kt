package com.revenuecat.purchases.ui.revenuecatui.data.processed

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.errors.PackageConfigurationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import java.util.Locale

internal object PackageConfigurationFactory {
    @Suppress("LongParameterList", "LongMethod", "ReturnCount")
    fun createPackageConfiguration(
        variableDataProvider: VariableDataProvider,
        availablePackages: List<Package>,
        activelySubscribedProductIdentifiers: Set<String>,
        nonSubscriptionProductIdentifiers: Set<String>,
        packageIdsInConfig: List<String>,
        default: String?,
        configurationType: PackageConfigurationType,
        paywallData: PaywallData,
    ): Result<Pair<Locale, TemplateConfiguration.PackageConfiguration>> {
        val availablePackagesById = availablePackages.associateBy { it.identifier }
        val filteredRCPackages = packageIdsInConfig.mapNotNull {
            val rcPackage = availablePackagesById[it]
            if (rcPackage == null) {
                Logger.d("Package with id $it not found. Ignoring.")
            }
            rcPackage
        }.takeUnless {
            it.isEmpty()
        } ?: availablePackages

        if (filteredRCPackages.isEmpty()) {
            // This won't happen because availablePackages won't be empty. Offerings can't have empty available packages
            return Result.failure(PackageConfigurationError("No packages found for ids $packageIdsInConfig"))
        }

        return when (configurationType) {
            PackageConfigurationType.SINGLE -> {
                val (locale, packageInfos) = makePackageInfo(
                    packages = filteredRCPackages,
                    variableDataProvider = variableDataProvider,
                    activelySubscribedProductIdentifiers = activelySubscribedProductIdentifiers,
                    nonSubscriptionProductIdentifiers = nonSubscriptionProductIdentifiers,
                    paywallData = paywallData,
                )

                val firstPackage = packageInfos.first()

                Result.success(
                    Pair(
                        locale,
                        TemplateConfiguration.PackageConfiguration.Single(
                            singlePackage = firstPackage,
                        ),
                    ),
                )
            }
            PackageConfigurationType.MULTIPLE -> {
                val (locale, packageInfos) = makePackageInfo(
                    packages = filteredRCPackages,
                    variableDataProvider = variableDataProvider,
                    activelySubscribedProductIdentifiers = activelySubscribedProductIdentifiers,
                    nonSubscriptionProductIdentifiers = nonSubscriptionProductIdentifiers,
                    paywallData = paywallData,
                )

                val firstPackage = packageInfos.first()
                val defaultPackage = packageInfos.firstOrNull { it.rcPackage.identifier == default } ?: firstPackage

                Result.success(
                    Pair(
                        locale,
                        TemplateConfiguration.PackageConfiguration.Multiple(
                            TemplateConfiguration.PackageConfiguration.MultiPackage(
                                first = firstPackage,
                                default = defaultPackage,
                                all = packageInfos,
                            ),
                        ),
                    ),
                )
            }
            PackageConfigurationType.MULTITIER -> { this
                val tiers = paywallData.config.tiers ?: run {
                    return Result.failure(PackageConfigurationError("No tier found for $packageIdsInConfig"))
                }

                val (locale, localizedConfigurationByTier) = paywallData.tieredLocalizedConfiguration

                val all = tiers.associateWith { tier ->
                    val localizationForTier = localizedConfigurationByTier[tier.id]
                        ?: return Result.failure(PackageConfigurationError("No localization found for ${tier.id}"))

                    val packageInfosForTier = reprocessPackagesForTiers(
                        from = availablePackages,
                        filter = tier.packages,
                        localization = localizationForTier,
                        variableDataProvider = variableDataProvider,
                        activelySubscribedProductIdentifiers = activelySubscribedProductIdentifiers,
                        nonSubscriptionProductIdentifiers = nonSubscriptionProductIdentifiers,
                        locale = locale,
                    )

                    val firstPackage = packageInfosForTier.firstOrNull()
                        ?: return Result.failure(PackageConfigurationError("No packages found for tier ${tier.id}"))
                    val defaultPackage = packageInfosForTier
                        .firstOrNull { it.rcPackage.identifier == tier.defaultPackage } ?: firstPackage

                    TemplateConfiguration.PackageConfiguration.MultiPackage(
                        first = firstPackage,
                        default = defaultPackage,
                        all = packageInfosForTier,
                    )
                }

                val allTierInfos = all.entries.map { (tier, packageInfo) ->
                    val tierName = packageInfo.default.localization.tierName
                        ?: return Result.failure(
                            PackageConfigurationError("No localized tier name found for ${tier.id}"),
                        )

                    TemplateConfiguration.TierInfo(
                        id = tier.id,
                        name = tierName,
                        defaultPackage = packageInfo.default,
                        packages = packageInfo.all,
                    )
                }

                Result.success(
                    Pair(
                        locale,
                        TemplateConfiguration.PackageConfiguration.MultiTier(
                            firstTier = allTierInfos.first(),
                            allTiers = allTierInfos,
                        ),
                    ),
                )
            }
        }
    }

    private fun makePackageInfo(
        packages: List<Package>,
        variableDataProvider: VariableDataProvider,
        activelySubscribedProductIdentifiers: Set<String>,
        nonSubscriptionProductIdentifiers: Set<String>,
        paywallData: PaywallData,
    ): Pair<Locale, List<TemplateConfiguration.PackageInfo>> {
        val mostExpensivePricePerMonth = mostExpensivePricePerMonth(packages)

        val (locale, localization) = paywallData.localizedConfiguration
        val packageInfos = packages.map {
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

        return Pair(locale, packageInfos)
    }

    @Suppress("LongParameterList")
    private fun reprocessPackagesForTiers(
        from: List<Package>,
        filter: List<String>,
        localization: PaywallData.LocalizedConfiguration,
        variableDataProvider: VariableDataProvider,
        activelySubscribedProductIdentifiers: Set<String>,
        nonSubscriptionProductIdentifiers: Set<String>,
        locale: Locale,
    ): List<TemplateConfiguration.PackageInfo> {
        val filtered = from.filter { filter.contains(it.identifier) }
        val mostExpensivePricePerMonth = mostExpensivePricePerMonth(filtered.map { it })

        return filtered
            .map {
                val discount = productDiscount(
                    pricePerMonth = it.product.pricePerMonth(),
                    mostExpensive = mostExpensivePricePerMonth,
                )

                val currentlySubscribed = it.currentlySubscribed(
                    activelySubscribedProductIdentifiers,
                    nonSubscriptionProductIdentifiers,
                )

                TemplateConfiguration.PackageInfo(
                    rcPackage = it,
                    localization = ProcessedLocalizedConfiguration.create(
                        variableDataProvider = variableDataProvider,
                        context = VariableProcessor.PackageContext(discount),
                        localizedConfiguration = localization,
                        rcPackage = it,
                        locale = locale,
                    ),
                    currentlySubscribed = currentlySubscribed,
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
