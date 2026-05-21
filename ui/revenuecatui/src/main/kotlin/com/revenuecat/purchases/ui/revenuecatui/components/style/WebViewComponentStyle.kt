@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.paywalls.components.properties.Size
import java.net.URL

@Immutable
internal data class WebViewComponentStyle(
    val url: URL,
    override val visible: Boolean,
    override val size: Size,
) : ComponentStyle
