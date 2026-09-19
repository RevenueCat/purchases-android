package com.revenuecat.purchases.rules.customoperators

import com.revenuecat.purchases.rules.RulesEngine.EvaluationException
import com.revenuecat.purchases.rules.Scope
import com.revenuecat.purchases.rules.Value
import com.revenuecat.purchases.rules.operators.Operators

/** Name used in this operator's error messages. */
private const val OPERATOR_NAME = "rc.semverCompare"

/** Number of components in a semver core, once padded. */
private const val CORE_COMPONENTS = 3

/**
 * `rc.semverCompare` — three-way version comparison following
 * Semantic Versioning 2.0.0 precedence.
 */
internal object SemverOperator {

    private const val ARGUMENT_COUNT = 2

    /**
     * `{"rc.semverCompare": [left, right]}` — `-1`, `0`, or `1`, so rules
     * compare with the existing operators:
     * `{">=": [{"rc.semverCompare": [{"var": "appVersion"}, "2.1.0"]}, 0]}`.
     *
     * The built-in `<` / `>` compare version strings lexicographically,
     * which puts `"10.0.0"` below `"9.0.0"`.
     *
     * Requires exactly two operands, both strings that parse, otherwise
     * [EvaluationException.TypeMismatch].
     */
    fun opSemverCompare(args: Value, vars: Scope): Value {
        val evaluated = Operators.evalArgs(args, vars)

        Operators.checkArity(evaluated.size, listOf(ARGUMENT_COUNT), OPERATOR_NAME)

        val left = SemanticVersion.parse(evaluated[0])
        val right = SemanticVersion.parse(evaluated[1])
        return Value.IntValue(left.compare(right).toLong())
    }
}

/**
 * A parsed version: core components padded to three, plus prerelease
 * identifiers (empty for a release version). Build metadata is dropped, since
 * semver §10 excludes it from precedence.
 *
 * Three deliberate concessions to real-world version strings: a missing minor
 * or patch is `0` (`"2.1"` ≡ `"2.1.0"`), leading zeros are accepted
 * (`"1.02.0"` ≡ `"1.2.0"`) even though the spec forbids them, and build
 * metadata is dropped without being validated.
 */
private class SemanticVersion(
    val core: List<Long>,
    val prerelease: List<String>,
) {

    fun compare(other: SemanticVersion): Int {
        core.zip(other.core).forEach { (lhs, rhs) ->
            if (lhs != rhs) return if (lhs < rhs) -1 else 1
        }

        return when {
            prerelease.isEmpty() && other.prerelease.isEmpty() -> 0
            // A release outranks any prerelease of the same core.
            prerelease.isEmpty() -> 1
            other.prerelease.isEmpty() -> -1
            else -> comparePrerelease(prerelease, other.prerelease)
        }
    }

    companion object {

        fun parse(value: Value): SemanticVersion {
            if (value !is Value.StringValue) {
                throw EvaluationException.TypeMismatch(
                    "operator '$OPERATOR_NAME' expected version strings, got $value",
                )
            }

            val original = value.value
            val withoutBuild = original.substringBefore('+')
            val separator = withoutBuild.indexOf('-')

            val coreText: String
            val prereleaseText: String?
            if (separator < 0) {
                coreText = withoutBuild
                prereleaseText = null
            } else {
                coreText = withoutBuild.substring(0, separator)
                prereleaseText = withoutBuild.substring(separator + 1)
            }

            return SemanticVersion(
                core = parseCore(coreText, original),
                prerelease = parsePrerelease(prereleaseText, original),
            )
        }

        private fun parseCore(text: String, original: String): List<Long> {
            val components = text.split(".")

            if (components.isEmpty() || components.size > CORE_COMPONENTS) {
                throw invalid(original)
            }

            val core = components.map { component ->
                numericIdentifier(component) ?: throw invalid(original)
            }

            return core + List(CORE_COMPONENTS - core.size) { 0L }
        }

        private fun parsePrerelease(text: String?, original: String): List<String> {
            if (text == null) return emptyList()

            return text.split(".").map { identifier ->
                if (identifier.isEmpty() || !identifier.all(::isIdentifierCharacter)) {
                    throw invalid(original)
                }
                identifier
            }
        }

        /**
         * Semver §11: identifiers are compared field by field. When every
         * shared field ties, the version with more fields wins.
         */
        private fun comparePrerelease(lhs: List<String>, rhs: List<String>): Int {
            lhs.zip(rhs).forEach { (left, right) ->
                val ordering = compareIdentifiers(left, right)
                if (ordering != 0) return ordering
            }

            return if (lhs.size == rhs.size) 0 else if (lhs.size < rhs.size) -1 else 1
        }

        /**
         * Numeric identifiers compare numerically, alphanumeric ones by ASCII
         * order, and a numeric identifier always ranks below an alphanumeric one.
         */
        private fun compareIdentifiers(left: String, right: String): Int {
            val leftNumber = numericIdentifier(left)
            val rightNumber = numericIdentifier(right)

            return when {
                leftNumber != null && rightNumber != null ->
                    if (leftNumber == rightNumber) 0 else if (leftNumber < rightNumber) -1 else 1
                leftNumber != null -> -1
                rightNumber != null -> 1
                left == right -> 0
                else -> if (left < right) -1 else 1
            }
        }

        /**
         * A numeric identifier is digits only. [String.toLongOrNull] alone would
         * also accept a signed identifier like `-1`, which is alphanumeric here.
         *
         * `Long` rather than `Int` so a component wider than 32 bits, such as a
         * timestamp-shaped build number, still parses as it does on iOS.
         */
        private fun numericIdentifier(text: String): Long? {
            if (text.isEmpty() || !text.all { it in '0'..'9' }) return null
            return text.toLongOrNull()
        }

        private fun isIdentifierCharacter(character: Char): Boolean =
            character in 'a'..'z' || character in 'A'..'Z' || character in '0'..'9' || character == '-'

        private fun invalid(version: String): EvaluationException.TypeMismatch =
            EvaluationException.TypeMismatch(
                "operator '$OPERATOR_NAME' could not parse version '$version'",
            )
    }
}
