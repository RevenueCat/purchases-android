package com.revenuecat.purchases.ui.revenuecatui.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.revenuecat.purchases.ui.revenuecatui.data.processed.ProcessedLocalizedConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration

internal sealed class PaywallViewState {
    object Loading : PaywallViewState()
    data class Error(val errorMessage: String) : PaywallViewState()
    data class Loaded(
        val templateConfiguration: TemplateConfiguration,
        val selectedPackage: TemplateConfiguration.PackageInfo,
    ) : PaywallViewState()
}

internal val PaywallViewState.Loaded.selectedLocalization: ProcessedLocalizedConfiguration
    get() = selectedPackage.localization

internal val PaywallViewState.Loaded.currentColors: TemplateConfiguration.Colors
    @Composable @ReadOnlyComposable
    get() = templateConfiguration.getCurrentColors()
