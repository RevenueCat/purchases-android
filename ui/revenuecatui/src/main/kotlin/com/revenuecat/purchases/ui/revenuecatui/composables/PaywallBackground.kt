package com.revenuecat.purchases.ui.revenuecatui.composables

import BlurTransformation
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional
import com.revenuecat.purchases.ui.revenuecatui.extensions.defaultBackgroundPlaceholder

@Composable
internal fun BoxScope.PaywallBackground(templateConfiguration: TemplateConfiguration) {
    // current implementation uses a transformation on API level 30-, modifier on 31+.
    val transformation = if (templateConfiguration.configuration.blurredBackgroundImage && Build.VERSION.SDK_INT < 31)
        BlurTransformation(
            context = LocalContext.current, radius = BackgroundUIConstants.blurSize.toFloatPx(),
            scale = BackgroundUIConstants.blurScale
        )
    else null

    val modifier = Modifier
        .matchParentSize()
        // TODO: try to unify both methods into either a transformation or a modifier
        // one notable difference is that the transformation works at the image level so it'd run only once
        .conditional(
            templateConfiguration.configuration.blurredBackgroundImage
                && Build.VERSION.SDK_INT >= 31
        ) {
            blur(BackgroundUIConstants.blurSize, edgeTreatment = BlurredEdgeTreatment.Unbounded)
        }

    if (templateConfiguration.configuration.images.background == PaywallData.defaultBackgroundPlaceholder) {
        Image(
            modifier = modifier,
            painter = painterResource(id = R.drawable.default_background),
            contentDescription = null,
            contentScale = BackgroundUIConstants.contentScale,
        )
    } else {
        templateConfiguration.images.backgroundUri?.let {
            RemoteImage(
                urlString = it.toString(),
                modifier = modifier,
                contentScale = BackgroundUIConstants.contentScale,
                transformation = transformation,
                alpha = BackgroundUIConstants.blurAlpha
            )
        }
    }
}

private object BackgroundUIConstants {
    val blurSize = 40.dp
    const val blurAlpha = 0.7f
    val contentScale = ContentScale.Crop
    const val blurScale = 0.5f
}

@Composable
private fun Dp.toFloatPx(): Float {
    val density = LocalDensity.current.density
    return (this.value * density)
}
