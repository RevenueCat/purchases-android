@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.utils

import java.net.URL

@JvmSynthetic
internal fun URL.appendQueryParameter(name: String, value: String): URL {
    val encodedParameter = "${name.encodeQueryParameterComponent()}=${value.encodeQueryParameterComponent()}"
    val urlString = toString()
    val fragmentIndex = urlString.indexOf('#')
    val urlWithoutFragment = if (fragmentIndex == -1) urlString else urlString.substring(0, fragmentIndex)
    val fragment = if (fragmentIndex == -1) "" else urlString.substring(fragmentIndex)
    val separator = if (this.query == null) "?" else "&"

    return URL("$urlWithoutFragment$separator$encodedParameter$fragment")
}
