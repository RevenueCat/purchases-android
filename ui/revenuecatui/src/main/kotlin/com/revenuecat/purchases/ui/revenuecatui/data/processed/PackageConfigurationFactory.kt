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
        storefrontCountryCode: String?,
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
                makeSinglePackageConfiguration(
                    filteredRCPackages,
                    variableDataProvider,
                    activelySubscribedProductIdentifiers,
                    nonSubscriptionProductIdentifiers,
                    paywallData,
                    storefrontCountryCode,
                )
            }
            PackageConfigurationType.MULTIPLE -> {
                makeMultiplePackageConfiguration(
                    filteredRCPackages,
                    variableDataProvider,
                    activelySubscribedProductIdentifiers,
                    nonSubscriptionProductIdentifiers,
                    paywallData,
                    default,
                    storefrontCountryCode,
                )
            }
            PackageConfigurationType.MULTITIER -> {
                makeMultiTierPackageConfiguration(
                    paywallData,
                    packageIdsInConfig,
                    availablePackages,
                    variableDataProvider,
                    activelySubscribedProductIdentifiers,
                    nonSubscriptionProductIdentifiers,
                    storefrontCountryCode,
                )
            }
        }
    }

    @Suppress("LongParameterList", "ReturnCount")
    private fun makeMultiTierPackageConfiguration(
        paywallData: PaywallData,
        packageIdsInConfig: List<String>,
        availablePackages: List<Package>,
        variableDataProvider: VariableDataProvider,
        activelySubscribedProductIdentifiers: Set<String>,
        nonSubscriptionProductIdentifiers: Set<String>,
        storefrontCountryCode: String?,
    ): Result<Pair<Locale, TemplateConfiguration.PackageConfiguration.MultiTier>> {
        val tiers = paywallData.config.tiers ?: return Result.failure(
            PackageConfigurationError("No tier found for $packageIdsInConfig"),
        )

        val (locale, localizedConfigurationByTier) = paywallData.tieredLocalizedConfiguration

        val all = tiers.associate { tier ->
            val localizationForTier = localizedConfigurationByTier[tier.id]
                ?: return Result.failure(PackageConfigurationError("No localization found for ${tier.id}"))

            val tierName = localizationForTier.tierName ?: ""

            val packageInfosForTier = reprocessPackagesForTiers(
                from = availablePackages,
                filter = tier.packageIds,
                localization = localizationForTier,
                variableDataProvider = variableDataProvider,
                activelySubscribedProductIdentifiers = activelySubscribedProductIdentifiers,
                nonSubscriptionProductIdentifiers = nonSubscriptionProductIdentifiers,
                locale = locale,
                storefrontCountryCode = storefrontCountryCode,
                zeroDecimalPlaceCountries = paywallData.zeroDecimalPlaceCountries,
            )

            val firstPackage = packageInfosForTier.firstOrNull()
                ?: run {
                    // Filter out tiers that don't have any products
                    Logger.e("Tier $tierName has no available products and will be removed from the paywall.")
                    return@associate tier to null
                }
            val defaultPackage = packageInfosForTier
                .firstOrNull { it.rcPackage.identifier == tier.defaultPackageId } ?: firstPackage

            tier to TemplateConfiguration.PackageConfiguration.MultiPackage(
                first = firstPackage,
                default = defaultPackage,
                all = packageInfosForTier,
            )
        }.filterNotNullValues()

        if (all.isEmpty()) {
            return Result.failure(PackageConfigurationError("None of the tiers have any available products."))
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

        val defaultTierInfo = paywallData.config.defaultTier?.let { tierId ->
            allTierInfos.firstOrNull { it.id == tierId }
        } ?: allTierInfos.first()

        return Result.success(
            locale to TemplateConfiguration.PackageConfiguration.MultiTier(
                defaultTier = defaultTierInfo,
                allTiers = allTierInfos,
            ),
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun <K, V> Map<K, V?>.filterNotNullValues(): Map<K, V> =
        filterValues { it != null } as Map<K, V>

    @Suppress("LongParameterList")
    private fun makeMultiplePackageConfiguration(
        filteredRCPackages: List<Package>,
        variableDataProvider: VariableDataProvider,
        activelySubscribedProductIdentifiers: Set<String>,
        nonSubscriptionProductIdentifiers: Set<String>,
        paywallData: PaywallData,
        default: String?,
        storefrontCountryCode: String?,
    ): Result<Pair<Locale, TemplateConfiguration.PackageConfiguration.Multiple>> {
        val (locale, packageInfos) = makePackageInfo(
            packages = filteredRCPackages,
            variableDataProvider = variableDataProvider,
            activelySubscribedProductIdentifiers = activelySubscribedProductIdentifiers,
            nonSubscriptionProductIdentifiers = nonSubscriptionProductIdentifiers,
            paywallData = paywallData,
            storefrontCountryCode = storefrontCountryCode,
        )

        val firstPackage = packageInfos.first()
        val defaultPackage = packageInfos.firstOrNull { it.rcPackage.identifier == default } ?: firstPackage

        return Result.success(
            locale to TemplateConfiguration.PackageConfiguration.Multiple(
                TemplateConfiguration.PackageConfiguration.MultiPackage(
                    first = firstPackage,
                    default = defaultPackage,
                    all = packageInfos,
                ),
            ),
        )
    }

    @Suppress("LongParameterList")
    private fun makeSinglePackageConfiguration(
        filteredRCPackages: List<Package>,
        variableDataProvider: VariableDataProvider,
        activelySubscribedProductIdentifiers: Set<String>,
        nonSubscriptionProductIdentifiers: Set<String>,
        paywallData: PaywallData,
        storefrontCountryCode: String?,
    ): Result<Pair<Locale, TemplateConfiguration.PackageConfiguration.Single>> {
        val (locale, packageInfos) = makePackageInfo(
            packages = filteredRCPackages,
            variableDataProvider = variableDataProvider,
            activelySubscribedProductIdentifiers = activelySubscribedProductIdentifiers,
            nonSubscriptionProductIdentifiers = nonSubscriptionProductIdentifiers,
            paywallData = paywallData,
            storefrontCountryCode = storefrontCountryCode,
        )

        val firstPackage = packageInfos.first()

        return Result.success(
            locale to TemplateConfiguration.PackageConfiguration.Single(
                singlePackage = firstPackage,
            ),
        )
    }

    @Suppress("LongParameterList")
    private fun makePackageInfo(
        packages: List<Package>,
        variableDataProvider: VariableDataProvider,
        activelySubscribedProductIdentifiers: Set<String>,
        nonSubscriptionProductIdentifiers: Set<String>,
        paywallData: PaywallData,
        storefrontCountryCode: String?,
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

            val shouldRound = if (storefrontCountryCode != null) {
                paywallData.zeroDecimalPlaceCountries.contains(
                    storefrontCountryCode,
                )
            } else {
                false
            }

            TemplateConfiguration.PackageInfo(
                rcPackage = it,
                localization = ProcessedLocalizedConfiguration.create(
                    variableDataProvider = variableDataProvider,
                    context = VariableProcessor.PackageContext(discountRelativeToMostExpensivePerMonth, shouldRound),
                    localizedConfiguration = localization,
                    rcPackage = it,
                    locale = locale,
                ),
                currentlySubscribed = currentlySubscribed,
                discountRelativeToMostExpensivePerMonth = discountRelativeToMostExpensivePerMonth,
            )
        }

        return locale to packageInfos
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
        storefrontCountryCode: String?,
        zeroDecimalPlaceCountries: List<String>,
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

                val shouldRound = if (storefrontCountryCode != null) {
                    zeroDecimalPlaceCountries.contains(
                        storefrontCountryCode,
                    )
                } else {
                    false
                }

                TemplateConfiguration.PackageInfo(
                    rcPackage = it,
                    localization = ProcessedLocalizedConfiguration.create(
                        variableDataProvider = variableDataProvider,
                        context = VariableProcessor.PackageContext(discount, shouldRound),
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
