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
        validatedPaywallData: PaywallData,
        packages: List<Package>,
        activelySubscribedProductIdentifiers: Set<String>,
        template: PaywallTemplate,
    ): TemplateConfiguration {
        val (locale, localizedConfiguration) = validatedPaywallData.localizedConfiguration
        val packageIds = validatedPaywallData.config.packages.takeUnless {
            it.isEmpty()
        } ?: packages.map { it.identifier }
        val images = TemplateConfiguration.Images(
            iconUri = validatedPaywallData.getUriFromImage(validatedPaywallData.config.images.icon),
            backgroundUri = validatedPaywallData.getUriFromImage(validatedPaywallData.config.images.background),
            headerUri = validatedPaywallData.getUriFromImage(validatedPaywallData.config.images.header),
        )
        val packageConfiguration = PackageConfigurationFactory.createPackageConfiguration(
            variableDataProvider = variableDataProvider,
            packages = packages,
            activelySubscribedProductIdentifiers = activelySubscribedProductIdentifiers,
            filter = packageIds,
            default = validatedPaywallData.config.defaultPackage,
            localization = localizedConfiguration,
            configurationType = template.configurationType,
            locale = locale,
        )
        return TemplateConfiguration(
            template = template,
            mode = mode,
            packages = packageConfiguration,
            configuration = validatedPaywallData.config,
            images = images,
        )
    }

    private fun PaywallData.getUriFromImage(image: String?): Uri? {
        return image?.let { Uri.parse(assetBaseURL.toString()).buildUpon().path(it).build() }
    }
}
