package com.revenuecat.purchases.ui.revenuecatui.components.video

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
import com.revenuecat.purchases.paywalls.components.properties.ThemeVideoUrls
import com.revenuecat.purchases.paywalls.components.properties.VideoUrls
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
import com.revenuecat.purchases.ui.revenuecatui.components.style.VideoComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.calculateOfferEligibility
import dev.drewhamilton.poko.Poko

@Poko
internal class VideoComponentState(
    initialWindowSize: WindowWidthSizeClass,
    initialDensity: Density,
    initialDarkMode: Boolean,
    initialLayoutDirection: LayoutDirection,
    private val style: VideoComponentStyle,
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
            style.packageUniqueId != null -> style.packageUniqueId == selectedInfo?.uniqueId
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

    private val themeVideoUrls: ThemeVideoUrls by derivedStateOf {
        val localeId = localeProvider().toLocaleId()

        presentedPartial?.sources?.run { getOrDefault(localeId, entry.value) }
            ?: style.sources.run { getOrDefault(localeId, entry.value) }
    }

    private val fallbackImageUrls: ThemeImageUrls? by derivedStateOf {
        val localeId = localeProvider().toLocaleId()

        (presentedPartial?.fallbackSources ?: style.fallbackSources)?.run { getOrDefault(localeId, entry.value) }
    }

    @get:JvmSynthetic
    val visible by derivedStateOf { presentedPartial?.partial?.visible ?: style.visible }

    @get:JvmSynthetic
    val videoUrls: VideoUrls by derivedStateOf {
        if (darkMode) themeVideoUrls.dark ?: themeVideoUrls.light else themeVideoUrls.light
    }

    @get:JvmSynthetic
    val fallbackUrls: ImageUrls? by derivedStateOf {
        if (darkMode) fallbackImageUrls?.dark ?: fallbackImageUrls?.light else fallbackImageUrls?.light
    }

    private val imageAspectRatio: Float? by derivedStateOf {
        val fallbackUrls = fallbackUrls ?: return@derivedStateOf null
        fallbackUrls.width.toFloat() / fallbackUrls.height.toFloat()
    }

    private val videoAspectRatio: Float by derivedStateOf {
        videoUrls.width.toFloat() / videoUrls.height.toFloat()
    }

    @get:JvmSynthetic
    val size: Size by derivedStateOf {
        (presentedPartial?.partial?.size ?: style.size).adjustForVideo(videoUrls, density)
    }

    @get:JvmSynthetic
    val aspectRatio: AspectRatio? by derivedStateOf {
        with(size) {
            when (val height = height) {
                is Fit -> when (width) {
                    is Fit -> AspectRatio(ratio = videoAspectRatio, matchHeightConstraintsFirst = true)
                    is Fill -> AspectRatio(ratio = videoAspectRatio, matchHeightConstraintsFirst = true)
                    is Fixed -> null
                }

                is Fill -> when (width) {
                    is Fit -> AspectRatio(ratio = videoAspectRatio, matchHeightConstraintsFirst = false)
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
    val fallbackAspectRatio: AspectRatio? by derivedStateOf {
        imageAspectRatio?.let { imageAspectRatio ->
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
     * Adjusts this size to take into account the size of the video.
     */
    private fun Size.adjustForVideo(videoUrls: VideoUrls, density: Density): Size =
        Size(
            width = width.adjustDimension(
                other = height,
                thisDimensionPx = videoUrls.width,
                otherDimensionPx = videoUrls.height,
                density = density,
            ),
            height = height.adjustDimension(
                other = width,
                thisDimensionPx = videoUrls.height,
                otherDimensionPx = videoUrls.width,
                density = density,
            ),
        )

    /**
     * Adjusts this size constraint to take into account the size of the video.
     */
    private fun SizeConstraint.adjustDimension(
        other: SizeConstraint,
        thisDimensionPx: UInt,
        otherDimensionPx: UInt,
        density: Density,
    ): SizeConstraint = when (this) {
        is Fit -> {
            when (other) {
                is Fit -> Fixed(with(density) { thisDimensionPx.toInt().toDp().value.toUInt() })
                is Fill -> this

                is Fixed -> {
                    // If the other dimension is Fixed, we'll have to scale this one by the same factor.
                    val otherDimensionDp = with(density) { otherDimensionPx.toInt().toDp() }
                    val scaleFactor = other.value.toFloat() / otherDimensionDp.value
                    Fixed(with(density) { (scaleFactor * thisDimensionPx.toInt()).toDp().value.toUInt() })
                }
            }
        }

        is Fill,
        is Fixed,
        -> this
    }
}

@Stable
@JvmSynthetic
@Composable
internal fun rememberUpdatedVideoComponentState(
    style: VideoComponentStyle,
    paywallState: PaywallState.Loaded.Components,
): VideoComponentState {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
    val density = LocalDensity.current
    val darkMode = isSystemInDarkTheme()
    val layoutDirection = LocalLayoutDirection.current
    return remember(style, windowSize, density, darkMode, layoutDirection) {
        VideoComponentState(
            initialWindowSize = windowSize,
            initialDensity = density,
            initialDarkMode = darkMode,
            initialLayoutDirection = layoutDirection,
            style = style,
            localeProvider = { paywallState.locale },
            selectedPackageInfoProvider = { paywallState.selectedPackageInfo },
            selectedTabIndexProvider = { paywallState.selectedTabIndex },
        )
    }
}
