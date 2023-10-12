package com.revenuecat.purchases.ui.revenuecatui.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.mutableStateOf
import com.revenuecat.purchases.ui.revenuecatui.PaywallViewMode
import com.revenuecat.purchases.ui.revenuecatui.data.processed.ProcessedLocalizedConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration

internal sealed class PaywallViewState {
    object Loading : PaywallViewState()

    data class Error(val errorMessage: String) : PaywallViewState()

    data class Loaded(
        val templateConfiguration: TemplateConfiguration,
        val selectedPackage: MutableState<TemplateConfiguration.PackageInfo>,
    ) : PaywallViewState() {
        constructor(templateConfiguration: TemplateConfiguration, selectedPackage: TemplateConfiguration.PackageInfo) :
            this(templateConfiguration, mutableStateOf(selectedPackage))

        fun selectPackage(packageInfo: TemplateConfiguration.PackageInfo) {
            selectedPackage.value = packageInfo
        }
    }
}

internal val PaywallViewState.Loaded.selectedLocalization: ProcessedLocalizedConfiguration
    get() = selectedPackage.value.localization

internal val PaywallViewState.Loaded.currentColors: TemplateConfiguration.Colors
    @Composable @ReadOnlyComposable
    get() = templateConfiguration.getCurrentColors()

internal val PaywallViewState.Loaded.isInFullScreenMode: Boolean
    get() = templateConfiguration.mode == PaywallViewMode.FULL_SCREEN
