package com.revenuecat.purchases.ui.revenuecatui.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.mutableStateOf
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.data.processed.ProcessedLocalizedConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.isFullScreen
import com.revenuecat.purchases.ui.revenuecatui.templates.selectedLocalizationForTier

internal sealed class PaywallState {
    object Loading : PaywallState()

    data class Error(val errorMessage: String) : PaywallState() {
        init {
            Logger.e("Paywall transitioned to error state: $errorMessage")
        }
    }

    data class Loaded(
        val offering: Offering,
        val templateConfiguration: TemplateConfiguration,
        val selectedPackage: MutableState<TemplateConfiguration.PackageInfo>,
        val selectedTier: MutableState<PaywallData.Configuration.Tier>,
        val shouldDisplayDismissButton: Boolean,
    ) : PaywallState() {
        constructor(
            offering: Offering,
            templateConfiguration: TemplateConfiguration,
            selectedPackage: TemplateConfiguration.PackageInfo,
            selectedTier: PaywallData.Configuration.Tier,
            shouldDisplayDismissButton: Boolean,
        ) :
            this(offering, templateConfiguration, mutableStateOf(selectedPackage), mutableStateOf(selectedTier), shouldDisplayDismissButton)

        fun selectPackage(packageInfo: TemplateConfiguration.PackageInfo) {
            selectedPackage.value = packageInfo
        }

        fun selectTier(tier: PaywallData.Configuration.Tier) {
            selectedTier.value = tier
            selectedPackage.value = templateConfiguration.packagesByTier[selectedTier.value.id]!!.default
            // TODO: Fix this !!
        }
    }
}

internal fun PaywallState.loaded(): PaywallState.Loaded? {
    return when (val state = this) {
        is PaywallState.Error -> null
        is PaywallState.Loaded -> state
        is PaywallState.Loading -> null
    }
}

internal val PaywallState.Loaded.selectedLocalization: ProcessedLocalizedConfiguration
    get() = selectedPackage.value.localization

internal val PaywallState.Loaded.currentColors: TemplateConfiguration.Colors
    @Composable @ReadOnlyComposable
    get() = templateConfiguration.getCurrentColors()

internal val PaywallState.Loaded.isInFullScreenMode: Boolean
    get() = templateConfiguration.mode.isFullScreen
