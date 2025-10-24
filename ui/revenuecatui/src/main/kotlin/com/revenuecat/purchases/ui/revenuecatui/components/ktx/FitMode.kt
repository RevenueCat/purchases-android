@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.ktx

import androidx.compose.ui.layout.ContentScale
import com.revenuecat.purchases.paywalls.components.properties.FitMode

@JvmSynthetic
internal fun FitMode.toContentScale() =
    when (this) {
        FitMode.FIT -> ContentScale.Fit
        FitMode.FILL -> ContentScale.Crop
    }
