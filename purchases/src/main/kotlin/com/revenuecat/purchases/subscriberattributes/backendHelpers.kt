package com.revenuecat.purchases.subscriberattributes

import com.revenuecat.purchases.common.ATTRIBUTES_ERROR_RESPONSE_KEY
import com.revenuecat.purchases.common.ATTRIBUTE_ERRORS_KEY
import com.revenuecat.purchases.common.SubscriberAttributeError
import org.json.JSONObject

internal fun Map<String, SubscriberAttribute>.toBackendMap(): Map<String, Map<String, Any?>> {
    return map { (key, subscriberAttribute) ->
        key to subscriberAttribute.toBackendMap()
    }.toMap()
}

// Returns list of attribute errors found in the JSON object. The errors can be under
// ATTRIBUTES_ERROR_RESPONSE_KEY (for post receipt calls) or under
// ATTRIBUTE_ERRORS_KEY (for post subscriber attributes calls). If no attribute errors,
// returns an empty list
internal fun JSONObject?.getAttributeErrors(): List<SubscriberAttributeError> {
    if (this == null) return emptyList()

    val attributeErrorsJSONObject =
        this.optJSONObject(ATTRIBUTES_ERROR_RESPONSE_KEY) ?: this

    return attributeErrorsJSONObject.optJSONArray(ATTRIBUTE_ERRORS_KEY)
        ?.let { jsonArray ->
            (0 until jsonArray.length())
                .map { index -> jsonArray.getJSONObject(index) }
                .filter { it.has("key_name") && it.has("message") }
                .map {
                    SubscriberAttributeError(
                        it.getString("key_name"),
                        it.getString("message"),
                    )
                }
                .toList()
        } ?: emptyList()
}
