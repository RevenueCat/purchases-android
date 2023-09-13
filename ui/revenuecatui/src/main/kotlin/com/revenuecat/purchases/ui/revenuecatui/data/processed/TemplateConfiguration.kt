package com.revenuecat.purchases.ui.revenuecatui.data.processed

import android.net.Uri
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.PaywallViewMode

internal data class TemplateConfiguration(
    val template: PaywallTemplate,
    val mode: PaywallViewMode,
    val packageConfiguration: PackageConfiguration,
    val configuration: PaywallData.Configuration,
    val images: Images,
) {
    private val darkModeColors = ColorsFactory.create(configuration.colors, true)
    private val lightModeColors = ColorsFactory.create(configuration.colors, false)

    @Composable
    @ReadOnlyComposable
    fun getCurrentColors(): Colors {
        return if (isSystemInDarkTheme()) darkModeColors else lightModeColors
    }

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
        val callToActionBackground: Color,
        val callToActionForeground: Color,
        val callToActionSecondaryBackground: Color,
        val accent1: Color,
        val accent2: Color,
    )

    sealed class PackageConfiguration {
        data class Single(val packageInfo: PackageInfo) : PackageConfiguration()
        data class Multiple(
            val first: PackageInfo,
            val default: PackageInfo,
            val all: List<PackageInfo>,
        ) : PackageConfiguration()

        val defaultInfo: PackageInfo
            get() = when (this) {
                is Single -> packageInfo
                is Multiple -> default
            }

        val allPackagesInfo: List<PackageInfo>
            get() = when (this) {
                is Single -> listOf(packageInfo)
                is Multiple -> all
            }
    }
}
