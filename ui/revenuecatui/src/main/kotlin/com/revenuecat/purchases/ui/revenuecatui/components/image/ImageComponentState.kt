@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.image

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.window.core.layout.WindowWidthSizeClass
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentViewState
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition
import com.revenuecat.purchases.ui.revenuecatui.components.buildPresentedPartial
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.addMargin
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toContentScale
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toLocaleId
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toShape
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.AspectRatio
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.style.ImageComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.calculateOfferEligibility

@Stable
@JvmSynthetic
@Composable
internal fun rememberUpdatedImageComponentState(
    style: ImageComponentStyle,
    paywallState: PaywallState.Loaded.Components,
): ImageComponentState {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
    val density = LocalDensity.current
    val darkMode = isSystemInDarkTheme()
    val layoutDirection = LocalLayoutDirection.current

    return remember(style) {
        ImageComponentState(
            initialWindowSize = windowSize,
            initialDensity = density,
            initialDarkMode = darkMode,
            initialLayoutDirection = layoutDirection,
            style = style,
            localeProvider = { paywallState.locale },
            selectedPackageInfoProvider = { paywallState.selectedPackageInfo },
            selectedTabIndexProvider = { paywallState.selectedTabIndex },
        )
    }.apply {
        update(
            windowSize = windowSize,
            density = density,
            darkMode = darkMode,
            layoutDirection = layoutDirection,
        )
    }
}

@Suppress("LongParameterList")
@Stable
internal class ImageComponentState(
    initialWindowSize: WindowWidthSizeClass,
    initialDensity: Density,
    initialDarkMode: Boolean,
    initialLayoutDirection: LayoutDirection,
    private val style: ImageComponentStyle,
    private val localeProvider: () -> Locale,
    private val selectedPackageInfoProvider: () -> PaywallState.Loaded.Components.SelectedPackageInfo?,
    private val selectedTabIndexProvider: () -> Int,
) {
    private var windowSize by mutableStateOf(initialWindowSize)
    private var density by mutableStateOf(initialDensity)
    private var darkMode by mutableStateOf(initialDarkMode)
    private var layoutDirection by mutableStateOf(initialLayoutDirection)

    private val selected by derivedStateOf {
        val selectedInfo = selectedPackageInfoProvider()
        when {
            style.rcPackage != null -> style.rcPackage.identifier == selectedInfo?.rcPackage?.identifier
            style.tabIndex != null -> style.tabIndex == selectedTabIndexProvider()
            else -> false
        }
    }

    private val offerEligibility by derivedStateOf {
        if (style.rcPackage != null) {
            calculateOfferEligibility(style.resolvedOffer, style.rcPackage)
        } else {
            selectedPackageInfoProvider()?.let {
                calculateOfferEligibility(it.resolvedOffer, it.rcPackage)
            } ?: OfferEligibility.Ineligible
        }
    }

    private val presentedPartial by derivedStateOf {
        val windowCondition = ScreenCondition.from(windowSize)
        val componentState = if (selected) ComponentViewState.SELECTED else ComponentViewState.DEFAULT

        style.overrides.buildPresentedPartial(windowCondition, offerEligibility, componentState)
    }
    private val themeImageUrls: ThemeImageUrls by derivedStateOf {
        val localeId = localeProvider().toLocaleId()

        presentedPartial?.sources?.run { getOrDefault(localeId, entry.value) }
            ?: style.sources.run { getOrDefault(localeId, entry.value) }
    }

    @get:JvmSynthetic
    val visible by derivedStateOf { presentedPartial?.partial?.visible ?: style.visible }

    @get:JvmSynthetic
    val imageUrls: ImageUrls by derivedStateOf {
        if (darkMode) themeImageUrls.dark ?: themeImageUrls.light else themeImageUrls.light
    }

    private val imageAspectRatio: Float by derivedStateOf {
        imageUrls.width.toFloat() / imageUrls.height.toFloat()
    }

    @get:JvmSynthetic
    val size: Size by derivedStateOf {
        (presentedPartial?.partial?.size ?: style.size).adjustForImage(imageUrls, density)
    }

    /**
     * Depending on the [size], it's sometimes possible to figure out the aspect ratio of the view before the
     * measurement phase. If that is the case, this will be non-null. This is especially helpful when one axis is set
     * to Fit.
     */
    @get:JvmSynthetic
    val aspectRatio: AspectRatio? by derivedStateOf {
        with(size) {
            when (val height = height) {
                is Fit -> when (width) {
                    is Fit -> AspectRatio(ratio = imageAspectRatio, matchHeightConstraintsFirst = true)
                    is Fill -> AspectRatio(ratio = imageAspectRatio, matchHeightConstraintsFirst = true)
                    is Fixed -> null
                }

                is Fill -> when (width) {
                    is Fit -> AspectRatio(ratio = imageAspectRatio, matchHeightConstraintsFirst = false)
                    is Fill -> null
                    is Fixed -> null
                }

                is Fixed -> when (val width = width) {
                    is Fit -> null
                    is Fill -> null
                    is Fixed -> AspectRatio(
                        ratio = width.value.toFloat() / height.value.toFloat(),
                        matchHeightConstraintsFirst = true,
                    )
                }
            }
        }
    }

    @get:JvmSynthetic
    val padding by derivedStateOf { presentedPartial?.partial?.padding?.toPaddingValues() ?: style.padding }

    @get:JvmSynthetic
    val margin by derivedStateOf { presentedPartial?.partial?.margin?.toPaddingValues() ?: style.margin }

    @get:JvmSynthetic
    val sizePlusMargin: Size by derivedStateOf {
        size.addMargin(margin, layoutDirection)
    }

    @get:JvmSynthetic
    val marginAdjustedAspectRatio: AspectRatio? by derivedStateOf {
        with(sizePlusMargin) {
            when (val height = height) {
                is Fixed -> when (val width = width) {
                    is Fit,
                    is Fill,
                    -> null
                    is Fixed -> {
                        AspectRatio(
                            ratio = width.value.toFloat() / height.value.toFloat(),
                            matchHeightConstraintsFirst = true,
                        )
                    }
                }
                is Fit,
                is Fill,
                -> null
            }
        }
    }

    @get:JvmSynthetic
    val shape: Shape? by derivedStateOf { presentedPartial?.partial?.maskShape?.toShape() ?: style.shape }

    @get:JvmSynthetic
    val border by derivedStateOf { presentedPartial?.border ?: style.border }

    @get:JvmSynthetic
    val shadow by derivedStateOf { presentedPartial?.shadow ?: style.shadow }

    @get:JvmSynthetic
    val overlay: ColorStyles? by derivedStateOf { presentedPartial?.overlay ?: style.overlay }

    @get:JvmSynthetic
    val contentScale: ContentScale by derivedStateOf {
        presentedPartial?.partial?.fitMode?.toContentScale() ?: style.contentScale
    }

    @JvmSynthetic
    fun update(
        windowSize: WindowWidthSizeClass? = null,
        density: Density? = null,
        darkMode: Boolean? = null,
        layoutDirection: LayoutDirection? = null,
    ) {
        if (windowSize != null) this.windowSize = windowSize
        if (density != null) this.density = density
        if (darkMode != null) this.darkMode = darkMode
        if (layoutDirection != null) this.layoutDirection = layoutDirection
    }

    /**
     * Adjusts this size to take into account the size of the image.
     */
    private fun Size.adjustForImage(imageUrls: ImageUrls, density: Density): Size =
        Size(
            width = width.adjustDimension(
                other = height,
                thisImageDimensionPx = imageUrls.width,
                otherImageDimensionPx = imageUrls.height,
                density = density,
            ),
            height = height.adjustDimension(
                other = width,
                thisImageDimensionPx = imageUrls.height,
                otherImageDimensionPx = imageUrls.width,
                density = density,
            ),
        )

    /**
     * Adjusts this size constraint to take into account the size of the image.
     */
    private fun SizeConstraint.adjustDimension(
        other: SizeConstraint,
        thisImageDimensionPx: UInt,
        otherImageDimensionPx: UInt,
        density: Density,
    ): SizeConstraint = when (this) {
        is Fit -> {
            when (other) {
                is Fit -> Fixed(with(density) { thisImageDimensionPx.toInt().toDp().value.toUInt() })
                is Fill -> this

                is Fixed -> {
                    // If the other dimension is Fixed, we'll have to scale this one by the same factor.
                    val otherImageDimensionDp = with(density) { otherImageDimensionPx.toInt().toDp() }
                    val scaleFactor = other.value.toFloat() / otherImageDimensionDp.value
                    Fixed(with(density) { (scaleFactor * thisImageDimensionPx.toInt()).toDp().value.toUInt() })
                }
            }
        }

        is Fill,
        is Fixed,
        -> this
    }
}
