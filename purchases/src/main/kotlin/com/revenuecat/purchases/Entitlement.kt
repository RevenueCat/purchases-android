package com.revenuecat.purchases

import org.json.JSONException
import org.json.JSONObject

data class Entitlement(
    val offerings: Map<String, Offering>
) {

    internal object Factory {

        private const val ACTIVE_PRODUCT_IDENTIFIER_KEY = "active_product_identifier"
        private const val OFFERINGS_KEY = "offerings"

        private fun JSONObject.parseActiveProductIdentifier() =
            Offering(getString(ACTIVE_PRODUCT_IDENTIFIER_KEY))

        private fun JSONObject.parseEntitlement(): Entitlement {
            return Entitlement(
                getJSONObject(OFFERINGS_KEY).mapKeyAndContent { parseActiveProductIdentifier() }
            )
        }

        fun build(jsonObject: JSONObject): Map<String, Entitlement> {
            return jsonObject.mapKeyAndContent { parseEntitlement() }
        }

    }

}

private fun <T> JSONObject.mapKeyAndContent(transformation: JSONObject.() -> T): Map<String, T> {
    return keys().asSequence().map { jsonName ->
        try {
            jsonName to transformation.invoke(getJSONObject(jsonName))
        } catch (ignored: JSONException) {
            null
        }
    }.filterNotNull().toMap()
}