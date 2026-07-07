@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.paywalls.components.properties.Size

@Immutable
internal data class WebViewComponentStyle(
    val url: String,
    override val visible: Boolean,
    override val size: Size,
    /**
     * The schema-declared `protocol_version`. The backend always sends this field (required, strictly 1).
     * When absent in legacy configs it is treated as 1. Content-Security-Policy is always applied.
     */
    val protocolVersion: Int? = null,
) : ComponentStyle
