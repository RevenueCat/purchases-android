package com.revenuecat.purchases.ui.revenuecatui.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.mutableStateOf
import com.revenuecat.purchases.ui.revenuecatui.PaywallMode
import com.revenuecat.purchases.ui.revenuecatui.data.processed.ProcessedLocalizedConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration

internal sealed class PaywallState {
    object Loading : PaywallState()

    data class Error(val errorMessage: String) : PaywallState()

    data class Loaded(
        val templateConfiguration: TemplateConfiguration,
        val selectedPackage: MutableState<TemplateConfiguration.PackageInfo>,
        val shouldDisplayDismissButton: Boolean,
    ) : PaywallState() {
        constructor(
            templateConfiguration: TemplateConfiguration,
            selectedPackage: TemplateConfiguration.PackageInfo,
            shouldDisplayDismissButton: Boolean,
        ) :
            this(templateConfiguration, mutableStateOf(selectedPackage), shouldDisplayDismissButton)

        fun selectPackage(packageInfo: TemplateConfiguration.PackageInfo) {
            selectedPackage.value = packageInfo
        }
    }
}

internal val PaywallState.Loaded.selectedLocalization: ProcessedLocalizedConfiguration
    get() = selectedPackage.value.localization

internal val PaywallState.Loaded.currentColors: TemplateConfiguration.Colors
    @Composable @ReadOnlyComposable
    get() = templateConfiguration.getCurrentColors()

internal val PaywallState.Loaded.isInFullScreenMode: Boolean
    get() = templateConfiguration.mode == PaywallMode.FULL_SCREEN
