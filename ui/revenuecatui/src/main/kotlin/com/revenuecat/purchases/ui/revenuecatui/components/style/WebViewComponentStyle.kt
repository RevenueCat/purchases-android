@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.paywalls.components.properties.Size

@Immutable
internal data class WebViewComponentStyle(
    val url: String,
    override val visible: Boolean,
    override val size: Size,
    /** Schema `web_view.id`, sent to the content during the handshake. A blank id renders nothing. */
    val componentId: String,
    /** Schema-declared `protocol_version`, preserved for future use; the bridge advertises its own. */
    val protocolVersion: Int,
) : ComponentStyle
