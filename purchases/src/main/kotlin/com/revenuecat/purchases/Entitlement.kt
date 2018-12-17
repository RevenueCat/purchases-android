package com.revenuecat.purchases

import org.json.JSONException
import org.json.JSONObject

/**
 * An entitlement represents features or content that a user is "entitled" to.
 * Entitlements are unlocked by having an active subscription or making a one-time purchase.
 * Many different products can unlock. Most subscription apps only have one entitlement,
 * unlocking all premium features. However, if you had two tiers of content such as premium and
 * premium_plus, you would have 2 entitlements. A common and simple setup example is one entitlement
 * with identifier pro, one offering monthly, with one product.
 * See [this link](https://docs.revenuecat.com/docs/entitlements) for more info
 * @property offerings Map of offering objects by name
 */
data class Entitlement internal constructor(
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