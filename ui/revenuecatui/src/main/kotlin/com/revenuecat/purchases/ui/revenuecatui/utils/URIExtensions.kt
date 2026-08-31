@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.utils

import android.net.Uri
import java.net.URI

/**
 * Returns this URI with [parameters] set, encoding names and values. Existing parameters with the same name are
 * replaced where they are, further duplicates of those names are dropped, and the rest are appended in iteration
 * order. Any fragment is preserved.
 *
 * The query is edited as a string rather than through [URI]'s components, so opaque URIs like
 * `merchant:checkout?campaign=summer` keep their query too.
 */
@JvmSynthetic
internal fun URI.upsertQueryParameters(parameters: Map<String, String>): URI {
    if (parameters.isEmpty()) return this

    // Drained as we walk the existing query: the first occurrence of a name is replaced in place, any later
    // occurrence maps to null and is dropped, and whatever is left over has no occurrence to replace.
    val pending = parameters.entries.associateTo(LinkedHashMap()) { (name, value) ->
        name to "${Uri.encode(name)}=${Uri.encode(value)}"
    }

    val uriString = toString()
    val beforeFragment = uriString.substringBefore('#')
    val fragment = uriString.removePrefix(beforeFragment)
    val updatedQuery = beforeFragment.substringAfter('?', missingDelimiterValue = "")
        .split('&')
        .filter { it.isNotEmpty() }
        .mapNotNull { queryPart ->
            val name = Uri.decode(queryPart.substringBefore('='))
            if (name in parameters) pending.remove(name) else queryPart
        }
        .plus(pending.values)
        .joinToString("&")

    return URI("${beforeFragment.substringBefore('?')}?$updatedQuery$fragment")
}
