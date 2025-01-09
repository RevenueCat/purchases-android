package com.revenuecat.purchases.ui.revenuecatui.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.intl.LocaleList
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toComposeLocale
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toLocaleId
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.processed.ProcessedLocalizedConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptySet
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

        @Suppress("LongParameterList")
        @Stable
        class Components(
            val stack: ComponentStyle,
            val stickyFooter: ComponentStyle?,
            val background: Background,
            override val offering: Offering,
            /**
             * All locales that this paywall supports, with `locales.head` being the default one.
             */
            private val locales: NonEmptySet<LocaleId>,
            initialSelectedPackage: Package?,
            initialLocaleList: LocaleList = LocaleList.current,
            initialIsEligibleForIntroOffer: Boolean = false,
        ) : Loaded {
            private var localeId by mutableStateOf(initialLocaleList.toLocaleId())

            val locale by derivedStateOf { localeId.toComposeLocale() }

            var isEligibleForIntroOffer by mutableStateOf(initialIsEligibleForIntroOffer)
                private set
            var selectedPackage by mutableStateOf<Package?>(initialSelectedPackage)
                private set

            // TODO Actually determine this.
            val showZeroDecimalPlacePrices: Boolean = true
            val mostExpensivePricePerMonthMicros: Long? = offering.availablePackages.mostExpensivePricePerMonthMicros()

            fun update(localeList: FrameworkLocaleList? = null, isEligibleForIntroOffer: Boolean? = null) {
                if (localeList != null) localeId = LocaleList(localeList.toLanguageTags()).toLocaleId()
                if (isEligibleForIntroOffer != null) this.isEligibleForIntroOffer = isEligibleForIntroOffer
            }

            fun update(selectedPackage: Package?) {
                this.selectedPackage = selectedPackage
            }

            private fun LocaleList.toLocaleId(): LocaleId =
                // Configured locales take precedence over the default one.
                map { it.toLocaleId() }.plus(locales.head)
                    // Find the first locale we have a LocalizationDictionary for.
                    .first { id -> locales.contains(id) }

            private fun List<Package>.mostExpensivePricePerMonthMicros(): Long? =
                asSequence()
                    .map { pkg -> pkg.product }
                    .mapNotNull { product -> product.pricePerMonth() }
                    .maxByOrNull { price -> price.amountMicros }
                    ?.amountMicros
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
