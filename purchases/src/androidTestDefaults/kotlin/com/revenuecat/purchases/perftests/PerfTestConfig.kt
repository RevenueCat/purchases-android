package com.revenuecat.purchases.perftests

import androidx.test.platform.app.InstrumentationRegistry
import com.revenuecat.purchases.ForceServerErrorStrategy
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import java.net.URL

/**
 * The SDK configuration under measurement. Each scenario maps to a way of wiring the SDK, so runs are
 * comparable across the workflows migration:
 *
 * - [BASELINE]: workflows disabled — the pre-migration `getOfferings` path with no `/v1/config` dependency.
 * - [WORKFLOWS]: workflows enabled — `getOfferings` gates its callback on config readiness
 *   (`WorkflowManager.onPaywallConfigReady`).
 * - [WORKFLOWS_CONFIG_404]: workflows enabled, `/v1/config` synthesizes a 404 without hitting the network —
 *   exercises the session kill-switch. Note the 4xx is instant, so this measures kill-switch *behavior*
 *   (no hang, fallback works, steady-state cost), not the network cost of a real 4xx round trip.
 * - [WORKFLOWS_CONFIG_500]: workflows enabled, `/v1/config` requests are routed to the force-server-failure
 *   URL (5xx) — the SHOULD_RETRY path, where the endpoint keeps being attempted.
 * - [WORKFLOWS_CONFIG_UNREACHABLE]: workflows enabled, `/v1/config` requests are routed to an unreachable
 *   address — measures the gate's worst case: blocking on the HTTP client's transport timeout.
 *
 * The error strategies only affect the `/v1/config` endpoint; offerings and blob traffic are untouched
 * (matching a real incident where only the config endpoint is failing).
 */
internal enum class PerfScenario(val wireName: String, val useWorkflows: Boolean) {
    BASELINE("baseline", useWorkflows = false),
    WORKFLOWS("workflows", useWorkflows = true),
    WORKFLOWS_CONFIG_404("workflows_config_404", useWorkflows = true),
    WORKFLOWS_CONFIG_500("workflows_config_500", useWorkflows = true),
    WORKFLOWS_CONFIG_UNREACHABLE("workflows_config_unreachable", useWorkflows = true),
    ;

    fun forceServerErrorStrategy(): ForceServerErrorStrategy? = when (this) {
        BASELINE, WORKFLOWS -> null
        WORKFLOWS_CONFIG_404 -> object : ForceServerErrorStrategy {
            override fun shouldForceServerError(baseURL: URL, endpoint: Endpoint): Boolean = false
            override fun fakeResponseWithoutPerformingRequest(baseURL: URL, endpoint: Endpoint): HTTPResult? {
                if (endpoint !is Endpoint.GetRemoteConfig) return null
                return HTTPResult(
                    responseCode = RESPONSE_CODE_NOT_FOUND,
                    payload = "{}",
                    origin = HTTPResult.Origin.BACKEND,
                    requestDate = null,
                    verificationResult = VerificationResult.NOT_REQUESTED,
                    isLoadShedderResponse = false,
                    isFallbackURL = false,
                )
            }
        }
        WORKFLOWS_CONFIG_500 -> ForceServerErrorStrategy { _, endpoint ->
            endpoint is Endpoint.GetRemoteConfig
        }
        WORKFLOWS_CONFIG_UNREACHABLE -> object : ForceServerErrorStrategy {
            override val serverErrorURL: String
                get() = "http://localhost:9/unreachable-address"

            override fun shouldForceServerError(baseURL: URL, endpoint: Endpoint): Boolean =
                endpoint is Endpoint.GetRemoteConfig
        }
    }

    companion object {
        private const val RESPONSE_CODE_NOT_FOUND = 404

        fun fromWireName(name: String): PerfScenario =
            values().firstOrNull { it.wireName == name }
                ?: error("Unknown PERF_TEST_SCENARIO '$name'. Valid: ${values().joinToString { it.wireName }}")
    }
}

/**
 * The cache state each timed iteration starts from. Offerings and remote config have *independent* caches
 * (plus the blob store), so warmth is not a single bit:
 *
 * - [COLD]: everything cleared (SharedPreferences incl. offerings disk cache and ETags, remote config disk
 *   cache, blob store) and a fresh app user id — a fresh-install approximation.
 * - [WARM_DISK]: caches primed by an untimed full run, then the SDK instance is recreated. In-memory caches
 *   are gone (offerings still refetches over the network, with warm ETags), config disk cache and blob store
 *   are warm — an app-restart approximation.
 * - [WARM_MEMORY]: caches primed on the same instance; the timed call hits the in-memory offerings cache and
 *   the committed config — measures pure gate overhead on the fully warm path, which used to be ~instant
 *   before the workflows gate.
 * - [WARM_OFFERINGS_COLD_CONFIG]: primed, instance recreated, and *only* the remote config disk cache and
 *   blob store deleted. Offerings disk cache and ETags stay warm — the post-app-update migration scenario
 *   where the config layer starts cold while offerings data exists.
 */
internal enum class PerfCacheMode(val wireName: String) {
    COLD("cold"),
    WARM_DISK("warm_disk"),
    WARM_MEMORY("warm_memory"),
    WARM_OFFERINGS_COLD_CONFIG("warm_offerings_cold_config"),
    ;

    companion object {
        fun fromWireName(name: String): PerfCacheMode =
            values().firstOrNull { it.wireName == name }
                ?: error("Unknown PERF_TEST_CACHE_MODE '$name'. Valid: ${values().joinToString { it.wireName }}")
    }
}

/**
 * Run configuration, read from instrumentation arguments. All arguments are optional except
 * `PERF_TEST_API_KEY` (the test self-skips when it is absent, mirroring the backend integration tests):
 *
 * - `PERF_TEST_API_KEY`: API key of the dedicated performance test project.
 * - `PERF_TEST_SCENARIO`: one of [PerfScenario] wire names. Default: `workflows`.
 * - `PERF_TEST_CACHE_MODE`: one of [PerfCacheMode] wire names. Default: `cold`.
 * - `PERF_TEST_ITERATIONS`: timed iterations per run. Default: 3.
 * - `PERF_TEST_NETWORK_LABEL`: free-form label for the (externally applied) network shaping, recorded in the
 *   output so results can be grouped (e.g. `ideal`, `lte`, `lte_loss10`). Default: `unlabeled`.
 * - `PERF_TEST_PROJECT_LABEL`: free-form label of the test project (e.g. `large-loseit-copy`, `small`).
 * - `PERF_TEST_BASE_PLAN_IDS`: comma-separated base plan ids to fabricate per product. Required for Play
 *   Store projects because the Google offering parser matches packages by (productId, basePlanId); Test
 *   Store projects match by productId alone and can leave this unset.
 */
internal data class PerfTestConfig(
    val apiKey: String,
    val scenario: PerfScenario,
    val cacheMode: PerfCacheMode,
    val iterations: Int,
    val networkLabel: String,
    val projectLabel: String,
    val basePlanIds: List<String>,
) {
    companion object {
        private const val DEFAULT_ITERATIONS = 3

        fun fromInstrumentationArguments(): PerfTestConfig {
            val args = InstrumentationRegistry.getArguments()
            fun arg(name: String): String? = args.getString(name)?.takeIf { it.isNotBlank() }
            return PerfTestConfig(
                apiKey = arg("PERF_TEST_API_KEY") ?: "",
                scenario = PerfScenario.fromWireName(arg("PERF_TEST_SCENARIO") ?: "workflows"),
                cacheMode = PerfCacheMode.fromWireName(arg("PERF_TEST_CACHE_MODE") ?: "cold"),
                iterations = arg("PERF_TEST_ITERATIONS")?.toIntOrNull() ?: DEFAULT_ITERATIONS,
                networkLabel = arg("PERF_TEST_NETWORK_LABEL") ?: "unlabeled",
                projectLabel = arg("PERF_TEST_PROJECT_LABEL") ?: "default",
                basePlanIds = (arg("PERF_TEST_BASE_PLAN_IDS") ?: "")
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() },
            )
        }
    }
}
