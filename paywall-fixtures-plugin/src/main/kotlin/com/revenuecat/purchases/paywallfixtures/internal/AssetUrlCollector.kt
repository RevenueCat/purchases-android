package com.revenuecat.purchases.paywallfixtures.internal

import org.json.JSONArray
import org.json.JSONObject

/**
 * Collects all downloadable asset URLs referenced by a paywall, by walking the offerings JSON:
 * - Any string value that is an http(s) URL with an image extension (e.g. the `original`, `webp` and
 *   `webp_low_res` variants of image components and backgrounds).
 * - Icon components, which reference their assets indirectly as `base_url` + `formats`: the rendered
 *   variant (`webp`) is collected.
 */
internal object AssetUrlCollector {

    private val imageExtensions = setOf("webp", "png", "jpg", "jpeg", "gif", "heic")

    internal fun collect(json: JSONObject): Set<String> {
        val urls = mutableSetOf<String>()
        walk(json, urls)
        return urls
    }

    private fun walk(value: Any?, urls: MutableSet<String>) {
        when (value) {
            is JSONObject -> {
                collectIconUrl(value, urls)
                value.keys().forEach { key -> walk(value.opt(key), urls) }
            }

            is JSONArray -> {
                for (i in 0 until value.length()) {
                    walk(value.opt(i), urls)
                }
            }

            is String -> {
                if (value.isImageUrl()) {
                    urls.add(value)
                }
            }
        }
    }

    private fun collectIconUrl(json: JSONObject, urls: MutableSet<String>) {
        val baseUrl = json.optString("base_url")
        val webpName = json.optJSONObject("formats")?.optString("webp").orEmpty()
        if (baseUrl.startsWith("http") && webpName.isNotEmpty()) {
            urls.add("${baseUrl.removeSuffix("/")}/$webpName")
        }
    }

    private fun String.isImageUrl(): Boolean {
        if (!startsWith("https://") && !startsWith("http://")) return false
        val extension = substringAfterLast('.', missingDelimiterValue = "").lowercase()
        return extension in imageExtensions
    }
}
