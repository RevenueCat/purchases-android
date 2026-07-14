package com.revenuecat.purchases.perf

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONArray

data class FixtureEntry(val match: String, val file: String, val status: Int)

object PerfFixtures {
    private const val ROOT = "/perf-fixtures"
    const val PLACEHOLDER_HOST = "http://PERF_MOCK_HOST/"

    fun loadManifest(): List<FixtureEntry> {
        val json = readBody("manifest.json")
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            FixtureEntry(o.getString("match"), o.getString("file"), o.optInt("status", 200))
        }
    }

    fun readBody(file: String): String =
        PerfFixtures::class.java.getResourceAsStream("$ROOT/$file")
            ?.bufferedReader()?.use { it.readText() }
            ?: error("perf fixture not found: $file")

    fun dispatcher(mockBaseUrl: String): Dispatcher {
        val entries = loadManifest()
        return object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path ?: ""
                val entry = entries.firstOrNull { path.contains(it.match) }
                    ?: return MockResponse().setResponseCode(404).setBody("{}")
                val body = readBody(entry.file).replace(PLACEHOLDER_HOST, mockBaseUrl)
                return MockResponse().setResponseCode(entry.status).setBody(body)
            }
        }
    }
}
