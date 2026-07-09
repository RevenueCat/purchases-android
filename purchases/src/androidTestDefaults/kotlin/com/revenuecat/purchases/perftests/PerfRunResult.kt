package com.revenuecat.purchases.perftests

import org.json.JSONArray
import org.json.JSONObject

/**
 * One timed iteration. All durations are milliseconds measured with a monotonic clock
 * (`SystemClock.elapsedRealtimeNanos`) inside the process, around the SDK calls themselves.
 *
 * `getOfferingsMs` (T3 - T2 in the test plan) is the headline number. `workflowBodyMs` and `uiConfigMs` are
 * the co-headline "render readiness" numbers: under the current gate, blob-backed workflow bodies are *not*
 * guaranteed ready when `getOfferings` returns, so a fast `getOfferings` can hide slow render readiness.
 *
 * `uiConfigIsDefault` and `workflowResolved` are completeness indicators: on lossy networks the SDK can
 * return *faster but degraded* results (ui_config falls back to a default instance, workflow bodies are
 * unavailable), which latency columns alone would misreport as a win.
 */
internal data class PerfIterationResult(
    val iteration: Int,
    val success: Boolean,
    val errorMessage: String? = null,
    val configureToOfferingsMs: Double? = null,
    val getOfferingsMs: Double? = null,
    val offeringsCount: Int? = null,
    val currentOfferingId: String? = null,
    val uiConfigMs: Double? = null,
    val uiConfigIsDefault: Boolean? = null,
    val workflowId: String? = null,
    val workflowResolutionMs: Double? = null,
    val workflowBodyMs: Double? = null,
    val workflowResolved: Boolean? = null,
    val httpEvents: List<HttpRequestEvent> = emptyList(),
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("iteration", iteration)
        put("success", success)
        putOpt("error_message", errorMessage)
        putOpt("configure_to_offerings_ms", configureToOfferingsMs)
        putOpt("get_offerings_ms", getOfferingsMs)
        putOpt("offerings_count", offeringsCount)
        putOpt("current_offering_id", currentOfferingId)
        putOpt("ui_config_ms", uiConfigMs)
        putOpt("ui_config_is_default", uiConfigIsDefault)
        putOpt("workflow_id", workflowId)
        putOpt("workflow_resolution_ms", workflowResolutionMs)
        putOpt("workflow_body_ms", workflowBodyMs)
        putOpt("workflow_resolved", workflowResolved)
        put("http_request_count", httpEvents.size)
        put("http_events", JSONArray(httpEvents.map { it.toJson() }))
    }
}

/**
 * An `http_request_performed` diagnostics event harvested from the SDK's own diagnostics file during the
 * iteration. Gives per-endpoint response times (`get_offerings`, `remote_config`, ...) without duplicating
 * instrumentation inside the SDK. Blob downloads bypass `HTTPClient` and are NOT captured here — count them
 * at the network shaping layer.
 */
internal data class HttpRequestEvent(
    val endpointName: String?,
    val responseTimeMillis: Long?,
    val responseCode: Int?,
    val successful: Boolean?,
    val etagHit: Boolean?,
    val isRetry: Boolean?,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        putOpt("endpoint_name", endpointName)
        putOpt("response_time_millis", responseTimeMillis)
        putOpt("response_code", responseCode)
        putOpt("successful", successful)
        putOpt("etag_hit", etagHit)
        putOpt("is_retry", isRetry)
    }
}

/** Nearest-rank percentile aggregates over one metric across iterations. */
internal data class PerfAggregate(
    val count: Int,
    val min: Double,
    val p50: Double,
    val p90: Double,
    val p95: Double,
    val max: Double,
    val mean: Double,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("count", count)
        put("min", min)
        put("p50", p50)
        put("p90", p90)
        put("p95", p95)
        put("max", max)
        put("mean", mean)
    }

    companion object {
        private const val P50 = 50.0
        private const val P90 = 90.0
        private const val P95 = 95.0

        fun of(values: List<Double>): PerfAggregate? {
            if (values.isEmpty()) return null
            val sorted = values.sorted()
            return PerfAggregate(
                count = sorted.size,
                min = sorted.first(),
                p50 = percentile(sorted, P50),
                p90 = percentile(sorted, P90),
                p95 = percentile(sorted, P95),
                max = sorted.last(),
                mean = sorted.average(),
            )
        }

        @Suppress("MagicNumber")
        private fun percentile(sorted: List<Double>, percentile: Double): Double {
            val rank = Math.ceil(percentile / 100.0 * sorted.size).toInt().coerceIn(1, sorted.size)
            return sorted[rank - 1]
        }
    }
}

/** The complete output of one run: metadata, per-iteration results, and per-metric aggregates. */
internal data class PerfRunResult(
    val config: PerfTestConfig,
    val sdkVersion: String,
    val deviceModel: String,
    val androidApiLevel: Int,
    val startedAtEpochMs: Long,
    val iterations: List<PerfIterationResult>,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("schema_version", 1)
        put("scenario", config.scenario.wireName)
        put("cache_mode", config.cacheMode.wireName)
        put("network_label", config.networkLabel)
        put("project_label", config.projectLabel)
        put("requested_iterations", config.iterations)
        put("sdk_version", sdkVersion)
        put("device_model", deviceModel)
        put("android_api_level", androidApiLevel)
        put("started_at_epoch_ms", startedAtEpochMs)
        put("error_count", iterations.count { !it.success })
        put("ui_config_default_fallback_count", iterations.count { it.uiConfigIsDefault == true })
        put("workflow_unresolved_count", iterations.count { it.workflowResolved == false })
        put("iterations", JSONArray(iterations.map { it.toJson() }))
        put(
            "aggregates",
            JSONObject().apply {
                putOpt("get_offerings_ms", aggregate { it.getOfferingsMs }?.toJson())
                putOpt("configure_to_offerings_ms", aggregate { it.configureToOfferingsMs }?.toJson())
                putOpt("ui_config_ms", aggregate { it.uiConfigMs }?.toJson())
                putOpt("workflow_body_ms", aggregate { it.workflowBodyMs }?.toJson())
                putOpt(
                    "http_request_count",
                    aggregate { result -> result.httpEvents.size.toDouble().takeIf { result.success } }?.toJson(),
                )
            },
        )
    }

    private fun aggregate(selector: (PerfIterationResult) -> Double?): PerfAggregate? =
        PerfAggregate.of(iterations.mapNotNull(selector))
}
