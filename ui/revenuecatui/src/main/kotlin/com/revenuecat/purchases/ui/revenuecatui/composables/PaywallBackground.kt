package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional

@Composable
internal fun PaywallBackground(templateConfiguration: TemplateConfiguration) {
    templateConfiguration.images.backgroundUri?.let {
        RemoteImage(
            urlString = it.toString(),
            modifier = Modifier
                .fillMaxSize()
                .conditional(templateConfiguration.configuration.blurredBackgroundImage) {
                    // TODO-PAYWALLS: backwards compatibility for blurring
                    blur(BackgroundUIConstants.blurSize, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                        .alpha(BackgroundUIConstants.blurAlpha)
                },
            contentScale = ContentScale.Crop,
        )
    }
}

private object BackgroundUIConstants {
    val blurSize = 40.dp
    const val blurAlpha = 0.7f
}
