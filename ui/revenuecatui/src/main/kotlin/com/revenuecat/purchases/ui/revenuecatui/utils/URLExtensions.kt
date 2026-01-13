@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.utils

import java.net.URL

@JvmSynthetic
internal fun URL.appendQueryParameter(name: String, value: String): URL {
    val separator = if (this.query == null) "?" else "&"
    return URL("$this$separator$name=$value")
}
