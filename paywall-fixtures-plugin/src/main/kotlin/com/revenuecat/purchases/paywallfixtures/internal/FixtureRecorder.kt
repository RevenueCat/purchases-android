package com.revenuecat.purchases.paywallfixtures.internal

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.time.Instant

/**
 * Records paywall fixtures: fetches the offerings response, filters it to offerings with a components
 * paywall, downloads the assets each paywall references, and mirrors everything into [outputDirectory]
 * in the layout that `PaywallFixtures.load()` (purchases-ui-testing) expects.
 */
@Suppress("LongParameterList")
internal class FixtureRecorder(
    private val apiKey: String,
    private val baseUrl: String,
    private val appUserId: String,
    private val offeringsFilter: Set<String>,
    private val outputDirectory: File,
    private val refresh: Boolean,
    private val log: (String) -> Unit,
) {

    private companion object {
        private const val CONNECTION_TIMEOUT_MS = 30_000
        private const val HTTP_OK = 200
        private const val MANIFEST_FILE_NAME = "fixture-manifest.json"
    }

    internal fun record() {
        val response = fetchOfferings()
        val filtered = filterOfferings(response)
        val offeringsArray = filtered.getJSONArray("offerings")
        check(offeringsArray.length() > 0) {
            "No offerings with a components paywall (Paywalls V2) were found" +
                (if (offeringsFilter.isEmpty()) "" else " matching $offeringsFilter") +
                ". Make sure your offerings have a paywall configured on the RevenueCat dashboard."
        }

        outputDirectory.mkdirs()
        File(outputDirectory, "offerings.json").writeText(filtered.toString(2))
        log("Recorded ${offeringsArray.length()} offering(s) to $outputDirectory.")

        val assetUrls = AssetUrlCollector.collect(filtered)
        var downloaded = 0
        var skipped = 0
        assetUrls.forEach { url ->
            if (downloadAsset(url)) downloaded++ else skipped++
        }
        log("Downloaded $downloaded asset(s), $skipped already present.")

        writeManifest(offeringsArray.length(), assetUrls.size)
    }

    private fun fetchOfferings(): JSONObject {
        val encodedUserId = URLEncoder.encode(appUserId, Charsets.UTF_8)
        val url = "${baseUrl.removeSuffix("/")}/v1/subscribers/$encodedUserId/offerings"
        log("Fetching offerings from $url...")
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = CONNECTION_TIMEOUT_MS
            connection.readTimeout = CONNECTION_TIMEOUT_MS
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("X-Platform", "android")
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode
            if (responseCode != HTTP_OK) {
                val errorBody = connection.errorStream?.use { it.readBytes().decodeToString() }.orEmpty()
                throw IOException("Fetching offerings failed with HTTP $responseCode: $errorBody")
            }
            return JSONObject(connection.inputStream.use { it.readBytes().decodeToString() })
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Keeps only offerings that have a components paywall and, when [offeringsFilter] is non-empty, are
     * part of the filter. Everything else in the response (current_offering_id, ui_config, placements,
     * etc.) is recorded verbatim.
     */
    private fun filterOfferings(response: JSONObject): JSONObject {
        val offerings = response.getJSONArray("offerings")
        val kept = JSONArray()
        for (i in 0 until offerings.length()) {
            val offering = offerings.getJSONObject(i)
            val hasComponentsPaywall = offering.has("paywall_components")
            val matchesFilter =
                offeringsFilter.isEmpty() || offeringsFilter.contains(offering.getString("identifier"))
            if (hasComponentsPaywall && matchesFilter) {
                kept.put(offering)
            }
        }
        return response.put("offerings", kept)
    }

    /**
     * Mirrors the asset at [url] into the output directory, at the host (minus TLD, reversed) + path —
     * the same layout `PaywallFixtures` resolves images from. Existing files are skipped unless
     * [refresh] is set. Returns true if the asset was downloaded.
     */
    private fun downloadAsset(url: String): Boolean {
        val target = File(outputDirectory, url.toMirrorPath())
        if (target.exists() && !refresh) return false

        target.parentFile.mkdirs()
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = CONNECTION_TIMEOUT_MS
            connection.readTimeout = CONNECTION_TIMEOUT_MS
            val responseCode = connection.responseCode
            if (responseCode != HTTP_OK) {
                throw IOException("Downloading asset $url failed with HTTP $responseCode.")
            }
            connection.inputStream.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        } finally {
            connection.disconnect()
        }
        return true
    }

    private fun writeManifest(offeringCount: Int, assetCount: Int) {
        val manifest = JSONObject()
            .put("format_version", 1)
            .put("recorded_at", Instant.now().toString())
            .put("app_user_id", appUserId)
            .put("offering_count", offeringCount)
            .put("asset_count", assetCount)
        File(outputDirectory, MANIFEST_FILE_NAME).writeText(manifest.toString(2))
    }
}

/**
 * `https://assets.pawwalls.com/header.webp` → `pawwalls/assets/header.webp`.
 */
internal fun String.toMirrorPath(): String {
    val uri = URI(this)
    val reversedHost = uri.host.split('.').dropLast(1).reversed().joinToString("/")
    return "$reversedHost${uri.path}".removePrefix("/")
}
