@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.WebViewComponent
import com.revenuecat.purchases.paywalls.components.properties.Size

@OptIn(InternalRevenueCatAPI::class)
@Immutable
internal data class WebViewComponentStyle(
    val urlTemplate: String,
    override val visible: Boolean,
    override val size: Size,
    /**
     * The canonical component id from the schema (`web_view.id`), used to scope bidirectional
     * messages. May be null for legacy/partial configs that omit it, in which case the message bridge
     * cannot match inbound `component_id`s and is not installed.
     */
    val componentId: String? = null,
    val fallbackStackComponentStyle: StackComponentStyle? = null,
    /**
     * Web view capabilities as declared by the schema. Drives request-level domain allowlisting,
     * media (camera/microphone) and geolocation permission decisions. `null` means no capabilities
     * are granted (the secure default).
     */
    val capabilities: WebViewComponent.Capabilities? = null,
) : ComponentStyle
