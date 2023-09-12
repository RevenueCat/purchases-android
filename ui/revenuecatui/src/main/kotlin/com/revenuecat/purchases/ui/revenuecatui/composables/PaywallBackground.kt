package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.extensions.backgroundUrlString
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional

@Composable
internal fun PaywallBackground(data: PaywallData) {
    data.backgroundUrlString?.let {
        RemoteImage(
            urlString = it,
            modifier = Modifier
                .fillMaxSize()
                .conditional(data.config.blurredBackgroundImage) {
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
