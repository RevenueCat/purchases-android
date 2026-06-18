@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.paywalls.components.properties.Size

@Immutable
internal data class WebViewComponentStyle(
    val urlTemplate: String,
    override val visible: Boolean,
    override val size: Size,
    val fallbackStackComponentStyle: StackComponentStyle? = null,
) : ComponentStyle
