@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.utils

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@JvmSynthetic
internal fun URI.appendQueryParameter(name: String, value: String): URI {
    val encodedParameter = "${name.encodeQueryParameterComponent()}=${value.encodeQueryParameterComponent()}"
    val uriString = toString()
    val fragmentIndex = uriString.indexOf('#')
    val uriWithoutFragment = if (fragmentIndex == -1) uriString else uriString.substring(0, fragmentIndex)
    val fragment = if (fragmentIndex == -1) "" else uriString.substring(fragmentIndex)
    val separator = if (this.rawQuery == null) "?" else "&"

    return URI("$uriWithoutFragment$separator$encodedParameter$fragment")
}

internal fun String.encodeQueryParameterComponent(): String =
    URLEncoder.encode(this, StandardCharsets.UTF_8.name()).replace("+", "%20")
