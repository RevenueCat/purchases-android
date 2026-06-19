@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.paywalls.components.properties.Size

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
) : ComponentStyle
