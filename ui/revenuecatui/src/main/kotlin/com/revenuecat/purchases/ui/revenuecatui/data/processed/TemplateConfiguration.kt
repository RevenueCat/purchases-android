package com.revenuecat.purchases.ui.revenuecatui.data.processed

import android.net.Uri
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.PaywallMode
import java.util.Locale

@Immutable
internal data class TemplateConfiguration(
    val template: PaywallTemplate,
    val mode: PaywallMode,
    val packages: PackageConfiguration,
    val configuration: PaywallData.Configuration,
    val images: Images,
    val imagesByTier: Map<String, Images>,
    val colors: PaywallData.Configuration.ColorInformation,
    val locale: Locale,
) {
    private val darkModeColors = ColorsFactory.create(colors.dark ?: colors.light)
    private val lightModeColors = ColorsFactory.create(colors.light)

    @Composable
    @ReadOnlyComposable
    fun getCurrentColors(): Colors {
        return if (isSystemInDarkTheme()) darkModeColors else lightModeColors
    }

    @Composable
    @ReadOnlyComposable
    fun getCurrentColorsForTier(tier: TierInfo): Colors {
        val colorByTier = configuration.colorsByTier?.get(tier.id)?.let {
            if (isSystemInDarkTheme()) ColorsFactory.create(it.dark ?: it.light) else ColorsFactory.create(it.light)
        }

        return colorByTier ?: getCurrentColors()
    }

    @Immutable
    data class PackageInfo(
        val rcPackage: Package,
        val localization: ProcessedLocalizedConfiguration,
        val discountRelativeToMostExpensivePerMonth: Double?,
    )

    @Immutable
    data class Images(
        val iconUri: Uri?,
        val backgroundUri: Uri?,
        val headerUri: Uri?,
    )

    @Immutable
    data class Colors(
        val background: Color,
        val text1: Color,
        val text2: Color,
        val text3: Color,
        val callToActionBackground: Color,
        val callToActionForeground: Color,
        val callToActionSecondaryBackground: Color?,
        val accent1: Color,
        val accent2: Color,
        val accent3: Color,
        val closeButton: Color?,
        val tierControlBackground: Color?,
        val tierControlForeground: Color?,
        val tierControlSelectedBackground: Color?,
        val tierControlSelectedForeground: Color?,
    )

    @Immutable
    data class TierInfo(
        val name: String,
        val id: String,
        val defaultPackage: PackageInfo,
        val packages: List<PackageInfo>,
    )

    @Stable
    sealed class PackageConfiguration {

        abstract val all: List<PackageInfo>
        abstract val default: PackageInfo

        @Immutable
        data class MultiPackage(
            val first: PackageInfo,
            val default: PackageInfo,
            val all: List<PackageInfo>,
        )

        @Immutable
        data class Single(val singlePackage: PackageInfo) : PackageConfiguration() {
            override val all: List<PackageInfo>
                get() = listOf(singlePackage)
            override val default: PackageInfo
                get() = singlePackage
        }

        @Immutable
        data class Multiple(val multiPackage: MultiPackage) : PackageConfiguration() {
            override val all: List<PackageInfo>
                get() = multiPackage.all
            override val default: PackageInfo
                get() = multiPackage.default
        }

        @Immutable
        data class MultiTier(
            val defaultTier: TierInfo,
            val allTiers: List<TierInfo>,
        ) : PackageConfiguration() {
            override val all: List<PackageInfo>
                get() = allTiers.map { it.packages }.flatten()
            override val default: PackageInfo
                get() = defaultTier.defaultPackage
        }
    }
}
