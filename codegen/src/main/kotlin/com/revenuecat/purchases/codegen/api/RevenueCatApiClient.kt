package com.revenuecat.purchases.codegen.api

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

internal class RevenueCatApiClient(
    private val apiKey: String,
    private val baseUrl: String = "https://api.revenuecat.com/v2",
) {

    private companion object {
        private const val MAX_RETRIES = 5
        private const val INITIAL_BACKOFF_MS = 1_000L
        private const val REQUEST_DELAY_MS = 500L
    }

    internal fun fetchEntitlements(projectId: String): List<EntitlementSchema> {
        return fetchPaginated("$baseUrl/projects/$projectId/entitlements") { json ->
            EntitlementSchema(
                id = json.getString("id"),
                lookupKey = json.getString("lookup_key"),
                displayName = json.optString("display_name", json.getString("lookup_key")),
            )
        }
    }

    internal fun fetchOfferings(projectId: String): List<OfferingSchema> {
        val offerings = fetchPaginated("$baseUrl/projects/$projectId/offerings") { json ->
            OfferingSchema(
                id = json.getString("id"),
                lookupKey = json.getString("lookup_key"),
                displayName = json.optString("display_name", json.getString("lookup_key")),
                isCurrent = json.optBoolean("is_current", false),
                packages = emptyList(),
            )
        }

        return offerings.map { offering ->
            Thread.sleep(REQUEST_DELAY_MS)
            val packages = fetchPackages(projectId, offering.id)
            offering.copy(packages = packages)
        }
    }

    private fun fetchPackages(projectId: String, offeringId: String): List<PackageSchema> {
        return fetchPaginated("$baseUrl/projects/$projectId/offerings/$offeringId/packages") { json ->
            PackageSchema(
                id = json.getString("id"),
                lookupKey = json.getString("lookup_key"),
                displayName = json.optString("display_name", json.getString("lookup_key")),
            )
        }
    }

    /**
     * Fetches all pages of a paginated list endpoint.
     * The RevenueCat API v2 returns `next_page` as a full URL or null.
     */
    private fun <T> fetchPaginated(
        initialUrl: String,
        mapper: (JSONObject) -> T,
    ): List<T> {
        val results = mutableListOf<T>()
        var nextUrl: String? = initialUrl

        while (nextUrl != null) {
            val response = httpGetWithRetry(nextUrl)
            val json = JSONObject(response)
            val items = json.getJSONArray("items")

            for (i in 0 until items.length()) {
                results.add(mapper(items.getJSONObject(i)))
            }

            // next_page is a full URL or null/empty
            val nextPage = json.opt("next_page")
            nextUrl = if (nextPage != null && nextPage != JSONObject.NULL &&
                nextPage.toString().isNotEmpty() && nextPage.toString() != "null"
            ) {
                nextPage.toString()
            } else {
                null
            }

            if (nextUrl != null) {
                Thread.sleep(REQUEST_DELAY_MS)
            }
        }

        return results
    }

    private fun httpGetWithRetry(url: String): String {
        var lastException: Exception? = null

        for (attempt in 0 until MAX_RETRIES) {
            try {
                return httpGet(url)
            } catch (e: RateLimitException) {
                lastException = e
                val backoffMs = if (e.backoffMs > 0) {
                    e.backoffMs
                } else {
                    INITIAL_BACKOFF_MS * (1L shl attempt)
                }
                Thread.sleep(backoffMs)
            } catch (e: IOException) {
                // Retry transient network errors (timeouts, connection resets, etc.)
                lastException = e
                Thread.sleep(INITIAL_BACKOFF_MS * (1L shl attempt))
            }
        }

        throw lastException ?: RuntimeException("Failed to fetch $url after $MAX_RETRIES retries")
    }

    private fun httpGet(url: String): String {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 30_000
            connection.readTimeout = 30_000

            val responseCode = connection.responseCode
            if (responseCode == 429) {
                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: ""
                val backoffMs = try {
                    JSONObject(errorBody).optLong("backoff_ms", 0)
                } catch (_: Exception) {
                    0L
                }
                throw RateLimitException(url, backoffMs)
            }
            if (responseCode != 200) {
                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: ""
                throw RuntimeException(
                    "RevenueCat API returned HTTP $responseCode for $url: $errorBody",
                )
            }

            return connection.inputStream.bufferedReader().readText()
        } finally {
            connection.disconnect()
        }
    }

    private class RateLimitException(
        url: String,
        val backoffMs: Long,
    ) : RuntimeException("Rate limited on $url (backoff: ${backoffMs}ms)")
}
