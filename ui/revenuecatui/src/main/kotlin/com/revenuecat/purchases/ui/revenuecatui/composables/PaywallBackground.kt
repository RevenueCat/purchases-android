package com.revenuecat.purchases.ui.revenuecatui.composables

import BlurTransformation
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional
import com.revenuecat.purchases.ui.revenuecatui.extensions.defaultBackgroundPlaceholder
import com.revenuecat.purchases.ui.revenuecatui.helpers.isInPreviewMode

// Current implementation uses a transformation on API level < 31, modifier on > 31.
@Composable
internal fun BoxScope.PaywallBackground(templateConfiguration: TemplateConfiguration) {
    val supportsNativeBlurring = Build.VERSION.SDK_INT >= BackgroundUIConstants.minSDKVersionSupportingBlur
    val shouldBlur = templateConfiguration.configuration.blurredBackgroundImage
    val imageAlpha = if (shouldBlur) { BackgroundUIConstants.blurAlpha } else 1.0f

    val backwardsCompatibleTransformation = if (shouldBlur && !supportsNativeBlurring) {
        BlurTransformation(
            context = LocalContext.current,
            radius = BackgroundUIConstants.blurSize.toFloatPx(),
        )
    } else {
        null
    }

    val modifier = Modifier
        .matchParentSize()
        .conditional(shouldBlur && supportsNativeBlurring) {
            blur(BackgroundUIConstants.blurSize, edgeTreatment = BlurredEdgeTreatment.Unbounded)
        }

    if (templateConfiguration.configuration.images.background == PaywallData.defaultBackgroundPlaceholder) {
        LocalImage(
            resource = R.drawable.default_background,
            modifier = modifier,
            contentScale = BackgroundUIConstants.contentScale,
            transformation = backwardsCompatibleTransformation,
            alpha = imageAlpha,
        )
    } else if (templateConfiguration.images.backgroundUri != null) {
        RemoteImage(
            urlString = templateConfiguration.images.backgroundUri.toString(),
            modifier = modifier,
            contentScale = BackgroundUIConstants.contentScale,
            transformation = backwardsCompatibleTransformation,
            alpha = imageAlpha,
        )
    }
}

private object BackgroundUIConstants {
    val blurSize = 40.dp
    val contentScale = ContentScale.Crop
    const val blurAlpha = 0.7f
    const val minSDKVersionSupportingBlur = 31
}

@Composable
private fun Dp.toFloatPx(): Float {
    val density = LocalDensity.current.density
    return (this.value * density)
}
