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
     * The schema-declared `protocol_version`. When present, the SDK isolates the web content from
     * external sources by enforcing a fixed Content-Security-Policy. `null` for legacy/partial configs
     * that omit it, in which case no policy is enforced.
     */
    val protocolVersion: Int? = null,
) : ComponentStyle
