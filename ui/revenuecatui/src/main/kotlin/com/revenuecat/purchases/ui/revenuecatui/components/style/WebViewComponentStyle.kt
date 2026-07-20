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
     * The canonical component id from the schema (`web_view.id`), assigned to the content during the
     * bridge handshake. Required to render; configs that omit it render nothing.
     */
    val componentId: String? = null,
    /**
     * The schema-declared `protocol_version`, decoded and preserved for future use. NOT the host's
     * capability: the bridge always advertises [WebViewEnvelope.DEFAULT_PROTOCOL_VERSION][
     * com.revenuecat.purchases.ui.revenuecatui.components.webview.WebViewEnvelope.DEFAULT_PROTOCOL_VERSION]
     * (the single version this SDK build implements) during the handshake, regardless of this value.
     */
    val protocolVersion: Int? = null,
) : ComponentStyle
