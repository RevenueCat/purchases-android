package com.revenuecat.purchases.paywalls

import android.content.Context
import android.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.common.GoogleOfferingParser
import com.revenuecat.purchases.common.SharedPreferencesManager
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import org.json.JSONObject
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/**
 * Manual benchmark harness for PWENG-106 — reproduces and quantifies the startup cost of reading + parsing +
 * mapping a large cached `/offerings` payload dominated by V2 paywall component trees.
 *
 * This test is [Ignore]d so it never runs in the normal CI suite. Run it explicitly:
 *
 * ```
 * ./gradlew :purchases:testDefaultsBc8DebugUnitTest \
 *   --tests "com.revenuecat.purchases.paywalls.OfferingsParsingBenchmarkTest" --info
 * ```
 *
 * It uses **synthetic** payloads by default (no customer data is committed). To run against a real local
 * payload (e.g. the customer blob, kept outside the repo), pass:
 *
 * ```
 * ./gradlew ... --tests "...OfferingsParsingBenchmarkTest" \
 *   -Dofferings.fixture=$HOME/Downloads/offerings_sdk_offeringsResponse.json
 * ```
 *
 * The four stages mirror the real startup path:
 *  1. `JSONObject(payload)` — raw org.json parse of the cached string.
 *  2. `OfferingParser.createOfferings(...)` — the eager full map, which deserializes *every* offering's
 *     `paywall_components` tree (the customer has 33; only the current one is ever displayed).
 *  3. A focused micro-bench of `PaywallComponentsData` deserialization at increasing tree depth — isolates the
 *     `PaywallComponentSerializer` `decodeJsonElement -> toString() -> decodeFromString` re-parse, which is
 *     ~quadratic in tree depth.
 *  4. SharedPreferences round-trip + `SharedPreferencesManager.ensureMigrated()` `.all` materialization.
 *
 * NOTE on stage 4: Robolectric's SharedPreferences are in-memory, so this measures the *code-path* overhead
 * (the `.all` deep copy + migration copy), NOT the real-device XML inflate that causes the on-device "98% CPU
 * while reading SharedPreferences". Stages 1–3 are the faithful CPU proxy; stage 4 only demonstrates the
 * extra main-thread work the migration adds on top of the framework's own XML load.
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
@Ignore("Manual benchmark for PWENG-106 — run explicitly via --tests, not in CI")
@OptIn(InternalRevenueCatAPI::class)
internal class OfferingsParsingBenchmarkTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `stage 1+2 - read and map offerings as paywall count scales`() {
        val fixture = System.getProperty("offerings.fixture")
        if (fixture != null) {
            val payload = File(fixture).readText()
            log("Using real fixture: $fixture (${payload.length / 1024} KB)")
            benchmarkPayload(label = "fixture", payloadString = payload)
            return
        }

        log("Synthetic payloads — each paywall ~${syntheticPaywallSizeKb()} KB, depth $TREE_DEPTH, breadth $TREE_BREADTH")
        log("%-10s | %-10s | %-14s | %-16s".format("paywalls", "size(KB)", "JSONObject(ms)", "createOfferings(ms)"))
        log("-".repeat(60))
        for (n in listOf(1, 10, 33, 100, 200)) {
            benchmarkPayload(label = "$n", payloadString = syntheticOfferingsPayload(numPaywalls = n))
        }
    }

    private fun benchmarkPayload(label: String, payloadString: String) {
        val parser = GoogleOfferingParser()
        // Warm up so JIT / serializer init don't pollute the first data point.
        JSONObject(payloadString).also { parser.createOfferings(it, productsById = emptyMap()) }

        val parseMs = measureMinMs { JSONObject(payloadString) }
        val jsonObject = JSONObject(payloadString)
        val mapMs = measureMinMs { parser.createOfferings(jsonObject, productsById = emptyMap()) }

        log("%-10s | %-10d | %-14.1f | %-16.1f".format(label, payloadString.length / 1024, parseMs, mapMs))
    }

    @Test
    fun `stage 2b - single realistic paywall decode (per-display cost after lazy parse)`() {
        // After Fix #3, offerings-load no longer decodes any tree; the cost moves to first display of ONE paywall.
        // This measures that single realistic-tree decode (depth/breadth matching the eager-map paywalls).
        val json = syntheticPaywallComponents(depth = TREE_DEPTH, breadth = TREE_BREADTH)
        JsonTools.json.decodeFromString<PaywallComponentsData>(json) // warm up
        val decodeMs = measureMinMs { JsonTools.json.decodeFromString<PaywallComponentsData>(json) }
        log("single paywall decode (~${json.length / 1024} KB, depth $TREE_DEPTH): %.1f ms".format(decodeMs))
        log("Compare to baseline stage-2 'createOfferings' which decoded ALL paywalls at load time.")
    }

    @Test
    fun `stage 3 - PaywallComponentsData deserialization is super-linear in tree depth`() {
        log("Single paywall deserialization vs tree depth (breadth=$MICRO_BREADTH):")
        log("%-8s | %-10s | %-10s | %-12s | %-14s".format("depth", "nodes", "size(KB)", "decode(ms)", "ms/node(x1000)"))
        log("-".repeat(64))
        for (depth in listOf(4, 8, 16, 32, 64)) {
            val json = syntheticPaywallComponents(depth = depth, breadth = MICRO_BREADTH)
            val nodes = depth * (MICRO_BREADTH + 1) + 1
            // Warm up.
            JsonTools.json.decodeFromString<PaywallComponentsData>(json)
            val decodeMs = measureMinMs { JsonTools.json.decodeFromString<PaywallComponentsData>(json) }
            val msPerNode = decodeMs / nodes * 1000.0
            log("%-8d | %-10d | %-10d | %-12.1f | %-14.3f".format(depth, nodes, json.length / 1024, decodeMs, msPerNode))
        }
        log("If 'ms/node' rises with depth, deserialization is super-linear in depth (the re-stringify re-parse).")
    }

    @Test
    fun `stage 4 - SharedPreferences round-trip and migration all-materialization`() {
        val bigValue = syntheticOfferingsPayload(numPaywalls = 33)
        log("Cached offerings string under test: ${bigValue.length / 1024} KB")

        // Plain put/get round-trip on the dedicated RC prefs file.
        val prefs = context.getSharedPreferences("benchmark_prefs", Context.MODE_PRIVATE)
        val putMs = measureMinMs { prefs.edit().putString(OFFERINGS_KEY, bigValue).commit() }
        val getMs = measureMinMs { prefs.getString(OFFERINGS_KEY, null) }
        log("putString+commit: %.1f ms | getString: %.1f ms".format(putMs, getMs))

        // Seed the *legacy* default prefs with an RC-prefixed key, then time the migration `.all` path that
        // configure() triggers on the main thread.
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString("com.revenuecat.purchases.benchmark.offeringsResponse", bigValue)
            .commit()
        val migrateMs = measureMinMs(iterations = 1) {
            SharedPreferencesManager(context).getSharedPreferences()
        }
        log("SharedPreferencesManager.getSharedPreferences() (ensureMigrated + .all): %.1f ms".format(migrateMs))
        log("Note: Robolectric prefs are in-memory; real-device XML inflate of a multi-MB file is far costlier.")
    }

    // region synthetic payload generation

    /** Full `/offerings` payload with [numPaywalls] offerings, each carrying a V2 paywall component tree. */
    private fun syntheticOfferingsPayload(numPaywalls: Int): String {
        val offerings = (0 until numPaywalls).joinToString(",") { i ->
            """{"identifier":"off_$i","description":"benchmark","packages":[],""" +
                """"paywall_components":${syntheticPaywallComponents(TREE_DEPTH, TREE_BREADTH)}}"""
        }
        return """{"current_offering_id":"off_0","offerings":[$offerings]}"""
    }

    /** A valid [PaywallComponentsData] JSON whose `components_config` is a nested stack tree. */
    private fun syntheticPaywallComponents(depth: Int, breadth: Int): String =
        """
        {
          "id":"pw","template_name":"components","asset_base_url":"https://assets.example.com",
          "components_config":{"base":{
            "stack":${stackTree(depth, breadth)},
            "background":{"type":"color","value":{"light":{"type":"alias","value":"primary"}}}
          }},
          "components_localizations":{"en_US":{"k0":"Hello"}},
          "default_locale":"en_US"
        }
        """.trimIndent()

    /**
     * A nested stack tree of nesting depth [depth]. Each spine level also holds [breadth] empty leaf stacks, so
     * node count ~= depth * (breadth + 1). Unknown padding keys approximate the many real per-node properties
     * (hex colors, sizes, URLs) that inflate `components_config`; [JsonTools.json] ignores unknown keys.
     */
    private fun stackTree(depth: Int, breadth: Int): String {
        val children = StringBuilder()
        for (i in 0 until breadth) {
            if (i > 0) children.append(",")
            children.append(leafStack())
        }
        if (depth > 0) {
            if (breadth > 0) children.append(",")
            children.append(stackTree(depth - 1, breadth))
        }
        return """{"type":"stack","components":[$children],$PADDING}"""
    }

    private fun leafStack(): String = """{"type":"stack","components":[],$PADDING}"""

    private fun syntheticPaywallSizeKb(): Int =
        syntheticPaywallComponents(TREE_DEPTH, TREE_BREADTH).length / 1024

    // endregion

    private fun measureMinMs(iterations: Int = 3, block: () -> Unit): Double {
        var best = Double.MAX_VALUE
        repeat(iterations) {
            val start = System.nanoTime()
            block()
            val elapsed = (System.nanoTime() - start) / 1_000_000.0
            if (elapsed < best) best = elapsed
        }
        return best
    }

    private fun log(message: String) {
        // Plain stdout so it shows under `--tests ... --info` without the SDK logger.
        println("[PWENG-106] $message")
    }

    private companion object {
        const val OFFERINGS_KEY = "com.revenuecat.purchases.benchmark.offeringsResponse"

        // Approximates the customer's real paywalls (measured: depth ~23, ~578 nodes, ~34 KB avg).
        const val TREE_DEPTH = 22
        const val TREE_BREADTH = 22
        const val MICRO_BREADTH = 4

        // ~80 chars of unknown padding keys to give each node realistic byte weight.
        const val PADDING =
            """"x_pad":"0123456789012345678901234567890123456789","x_color":"#FF00AACC","x_url":"https://a.example.com/asset.png""""
    }
}
