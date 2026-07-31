package com.revenuecat.purchases.common

import org.json.JSONObject

internal data class GeneratedOfferingsResponse(
    val text: String,
    val json: JSONObject,
)

internal object LargeOfferingsResponseGenerator {
    const val STRING_BUILDER_PREVIOUS_CAPACITY_CHARS = 9_437_182

    fun generateAtLeast(targetChars: Int): GeneratedOfferingsResponse {
        require(targetChars > 0)
        val prefix = """
            {"current_offering_id":"synthetic","offerings":[{"identifier":"synthetic","packages":[],"paywall_components":{"template_name":"synthetic","asset_base_url":"https://example.invalid/","components_config":{},"components_localizations":{"en_US":{"copy":"
        """.trimIndent()
        val suffix = "\"}},\"default_locale\":\"en_US\"}}]}"
        val valueLength = maxOf(1, targetChars - prefix.length - suffix.length)
        val text = prefix + "x".repeat(valueLength) + suffix
        return GeneratedOfferingsResponse(text, JSONObject(text))
    }
}
