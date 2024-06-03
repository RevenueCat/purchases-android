package com.revenuecat.purchases.ui.revenuecatui.data.processed

import android.net.Uri
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.PaywallMode

internal object TemplateConfigurationFactory {
    @Suppress("LongParameterList", "ThrowsCount")
    fun create(
        variableDataProvider: VariableDataProvider,
        mode: PaywallMode,
        paywallData: PaywallData,
        availablePackages: List<Package>,
        activelySubscribedProductIdentifiers: Set<String>,
        nonSubscriptionProductIdentifiers: Set<String>,
        template: PaywallTemplate,
    ): Result<TemplateConfiguration> {
        val (locale, localizedConfiguration) = paywallData.localizedConfiguration
        val sourceImages = paywallData.config.images

        val images = TemplateConfiguration.Images(
            iconUri = paywallData.getUriFromImage(sourceImages.icon),
            backgroundUri = paywallData.getUriFromImage(sourceImages.background),
            headerUri = paywallData.getUriFromImage(sourceImages.header),
        )

        val imagesByTier = paywallData.config.imagesByTier?.mapValues {
            TemplateConfiguration.Images(
                iconUri = paywallData.getUriFromImage(it.value.icon),
                backgroundUri = paywallData.getUriFromImage(it.value.background),
                headerUri = paywallData.getUriFromImage(it.value.header),
            )
        } ?: emptyMap()

        val createPackageResult =
            PackageConfigurationFactory.createPackageConfiguration(
                variableDataProvider = variableDataProvider,
                availablePackages = availablePackages,
                activelySubscribedProductIdentifiers = activelySubscribedProductIdentifiers,
                nonSubscriptionProductIdentifiers = nonSubscriptionProductIdentifiers,
                packageIdsInConfig = paywallData.config.packageIds,
                default = paywallData.config.defaultPackage,
                localization = localizedConfiguration,
                configurationType = template.configurationType,
                locale = locale,
                paywallData = paywallData,
            )
        val packageConfiguration = createPackageResult.getOrElse {
            return Result.failure(it)
        }

        return Result.success(
            TemplateConfiguration(
                locale = locale,
                template = template,
                mode = mode,
                packages = packageConfiguration,
                configuration = paywallData.config,
                images = images,
                imagesByTier = imagesByTier,
                colors = paywallData.config.colors,
            ),
        )
    }

    private fun PaywallData.getUriFromImage(image: String?): Uri? {
        return image?.let { Uri.parse(assetBaseURL.toString()).buildUpon().path(it).build() }
    }
}
