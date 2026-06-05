package com.revenuecat.purchases.rules.helpers

import com.revenuecat.purchases.rules.Value
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File

/**
 * In-repo JSON predicate fixtures, mirroring the iOS `RulesEngineInternal`
 * conformance format. Each fixture declares a predicate, the variable scope,
 * the expected outcome (a boolean or an error), and optionally the warnings
 * the evaluation should emit. A single parameterized test runs them all so
 * adding coverage is a JSON edit, not a new Kotlin test.
 */

/** Expected `RuleError` for a fixture that should fail to evaluate. */
internal data class ExpectedError(
    val kind: String,
    val operator: String?,
)

/** A fixture's expected result: either a truthiness boolean or an error. */
internal sealed class ExpectedOutcome {
    data class BooleanOutcome(val value: Boolean) : ExpectedOutcome()
    data class ErrorOutcome(val error: ExpectedError) : ExpectedOutcome()
}

internal data class ExpectedWarnings(
    /**
     * Substrings that must each appear in some emitted warning. An empty list
     * asserts that no warning is emitted at all.
     */
    val contains: List<String>,
)

internal data class PredicateConformanceFixtureCase(
    val id: String,
    val description: String?,
    val predicate: Value,
    val variables: Map<String, Value>,
    val expected: ExpectedOutcome,
    val expectedWarnings: ExpectedWarnings?,
) {
    // Drives the parameterized test display name and re-run identity.
    override fun toString(): String = id
}

internal object PredicateConformanceFixtureLoader {

    private const val FIXTURES_RESOURCE_DIR = "predicate-fixtures"

    /**
     * All in-repo fixtures, parsed once and reused across the suite (mirrors the
     * iOS loader). Reading and parsing the files on every call is wasted work.
     */
    val allCases: List<PredicateConformanceFixtureCase> by lazy {
        val directory = fixturesDirectory()
        val files = directory.listFiles { file -> file.extension == "json" }
            ?.sortedBy { it.name }
            .orEmpty()
        files.flatMap { loadCases(it) }
    }

    private fun fixturesDirectory(): File {
        val loader = javaClass.classLoader
            ?: error("No class loader available to locate $FIXTURES_RESOURCE_DIR")
        val url = loader.getResource(FIXTURES_RESOURCE_DIR)
            ?: error("Predicate fixtures resource directory not found: $FIXTURES_RESOURCE_DIR")
        return File(url.toURI())
    }

    private fun loadCases(file: File): List<PredicateConformanceFixtureCase> {
        val root = JSONTokener(file.readText()).nextValue() as JSONObject
        val fixtures = root.getJSONArray("fixtures")
        return (0 until fixtures.length()).map { parseCase(fixtures.getJSONObject(it)) }
    }

    private fun parseCase(json: JSONObject): PredicateConformanceFixtureCase =
        PredicateConformanceFixtureCase(
            id = json.getString("id"),
            description = if (json.has("description")) json.getString("description") else null,
            predicate = ValueJsonHelper.fromParsedJson(json.get("predicate")),
            variables = parseVariables(json),
            expected = parseExpected(json.get("expected")),
            expectedWarnings = parseExpectedWarnings(json),
        )

    private fun parseVariables(json: JSONObject): Map<String, Value> {
        val variables = json.optJSONObject("variables") ?: return emptyMap()
        return when (val value = ValueJsonHelper.fromParsedJson(variables)) {
            is Value.ObjectValue -> value.entries
            else -> emptyMap()
        }
    }

    private fun parseExpected(raw: Any?): ExpectedOutcome = when (raw) {
        is Boolean -> ExpectedOutcome.BooleanOutcome(raw)
        is JSONObject -> ExpectedOutcome.ErrorOutcome(
            ExpectedError(
                kind = raw.getString("error"),
                operator = if (raw.has("operator")) raw.getString("operator") else null,
            ),
        )
        else -> error("Unsupported `expected` shape: $raw")
    }

    private fun parseExpectedWarnings(json: JSONObject): ExpectedWarnings? {
        val warnings = json.optJSONObject("expectedWarnings") ?: return null
        val contains = warnings.optJSONArray("contains")
        val substrings = if (contains == null) {
            emptyList()
        } else {
            (0 until contains.length()).map { contains.getString(it) }
        }
        return ExpectedWarnings(contains = substrings)
    }
}
