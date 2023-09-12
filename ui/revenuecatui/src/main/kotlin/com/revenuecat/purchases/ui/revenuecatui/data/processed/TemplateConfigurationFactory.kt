package com.revenuecat.purchases.ui.revenuecatui.data.processed

import android.net.Uri
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.PaywallViewMode
import java.util.Locale

internal object TemplateConfigurationFactory {
    @Suppress("LongParameterList", "ThrowsCount")
    fun create(
        mode: PaywallViewMode,
        paywallData: PaywallData,
        packages: List<Package>,
        activelySubscribedProductIdentifiers: Set<String>,
        locale: Locale,
    ): TemplateConfiguration {
        val paywallTemplate = PaywallTemplate.fromId(paywallData.templateName)
            ?: throw IllegalArgumentException("Unknown template ${paywallData.templateName}")

        val localizedConfiguration = paywallData.configForLocale(locale)
            ?: error("No configuration found for locale $locale")
        val packageIds = paywallData.config.packages
        require(packageIds.isNotEmpty()) { "No packages ids found in paywall data" }
        require(packages.isNotEmpty()) { "No packages found in offering" }
        val images = TemplateConfiguration.Images(
            iconUri = paywallData.getUriFromImage(paywallData.config.images.icon),
            backgroundUri = paywallData.getUriFromImage(paywallData.config.images.background),
            headerUri = paywallData.getUriFromImage(paywallData.config.images.header),
        )
        val packageConfiguration = PackageConfigurationFactory.createPackageConfiguration(
            packages = packages,
            activelySubscribedProductIdentifiers = activelySubscribedProductIdentifiers,
            filter = packageIds,
            default = paywallData.config.defaultPackage,
            localization = localizedConfiguration,
            configurationType = paywallTemplate.configurationType,
            locale = locale,
        )
        return TemplateConfiguration(
            template = paywallTemplate,
            mode = mode,
            packageConfiguration = packageConfiguration,
            configuration = paywallData.config,
            images = images,
        )
    }

    private fun PaywallData.getUriFromImage(image: String?): Uri? {
        return image?.let { Uri.parse(assetBaseURL.toString()).buildUpon().path(it).build() }
    }
}
