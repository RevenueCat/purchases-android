package com.revenuecat.purchases.ui.revenuecatui.data.processed

import android.net.Uri
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.PaywallMode
import java.util.Locale

internal data class TemplateConfiguration(
    val template: PaywallTemplate,
    val mode: PaywallMode,
    val packages: PackageConfiguration,
    val packagesByTier: Map<String, PackageConfiguration>,
    val configuration: PaywallData.Configuration,
    val images: Images,
    val imagesByTier: Map<String, Images>,
    val locale: Locale,
) {
    private val darkModeColors = ColorsFactory.create(configuration.colors.dark ?: configuration.colors.light)
    private val lightModeColors = ColorsFactory.create(configuration.colors.light)

    @Composable
    @ReadOnlyComposable
    fun getCurrentColors(): Colors {
        return if (isSystemInDarkTheme()) darkModeColors else lightModeColors
    }

    @Composable
    @ReadOnlyComposable
    fun getCurrentColorsForTier(tier: PaywallData.Configuration.Tier): Colors {
        val colorByTier = configuration.colorsByTier?.get(tier.id)?.let {
            if (isSystemInDarkTheme()) ColorsFactory.create(it.dark ?: it.light) else ColorsFactory.create(it.light)
        }

        return colorByTier ?: run {
            // TODO: Add log that this is being used (maybe)
            getCurrentColors()
        }
    }

//    @Composable
//    @ReadOnlyComposable
//    fun getCurrentImagesForTier(tier: PaywallData.Configuration.Tier): Images {
//
//        return configuration.images
//    }

    data class PackageInfo(
        val rcPackage: Package,
        val localization: ProcessedLocalizedConfiguration,
        val currentlySubscribed: Boolean,
        val discountRelativeToMostExpensivePerMonth: Double?,
    )

    data class Images(
        val iconUri: Uri?,
        val backgroundUri: Uri?,
        val headerUri: Uri?,
    )

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
    )

    sealed class PackageConfiguration(open val default: PackageInfo, open val all: List<PackageInfo>) {
        data class Single(val packageInfo: PackageInfo) : PackageConfiguration(packageInfo, listOf(packageInfo))
        data class Multiple(
            val first: PackageInfo,
            override val default: PackageInfo,
            override val all: List<PackageInfo>,
        ) : PackageConfiguration(default, all)
    }
}
