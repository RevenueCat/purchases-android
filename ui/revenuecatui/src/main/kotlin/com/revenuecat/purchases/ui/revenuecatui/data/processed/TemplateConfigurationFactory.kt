package com.revenuecat.purchases.ui.revenuecatui.data.processed

import android.net.Uri
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.PaywallViewMode

internal object TemplateConfigurationFactory {
    @Suppress("LongParameterList", "ThrowsCount")
    fun create(
        variableDataProvider: VariableDataProvider,
        mode: PaywallViewMode,
        paywallData: PaywallData,
        packages: List<Package>,
        activelySubscribedProductIdentifiers: Set<String>,
        template: PaywallTemplate,
    ): Result<TemplateConfiguration> {
        val (locale, localizedConfiguration) = paywallData.localizedConfiguration
        val packageIds = paywallData.config.packages.takeUnless {
            it.isEmpty()
        } ?: packages.map { it.identifier }
        val images = TemplateConfiguration.Images(
            iconUri = paywallData.getUriFromImage(paywallData.config.images.icon),
            backgroundUri = paywallData.getUriFromImage(paywallData.config.images.background),
            headerUri = paywallData.getUriFromImage(paywallData.config.images.header),
        )

        val createPackageResult =
            PackageConfigurationFactory.createPackageConfiguration(
                variableDataProvider = variableDataProvider,
                packages = packages,
                activelySubscribedProductIdentifiers = activelySubscribedProductIdentifiers,
                filter = packageIds,
                default = paywallData.config.defaultPackage,
                localization = localizedConfiguration,
                configurationType = template.configurationType,
                locale = locale,
            )
        val packageConfiguration = createPackageResult.getOrElse {
            return Result.failure(it)
        }
        return Result.success(
            TemplateConfiguration(
                template = template,
                mode = mode,
                packages = packageConfiguration,
                configuration = paywallData.config,
                images = images,
            ),
        )
    }

    private fun PaywallData.getUriFromImage(image: String?): Uri? {
        return image?.let { Uri.parse(assetBaseURL.toString()).buildUpon().path(it).build() }
    }
}