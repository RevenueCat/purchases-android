package com.revenuecat.purchases.rules.helpers

import com.revenuecat.purchases.rules.Value
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import java.io.File

/**
 * In-repo JSON predicate fixtures. Each fixture declares a predicate, the variable scope,
 * the expected outcome (a boolean or an error), and optionally the warnings
 * the evaluation should emit. A single parameterized test runs them all so
 * adding coverage is a JSON edit, not a new Kotlin test.
 */

/** Expected `EvaluationException` for a fixture that should fail to evaluate. */
@Serializable
internal data class ExpectedError(
    @SerialName("error") val kind: String,
    val operator: String? = null,
)

/** A fixture's expected result: either a truthiness boolean or an error. */
internal sealed class ExpectedOutcome {
    data class BooleanOutcome(val value: Boolean) : ExpectedOutcome()
    data class ErrorOutcome(val error: ExpectedError) : ExpectedOutcome()
}

@Serializable
internal data class ExpectedWarnings(
    /**
     * Substrings that must each appear in some emitted warning. An empty list
     * asserts that no warning is emitted at all.
     */
    val contains: List<String> = emptyList(),
)

@Serializable
internal data class PredicateConformanceFixtureCase(
    val id: String,
    val description: String? = null,
    @Serializable(with = ValueSerializer::class) val predicate: Value,
    val variables: Map<String, @Serializable(with = ValueSerializer::class) Value> = emptyMap(),
    @Serializable(with = ExpectedOutcomeSerializer::class) val expected: ExpectedOutcome,
    val expectedWarnings: ExpectedWarnings? = null,
    /**
     * The exact, ordered list of messages the `log` channel must emit. An
     * empty list asserts that no log message is emitted. Unlike
     * [expectedWarnings] (substring matching), this is a full equality check.
     */
    val expectedLogs: List<String>? = null,
) {
    // Drives the parameterized test display name and re-run identity.
    override fun toString(): String = id
}

internal object ValueSerializer : KSerializer<Value> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): Value =
        (decoder as JsonDecoder).decodeJsonElement().toValue()

    override fun serialize(encoder: Encoder, value: Value): Nothing =
        error("ValueSerializer is decode-only")

    private fun JsonElement.toValue(): Value = when (this) {
        is JsonNull -> Value.Null
        is JsonObject -> Value.ObjectValue(mapValues { it.value.toValue() })
        is JsonArray -> Value.ArrayValue(map { it.toValue() })
        is JsonPrimitive -> toValue()
    }

    private fun JsonPrimitive.toValue(): Value = when {
        isString -> Value.StringValue(content)
        booleanOrNull != null -> Value.BoolValue(booleanOrNull!!)
        // An integer literal parses as `Long`; anything with a decimal point
        // (e.g. `1.0`) does not, so it falls through to `Double`/`FloatValue`.
        longOrNull != null -> Value.IntValue(longOrNull!!)
        doubleOrNull != null -> Value.FloatValue(doubleOrNull!!)
        else -> error("Unsupported JSON primitive: $content")
    }
}

/**
 * Picks the [ExpectedOutcome] shape from the JSON: a bare boolean is a
 * truthiness expectation, an object is an expected error.
 */
internal object ExpectedOutcomeSerializer : KSerializer<ExpectedOutcome> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): ExpectedOutcome {
        val jsonDecoder = decoder as JsonDecoder
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonObject -> ExpectedOutcome.ErrorOutcome(
                jsonDecoder.json.decodeFromJsonElement(ExpectedError.serializer(), element),
            )
            is JsonPrimitive -> ExpectedOutcome.BooleanOutcome(
                element.booleanOrNull ?: error("Unsupported `expected` shape: $element"),
            )
            else -> error("Unsupported `expected` shape: $element")
        }
    }

    override fun serialize(encoder: Encoder, value: ExpectedOutcome): Nothing =
        error("ExpectedOutcomeSerializer is decode-only")
}

internal object PredicateConformanceFixtureLoader {

    private const val FIXTURES_RESOURCE_DIR = "predicate-fixtures"

    /**
     * Resource path of the khepri-generated conformance envelope, downloaded on
     * demand by `scripts/rules_engine/download_predicate_conformance_fixtures.sh`
     * into `src/test/resources/predicate-conformance/`. Git-ignored and absent
     * by default, so the conformance suite is skipped unless it has been fetched.
     */
    private const val CONFORMANCE_RESOURCE_PATH = "predicate-conformance/predicate_conformance_v1.json"

    /**
     * Filesystem path override for the downloaded conformance envelope. When set
     * (and non-empty), it takes precedence over [CONFORMANCE_RESOURCE_PATH].
     * Mirrors the env var the download script honors.
     */
    const val CONFORMANCE_FIXTURE_PATH_ENV = "KHEPRI_PREDICATE_CONFORMANCE_FIXTURE_PATH"

    private val json = Json { ignoreUnknownKeys = true }

    val allCases: List<PredicateConformanceFixtureCase> by lazy {
        val directory = fixturesDirectory()
        val files = directory.listFiles { file -> file.extension == "json" }
            ?.sortedBy { it.name }
            .orEmpty()
        files.flatMap { loadCases(it) }
    }

    /**
     * The downloaded conformance envelope's cases, or null when the envelope is
     * absent (neither the env override nor the bundled resource resolves). Read
     * and decoded once. The download runs before the test JVM starts, so the
     * envelope is already present when this is first touched.
     */
    private val conformanceCasesOrNull: List<PredicateConformanceFixtureCase>? by lazy {
        val text = conformanceFixtureFile()?.takeIf { it.isFile }?.readText()
            ?: conformanceResourceText()
            ?: return@lazy null
        json.decodeFromString<Envelope>(text).fixtures
    }

    /** True when the downloaded conformance envelope is available (env override or bundled resource). */
    fun conformanceFixtureExists(): Boolean = conformanceCasesOrNull != null

    /** Parses the downloaded conformance envelope into fixture cases. Throws if it is absent. */
    fun conformanceCases(): List<PredicateConformanceFixtureCase> =
        conformanceCasesOrNull ?: error(
            "Predicate conformance fixtures not found. Run " +
                "scripts/rules_engine/download_predicate_conformance_fixtures.sh first.",
        )

    /** Like [conformanceCases] but yields an empty list when the envelope is absent, so the suite skips cleanly. */
    fun conformanceCasesOrEmpty(): List<PredicateConformanceFixtureCase> =
        conformanceCasesOrNull.orEmpty()

    private fun conformanceFixtureFile(): File? =
        System.getenv(CONFORMANCE_FIXTURE_PATH_ENV)
            ?.takeIf { it.isNotEmpty() }
            ?.let { File(it) }

    private fun conformanceResourceText(): String? =
        javaClass.classLoader?.getResource(CONFORMANCE_RESOURCE_PATH)?.readText()

    private fun fixturesDirectory(): File {
        val loader = javaClass.classLoader
            ?: error("No class loader available to locate $FIXTURES_RESOURCE_DIR")
        val url = loader.getResource(FIXTURES_RESOURCE_DIR)
            ?: error("Predicate fixtures resource directory not found: $FIXTURES_RESOURCE_DIR")
        return File(url.toURI())
    }

    private fun loadCases(file: File): List<PredicateConformanceFixtureCase> =
        json.decodeFromString<Envelope>(file.readText()).fixtures

    @Serializable
    private data class Envelope(val fixtures: List<PredicateConformanceFixtureCase>)
}
