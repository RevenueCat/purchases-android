package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional
import com.revenuecat.purchases.ui.revenuecatui.extensions.defaultBackgroundPlaceholder
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import kotlin.math.roundToInt

@Composable
internal fun BoxScope.PaywallBackground(templateConfiguration: TemplateConfiguration) {
    val transformation = if (templateConfiguration.configuration.blurredBackgroundImage)
        BlurTransformation(context = LocalContext.current, radius = BackgroundUIConstants.blurSize.toFloatPx(),
            scale = BackgroundUIConstants.blurScale)
    else null

    val modifier = Modifier
        .matchParentSize()
        .conditional(templateConfiguration.configuration.blurredBackgroundImage) {
            // TODO-PAYWALLS: backwards compatibility for blurring
            blur(BackgroundUIConstants.blurSize, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .alpha(BackgroundUIConstants.blurAlpha)
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
