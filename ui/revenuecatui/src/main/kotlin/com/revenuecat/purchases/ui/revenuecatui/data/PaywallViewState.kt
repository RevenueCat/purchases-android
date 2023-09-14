package com.revenuecat.purchases.ui.revenuecatui.data

import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration

internal sealed class PaywallViewState {
    object Loading : PaywallViewState()
    data class Error(val errorMessage: String) : PaywallViewState()
    data class Loaded(
        val templateConfiguration: TemplateConfiguration,
        val selectedPackage: TemplateConfiguration.PackageInfo,
    ) : PaywallViewState()
}
