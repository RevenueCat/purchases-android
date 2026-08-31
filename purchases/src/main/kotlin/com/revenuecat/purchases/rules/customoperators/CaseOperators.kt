package com.revenuecat.purchases.rules.customoperators

import com.revenuecat.purchases.rules.RulesEngine.EvaluationException
import com.revenuecat.purchases.rules.Scope
import com.revenuecat.purchases.rules.Value
import com.revenuecat.purchases.rules.operators.Operators

/**
 * `rc.lower` and `rc.upper` — locale-independent case conversion for
 * case-insensitive rule matching.
 */
internal object CaseOperators {

    /**
     * `{"rc.lower": value}` — lowercases a string argument.
     *
     * Uses Kotlin's parameterless [String.lowercase], which applies Unicode
     * default case mapping and is **locale-independent**. Do not use the
     * `Locale`-taking overloads: locale-sensitive conversion would make the
     * same rule match on one device (e.g. Turkish *I* → dotless *ı*) and fail
     * on another. Matches JS `toLowerCase()`, not `toLocaleLowerCase()`.
     *
     * Follows [Operators.firstArgEvaluated] spread semantics; extra arguments
     * are silently ignored.
     */
    fun opLower(args: Value, vars: Scope): Value =
        Value.StringValue(stringArgument(args, vars, operatorName = "rc.lower").lowercase())

    /**
     * `{"rc.upper": value}` — same string-only contract as [opLower], then
     * uppercases via locale-independent [String.uppercase].
     */
    fun opUpper(args: Value, vars: Scope): Value =
        Value.StringValue(stringArgument(args, vars, operatorName = "rc.upper").uppercase())

    /**
     * Requires a string operand, throwing [EvaluationException.TypeMismatch]
     * for anything else.
     *
     * Non-strings are **not** coerced through `jsString`, even though JS
     * `String(x).toLowerCase()` would accept them. Coercion makes data of
     * the wrong shape look like real data: an explicit `null` would lower
     * to `"null"` and compare equal to that string. Same reasoning as
     * `rc.length` and `rc.entries`.
     */
    private fun stringArgument(args: Value, vars: Scope, operatorName: String): String {
        val input = Operators.firstArgEvaluated(args, vars)

        if (input !is Value.StringValue) {
            throw EvaluationException.TypeMismatch(
                "operator '$operatorName' expected string, got $input",
            )
        }

        return input.value
    }
}
