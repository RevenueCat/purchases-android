package com.revenuecat.purchases.paywallfixtures.internal

import org.json.JSONArray
import org.json.JSONObject

/**
 * Collects all downloadable asset URLs referenced by a paywall, by walking the offerings JSON:
 * - Any string value that is an http(s) URL with an image extension (e.g. the `original`, `webp` and
 *   `webp_low_res` variants of image components and backgrounds).
 * - Icon components, which reference their assets as `base_url` + `formats.webp`. The `base_url` and
 *   `formats` may live on the same object (a top-level icon) or on different levels (an icon override /
 *   state variant carries only `formats`, inheriting `base_url` from its enclosing icon component), so
 *   the nearest `base_url` in scope is inherited down the tree.
 */
internal object AssetUrlCollector {

    private val imageExtensions = setOf("webp", "png", "jpg", "jpeg", "gif", "heic")

    internal fun collect(json: JSONObject): Set<String> {
        val urls = mutableSetOf<String>()
        walk(json, inheritedBaseUrl = null, urls = urls)
        return urls
    }

    private fun walk(value: Any?, inheritedBaseUrl: String?, urls: MutableSet<String>) {
        when (value) {
            is JSONObject -> {
                // An object may introduce a base_url that its descendants (e.g. icon overrides) inherit.
                val baseUrl = value.optString("base_url").takeIf { it.startsWith("http") } ?: inheritedBaseUrl
                collectIconUrl(value, baseUrl, urls)
                value.keys().forEach { key -> walk(value.opt(key), baseUrl, urls) }
            }

            is JSONArray -> {
                for (i in 0 until value.length()) {
                    walk(value.opt(i), inheritedBaseUrl, urls)
                }
            }

            is String -> {
                if (value.isImageUrl()) {
                    urls.add(value)
                }
            }
        }
    }

    private fun collectIconUrl(json: JSONObject, baseUrl: String?, urls: MutableSet<String>) {
        val webpName = json.optJSONObject("formats")?.optString("webp").orEmpty()
        if (baseUrl != null && webpName.isNotEmpty()) {
            urls.add("${baseUrl.removeSuffix("/")}/$webpName")
        }
    }

    private fun String.isImageUrl(): Boolean {
        if (!startsWith("https://") && !startsWith("http://")) return false
        val extension = substringAfterLast('.', missingDelimiterValue = "").lowercase()
        return extension in imageExtensions
    }
}
