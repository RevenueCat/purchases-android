package com.revenuecat.purchases.ui.revenuecatui.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.ui.revenuecatui.data.processed.ProcessedLocalizedConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.isFullScreen
import android.os.LocaleList as FrameworkLocaleList

internal sealed interface PaywallState {
    object Loading : PaywallState

    data class Error(val errorMessage: String) : PaywallState {
        init {
            Logger.e("Paywall transitioned to error state: $errorMessage")
        }
    }

    sealed interface Loaded : PaywallState {
        val offering: Offering

        data class Legacy(
            override val offering: Offering,
            val templateConfiguration: TemplateConfiguration,
            val selectedPackage: MutableState<TemplateConfiguration.PackageInfo>,
            val shouldDisplayDismissButton: Boolean,
        ) : Loaded {

            constructor(
                offering: Offering,
                templateConfiguration: TemplateConfiguration,
                selectedPackage: TemplateConfiguration.PackageInfo,
                shouldDisplayDismissButton: Boolean,
            ) :
                this(
                    offering,
                    templateConfiguration,
                    mutableStateOf(selectedPackage),
                    shouldDisplayDismissButton,
                )

            fun selectPackage(packageInfo: TemplateConfiguration.PackageInfo) {
                selectedPackage.value = packageInfo
            }
        }

        @Stable
        class Components(
            override val offering: Offering,
            val data: PaywallComponentsData,
            initialLocaleList: LocaleList = LocaleList.current,
            initialIsEligibleForIntroOffer: Boolean = false,
        ) : Loaded {
            private var localeId by mutableStateOf(initialLocaleList.toLocaleId())

            val localizationDictionary by derivedStateOf { data.componentsLocalizations.getValue(localeId) }
            val locale by derivedStateOf { localeId.toLocale() }

            var isEligibleForIntroOffer by mutableStateOf(initialIsEligibleForIntroOffer)
                private set

            fun update(localeList: FrameworkLocaleList? = null, isEligibleForIntroOffer: Boolean? = null) {
                if (localeList != null) localeId = LocaleList(localeList.toLanguageTags()).toLocaleId()
                if (isEligibleForIntroOffer != null) this.isEligibleForIntroOffer = isEligibleForIntroOffer
            }

            private fun LocaleList.toLocaleId(): LocaleId =
                // Configured locales take precedence over the default one.
                map { it.toLocaleId() }.plus(data.defaultLocaleIdentifier)
                    // Find the first locale we have a LocalizationDictionary for.
                    .first { id -> data.componentsLocalizations.containsKey(id) }

            private fun LocaleId.toLocale(): Locale =
                Locale(value.replace('_', '-'))

            private fun Locale.toLocaleId(): LocaleId =
                LocaleId(toLanguageTag().replace('-', '_'))
        }
    }
}

internal fun PaywallState.loadedLegacy(): PaywallState.Loaded.Legacy? {
    return when (val state = this) {
        is PaywallState.Error -> null
        is PaywallState.Loaded -> when (state) {
            is PaywallState.Loaded.Legacy -> state
            is PaywallState.Loaded.Components -> null
        }
        is PaywallState.Loading -> null
    }
}

internal val PaywallState.Loaded.Legacy.selectedLocalization: ProcessedLocalizedConfiguration
    get() = selectedPackage.value.localization

internal val PaywallState.Loaded.Legacy.currentColors: TemplateConfiguration.Colors
    @Composable @ReadOnlyComposable
    get() = templateConfiguration.getCurrentColors()

internal val PaywallState.Loaded.Legacy.isInFullScreenMode: Boolean
    get() = templateConfiguration.mode.isFullScreen
