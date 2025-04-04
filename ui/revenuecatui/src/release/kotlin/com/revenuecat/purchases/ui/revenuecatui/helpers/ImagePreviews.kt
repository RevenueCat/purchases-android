package com.revenuecat.purchases.ui.revenuecatui.helpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.painter.Painter
import coil.ImageLoader
import coil.request.ImageRequest
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI

// This file contains no-op implementations as a precaution. We should not be calling any of this code in release
// builds, but if we do, nothing (bad) will happen.

@JvmSynthetic
internal val LocalPreviewImageLoader: ProvidableCompositionLocal<ImageLoader?> = staticCompositionLocalOf { null }

@Suppress("UnusedParameter")
@JvmSynthetic
@Composable
internal fun ProvidePreviewImageLoader(imageLoader: ImageLoader, content: @Composable () -> Unit): Unit =
    CompositionLocalProvider(
        // Intentionally ignoring the provided imageLoader in release builds.
        LocalPreviewImageLoader provides null,
        content,
    )

@Suppress("FunctionOnlyReturningConstant", "UnusedParameter")
@JvmSynthetic
@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
internal fun ImageLoader.getPreviewPlaceholderBlocking(imageRequest: ImageRequest): Painter? =
    // Not actually doing anything in release builds.
    null
