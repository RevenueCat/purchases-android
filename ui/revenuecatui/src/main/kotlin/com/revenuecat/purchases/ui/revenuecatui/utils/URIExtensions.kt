@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.utils

import android.net.Uri
import java.net.URI

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

@JvmSynthetic
internal fun URI.upsertQueryParameters(parameters: Map<String, String>): URI {
    if (parameters.isEmpty()) return this

    val encodedParameters = parameters.mapValues { (name, value) ->
        "${name.encodeQueryParameterComponent()}=${value.encodeQueryParameterComponent()}"
    }
    val emittedParameterNames = mutableSetOf<String>()
    val updatedQueryParts = rawQueryForUpsert()
        ?.split("&")
        .orEmpty()
        .filter { it.isNotEmpty() }
        .mapNotNull { queryPart ->
            val decodedName = Uri.decode(queryPart.substringBefore('='))
            val replacement = encodedParameters[decodedName]
            when {
                replacement == null -> queryPart
                emittedParameterNames.add(decodedName) -> replacement
                else -> null
            }
        }
        .toMutableList()

    parameters.keys
        .filterNot(emittedParameterNames::contains)
        .mapTo(updatedQueryParts) { encodedParameters.getValue(it) }

    val uriString = toString()
    val fragmentIndex = uriString.indexOf('#')
    val fragment = if (fragmentIndex == -1) "" else uriString.substring(fragmentIndex)
    val uriWithoutFragment = if (fragmentIndex == -1) uriString else uriString.substring(0, fragmentIndex)
    val queryIndex = uriWithoutFragment.indexOf('?')
    val uriWithoutQuery = if (queryIndex == -1) uriWithoutFragment else uriWithoutFragment.substring(0, queryIndex)

    return URI("$uriWithoutQuery?${updatedQueryParts.joinToString("&")}$fragment")
}

private fun URI.rawQueryForUpsert(): String? {
    if (!isOpaque) return rawQuery

    val queryIndex = rawSchemeSpecificPart.indexOf('?')
    return if (queryIndex == -1) null else rawSchemeSpecificPart.substring(queryIndex + 1)
}

private fun String.encodeQueryParameterComponent(): String = Uri.encode(this)
