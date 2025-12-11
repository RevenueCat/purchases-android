@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.utils

import java.net.URI

@JvmSynthetic
internal fun URI.appendQueryParameter(name: String, value: String): URI {
    val separator = if (this.query == null) "?" else "&"
    return URI("$this$separator$name=$value")
}
