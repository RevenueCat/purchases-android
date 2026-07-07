@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.paywalls.components.properties.Size

@Immutable
internal data class WebViewComponentStyle(
    val url: String,
    override val visible: Boolean,
    override val size: Size,
) : ComponentStyle
